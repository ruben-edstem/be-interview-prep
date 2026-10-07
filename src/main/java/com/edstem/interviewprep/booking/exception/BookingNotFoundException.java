package com.edstem.interviewprep.booking.exception;

import org.springframework.http.HttpStatus;

public class BookingNotFoundException extends BookingException {

  public BookingNotFoundException(Long id) {
    super(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Booking %d not found".formatted(id));
  }
}
