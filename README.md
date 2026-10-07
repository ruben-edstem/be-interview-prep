# be-interview-prep

Backend Interview Prep Assignment, Set 2 (Java / Spring Boot). Five features, one pull request each.

| # | Question | PR link |
|---|----------|---------|
| 1 | Library API | [PR](https://github.com/ruben-edstem/be-interview-prep/pull/6) |
| 2 | Expense Tracker | [PR](https://github.com/ruben-edstem/be-interview-prep/pull/7) |
| 3 | File Upload Service | [PR](https://github.com/ruben-edstem/be-interview-prep/pull/8) |
| 4 | API Rate Limiting | [PR](https://github.com/ruben-edstem/be-interview-prep/pull/9) |
| 5 | Appointment Booking | [PR](https://github.com/ruben-edstem/be-interview-prep/pull/10) |

Video:

## Run

Requires Java 17 or newer. The Maven wrapper downloads Maven itself, and the app uses an in-memory H2 database, so nothing else needs installing.

```bash
./mvnw spring-boot:run
```

The app listens on `http://localhost:8080`. Uploaded files are stored in `./uploads`; set `FILE_STORAGE_DIR` to change the folder.

## Test

```bash
./mvnw test
```
