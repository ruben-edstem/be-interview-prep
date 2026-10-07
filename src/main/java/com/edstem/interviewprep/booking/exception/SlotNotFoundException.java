package com.edstem.interviewprep.booking.exception;

import org.springframework.http.HttpStatus;

public class SlotNotFoundException extends BookingException {

  public SlotNotFoundException(Long id) {
    super(HttpStatus.NOT_FOUND, "SLOT_NOT_FOUND", "Slot %d not found".formatted(id));
  }
}
