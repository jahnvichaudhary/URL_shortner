# URL Shortener Service

A production-grade distributed URL shortener built with
Java Spring Boot, Twitter Snowflake ID generation, and
Redis caching — similar to how Bitly works under the hood.



## Architecture
Internet → Controller → Service → Redis Cache → PostgreSQL

- **ID Generation** — Twitter Snowflake algorithm generates
  4 million unique IDs per millisecond across 1024 servers
  without any central coordination
- **Encoding** — Base62 encoding compresses 64-bit IDs into
  7-character alphanumeric short codes (3.5 trillion capacity)
- **Caching** — Redis Cache-Aside pattern reduces database
  load by ~95% on high-traffic links
- **Containerisation** — Full Docker Compose stack with
  Spring Boot, PostgreSQL, and Redis

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
| POST | /api/v1/urls | Create short URL |
| GET | /r/{shortCode} | Redirect to original URL |
| GET | /api/v1/urls/{code} | Get URL metadata |
| DELETE | /api/v1/urls/{code} | Deactivate URL |
| GET | /api/v1/stats | Platform statistics |
| GET | /swagger-ui.html | API documentation |

## Key Design Decisions


UUIDs are random — they cause B-tree index fragmentation
in PostgreSQL at scale. Snowflake IDs are time-ordered,
globally unique, and generated without database coordination.


The redirect endpoint is called millions of times per day.
Caching the URL mapping in Redis means 95%+ of redirects
never touch the database — sub-millisecond response times.


301 is cached by browsers permanently — you lose all click
tracking and cannot deactivate links. 302 checks the server
every time giving full control.


Deleting rows destroys analytics history. Setting
isActive=false preserves click data while stopping redirects.

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
