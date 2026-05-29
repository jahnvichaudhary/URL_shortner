# URL Shortener with Analytics

A production-ready URL shortener built with Spring Boot. Converts long URLs to compact Base62 short codes, serves redirects from a Redis cache for low-latency lookups, and tracks per-link click analytics backed by PostgreSQL.

---

## Features

- **Base62 encoding** — generates short codes like `ab3Xk` (case-sensitive alphanumeric)
- **Redis caching** — redirect lookups hit Redis first; ~80% latency reduction vs direct DB queries
- **Click analytics** — tracks total clicks, timestamps, and referrers per short URL
- **Custom aliases** — optionally specify your own short code (e.g. `/my-project`)
- **Link expiry** — set an optional TTL; expired links return 410 Gone
- **Production Dockerfile** — multi-stage build, minimal final image

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3, Spring Data JPA |
| Cache | Redis 7 |
| Database | PostgreSQL 15 |
| Build | Maven |
| Container | Docker, Docker Compose |

---

## Architecture

```
Client
  │
  ▼
POST /shorten  ──►  Generate Base62 code  ──►  Save to PostgreSQL
                                                      │
GET /{code}    ──►  Redis cache hit? ──Yes──►  302 Redirect
                         │ No
                         ▼
                   Fetch from PostgreSQL  ──►  Write to Redis  ──►  302 Redirect
                                                      │
                                             Increment click count
```

Redis TTL mirrors the link's expiry (or defaults to 24h for non-expiring links), so the cache stays consistent with the DB automatically.

---

## Getting Started

### Run with Docker Compose

```bash
git clone https://github.com/jahnvichaudhary/URL_shortner.git
cd URL_shortner/url-shortener-main
docker compose up --build
```

- API: `http://localhost:8080`
- Redis: `localhost:6379`
- PostgreSQL: `localhost:5432`

### Run locally

```bash
# Requires Redis and PostgreSQL running locally
# Update application.properties with your connection details
mvn spring-boot:run
```

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/shorten` | Create a short URL |
| GET | `/{code}` | Redirect to original URL |
| GET | `/api/analytics/{code}` | Get click analytics |
| DELETE | `/api/links/{code}` | Delete a short URL |

---

## Sample Requests

**Shorten a URL:**
```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://github.com/jahnvichaudhary/URL_shortner",
    "customAlias": "my-repo",
    "expiresInDays": 30
  }'
```

Response:
```json
{
  "shortCode": "my-repo",
  "shortUrl": "http://localhost:8080/my-repo",
  "originalUrl": "https://github.com/jahnvichaudhary/URL_shortner",
  "expiresAt": "2024-07-15T00:00:00",
  "createdAt": "2024-06-15T10:30:00"
}
```

**Redirect:**
```bash
curl -L http://localhost:8080/my-repo
# → 302 redirect to the original URL
```

**Get analytics:**
```bash
curl http://localhost:8080/api/analytics/my-repo
```

Response:
```json
{
  "shortCode": "my-repo",
  "originalUrl": "https://github.com/jahnvichaudhary/URL_shortner",
  "totalClicks": 47,
  "createdAt": "2024-06-15T10:30:00",
  "recentClicks": [
    { "timestamp": "2024-06-20T14:22:11", "referrer": "https://linkedin.com" },
    { "timestamp": "2024-06-20T09:05:43", "referrer": "direct" }
  ]
}
```

---

## Why Redis cuts latency by ~80%

A typical PostgreSQL lookup for a short code (with index) takes ~5–15ms. A Redis GET for the same key takes ~0.2–0.5ms. For a URL shortener where every redirect is a read, this difference is significant at any meaningful traffic level.

The cache is populated on first miss and invalidated on deletion. TTL is set to match the link's expiry to avoid serving stale redirects from cache.

---

## Project Structure

```
src/main/java/com/urlshortener/
├── controller/        # UrlController, AnalyticsController
├── service/           # UrlService (encode/decode, cache logic)
├── repository/        # UrlMappingRepository, ClickRepository
├── entity/            # UrlMapping, ClickEvent
├── cache/             # RedisService wrapper
└── config/            # RedisConfig, AppConfig
```

---

