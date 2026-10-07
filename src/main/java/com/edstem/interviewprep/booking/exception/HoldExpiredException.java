package com.edstem.interviewprep.booking.exception;

import org.springframework.http.HttpStatus;

public class HoldExpiredException extends BookingException {

  public HoldExpiredException(Long bookingId) {
    super(
        HttpStatus.CONFLICT,
        "HOLD_EXPIRED",
        "The hold on booking %d expired before it was confirmed".formatted(bookingId));
  }
}
