# Changelog

## 0.1.9

- Add thumbnails for uploaded JPEG and PNG images: a proportional preview of at most 200 pixels on the longest side, fetched with `GET /files/{id}/thumbnail`, removed with the file, and left out (without failing the upload) when it cannot be created. PDFs get none and return a clear error.

## 0.1.6

- Add the run and test steps, a title and the video line to the README.

## 0.1.5

- Add Q5 Appointment Booking: doctors with 30-minute slots, a 5-minute hold then confirm flow, cancellation that frees the slot, and a confirmation notification sent after the booking is saved. Double-booking is prevented by atomic conditional updates and proven by a 20-patient concurrency test.

## 0.1.4

- Add per-API-key rate limiting on `GET /api/quotes/random`: 10 requests per minute by default, configurable through `ratelimit.max-requests` and `ratelimit.window`, with 429 and `Retry-After` when exceeded.

## 0.1.3

- Add file upload service: upload, list, download and delete JPEG, PNG and PDF files up to 5 MB, with content-based type checks and path-traversal-safe storage.

## 0.1.2

- Add the expense tracker (Q2): add, list, update and delete expenses with exact two-decimal amounts, filter the list by date range and category, and a monthly summary with the total per category and overall.

## 0.1.1

- Add Q1 Library API: book CRUD with title/author search, atomic borrow and return, and a consistent JSON error format.

## 0.1.0

- Add base Spring Boot project (web, validation, JPA, H2, Lombok) and README with the question tracker.
