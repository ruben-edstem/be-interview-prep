# Changelog

## 0.1.4

- Add per-API-key rate limiting on `GET /api/quotes/random`: 10 requests per minute by default, configurable through `ratelimit.max-requests` and `ratelimit.window`, with 429 and `Retry-After` when exceeded.

## 0.1.0

- Add base Spring Boot project (web, validation, JPA, H2, Lombok) and README with the question tracker.
