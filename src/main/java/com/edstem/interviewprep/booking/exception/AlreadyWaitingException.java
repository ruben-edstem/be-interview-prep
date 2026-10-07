package com.edstem.interviewprep.booking.exception;

import org.springframework.http.HttpStatus;

public class AlreadyWaitingException extends BookingException {

  public AlreadyWaitingException(Long slotId) {
    super(
        HttpStatus.CONFLICT,
        "ALREADY_WAITING",
        "That patient is already on the waiting list for slot %d".formatted(slotId));
  }
}
