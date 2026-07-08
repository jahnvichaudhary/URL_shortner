

# URL Shortener — Snowflake IDs, Base62 Encoding & Cache-Aside Redis

A production-style URL shortening service built to explore three specific
distributed-systems problems — coordination-free unique ID generation at
scale, compact reversible encoding, and cache-aside performance under
high read load — rather than just mapping a random string to a row.

**Live:** [url-shortner-2-guma.onrender.com](https://url-shortner-2-guma.onrender.com/)
**API Docs:** `/swagger-ui.html`

## Architecture

```
Internet → Controller → Service → Redis Cache → PostgreSQL
```

- **ID Generation** — Twitter Snowflake algorithm generates 4 million
  unique IDs per millisecond across 1024 servers without any central
  coordination.
- **Encoding** — Base62 encoding compresses 64-bit IDs into 7-character
  alphanumeric short codes (3.5 trillion capacity).
- **Caching** — Redis Cache-Aside pattern reduces database load by ~95%
  on high-traffic links.
- **Containerisation** — Full Docker Compose stack with Spring Boot,
  PostgreSQL, and Redis.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4 |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| ORM | Hibernate / Spring Data JPA |
| Container | Docker, Docker Compose |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Build | Maven |

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/urls` | Create short URL (optional `customAlias`) |
| GET | `/r/{shortCode}` | Redirect to original URL |
| GET | `/api/v1/urls/{code}` | Get URL metadata |
| DELETE | `/api/v1/urls/{code}` | Deactivate URL |
| GET | `/api/v1/stats` | Platform statistics |
| GET | `/swagger-ui.html` | API documentation |

## Key Design Decisions

**Why Twitter Snowflake over UUID?**
UUIDs are random — they cause B-tree index fragmentation in PostgreSQL
at scale. Snowflake IDs are time-ordered, globally unique, and generated
without database coordination.

**Why Redis Cache-Aside?**
The redirect endpoint is called millions of times per day. Caching the
URL mapping in Redis means 95%+ of redirects never touch the database —
sub-millisecond response times.

**Why 302 over 301?**
301 is cached by browsers permanently — you lose all click tracking and
cannot deactivate links. 302 checks the server every time, giving full
control.

**Why soft delete?**
Deleting rows destroys analytics history. Setting `isActive=false`
preserves click data while stopping redirects.

**Why support custom aliases alongside generated codes?**
Not every link needs a Snowflake-generated code — a marketing link
wants `/r/summer-sale`, not `/r/4kX9pQ2`. The service accepts an
optional `customAlias` on creation; if it's already taken, the request
fails fast with a 409 rather than silently overwriting someone else's
link.

**Why RFC 7807 Problem Details for errors?**
Error responses use Spring's `ProblemDetail` (RFC 7807) instead of a
bare `{"error": "..."}` string — structured `type`, `title`, `status`,
and `detail` fields, plus request-specific extensions (e.g. the
conflicting `alias` and a `timestamp`). It's a small thing, but it
means every error is machine-parseable the same way across every
endpoint, instead of each exception handler inventing its own shape.

## Known Limitations

- Snowflake worker/datacenter ID is statically assigned, not
  dynamically leased — running multiple instances safely would need a
  coordination service (etcd/ZooKeeper) to prevent ID collisions.
- No rate limiting on `POST /api/v1/urls` — the endpoint is currently
  open to abuse/spam link generation.
- Single Redis and PostgreSQL instance — no replication or failover
  configured yet.
- No authentication on management endpoints (deactivate, metadata) —
  anyone with a short code can act on it.

## What I'd Do At Scale

- Rate-limit `POST /api/v1/urls` per-IP/user using the same Redis +
  Lua sliding-window pattern from my [Distributed Rate Limiter](https://github.com/jahnvichaudhary/Distributed-Rate-Limiter).
- Move Snowflake worker-ID assignment to a dynamic etcd/ZooKeeper lease
  so the service can safely scale to N instances.
- Add PostgreSQL read replicas for metadata/stats reads, keep writes
  single-master.
- Push extremely hot redirects to a CDN edge layer so the top N links
  never hit the application tier at all.

## Running Locally

### Prerequisites
- Docker Desktop
- Java 21
- Maven

### Start everything with one command
```bash
docker compose up -d
```

### Test it
```bash
# Create a short URL
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://www.github.com", "expiryDays": 30}'

# Create with a custom alias — returns 409 if already taken
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://www.github.com", "customAlias": "my-link", "expiryDays": 30}'

# Use the short code from the response
curl http://localhost:8080/r/YOUR_SHORT_CODE

# Open API docs
open http://localhost:8080/swagger-ui.html
```

## Project Structure

```
src/main/java/com/urlshortener/
│
├── UrlShortenerApplication.java
│
├── util/
│   ├── SnowflakeIdGenerator.java
│   └── Base62Encoder.java
│
├── entity/
│   └── UrlMapping.java
│
├── repository/
│   └── UrlMappingRepository.java
│
├── dto/
│   ├── ShortenRequest.java
│   ├── ShortenResponse.java
│   └── ErrorResponse.java
│
├── config/
│   └── RedisConfig.java
│
├── service/
│   └── UrlService.java
│
├── controller/
│   └── UrlController.java
│
└── exception/
    ├── UrlNotFoundException.java
    ├── UrlExpiredException.java
    ├── UrlInactiveException.java
    ├── AliasAlreadyExistsException.java
    ├── SnowflakeException.java
    └── GlobalExceptionHandler.java
```


