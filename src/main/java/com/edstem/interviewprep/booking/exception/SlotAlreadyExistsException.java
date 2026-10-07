package com.edstem.interviewprep.booking.exception;

import org.springframework.http.HttpStatus;

public class SlotAlreadyExistsException extends BookingException {

  public SlotAlreadyExistsException(Long doctorId) {
    super(
        HttpStatus.CONFLICT,
        "SLOT_ALREADY_EXISTS",
        "Doctor %d already has slots in that time range".formatted(doctorId));
  }
}
