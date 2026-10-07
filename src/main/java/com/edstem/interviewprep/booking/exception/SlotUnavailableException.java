package com.edstem.interviewprep.booking.exception;

import org.springframework.http.HttpStatus;

public class SlotUnavailableException extends BookingException {

  public SlotUnavailableException(Long slotId) {
    super(
        HttpStatus.CONFLICT,
        "SLOT_UNAVAILABLE",
        "Slot %d is held or booked by someone else".formatted(slotId));
  }
}
