# Changelog

## 0.1.5

- Add Q5 Appointment Booking: doctors with 30-minute slots, a 5-minute hold then confirm flow, cancellation that frees the slot, and a confirmation notification sent after the booking is saved. Double-booking is prevented by atomic conditional updates and proven by a 20-patient concurrency test.

## 0.1.2

- Add the expense tracker (Q2): add, list, update and delete expenses with exact two-decimal amounts, filter the list by date range and category, and a monthly summary with the total per category and overall.

## 0.1.1

- Add Q1 Library API: book CRUD with title/author search, atomic borrow and return, and a consistent JSON error format.

## 0.1.0

- Add base Spring Boot project (web, validation, JPA, H2, Lombok) and README with the question tracker.
