package com.edstem.interviewprep.booking.exception;

import org.springframework.http.HttpStatus;

public class InvalidBookingStateException extends BookingException {

  public InvalidBookingStateException(Long bookingId, String action) {
    super(
        HttpStatus.CONFLICT,
        "INVALID_BOOKING_STATE",
        "Booking %d cannot be %s in its current state".formatted(bookingId, action));
  }
}
