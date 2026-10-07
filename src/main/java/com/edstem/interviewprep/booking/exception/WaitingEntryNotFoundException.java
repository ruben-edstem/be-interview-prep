package com.edstem.interviewprep.booking.exception;

import org.springframework.http.HttpStatus;

public class WaitingEntryNotFoundException extends BookingException {

  public WaitingEntryNotFoundException(Long id) {
    super(
        HttpStatus.NOT_FOUND,
        "WAITING_ENTRY_NOT_FOUND",
        "Waiting list entry %d not found".formatted(id));
  }
}
