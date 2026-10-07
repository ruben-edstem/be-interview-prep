package com.edstem.interviewprep.booking.exception;

import org.springframework.http.HttpStatus;

public class SlotAvailableException extends BookingException {

  public SlotAvailableException(Long slotId) {
    super(
        HttpStatus.CONFLICT,
        "SLOT_AVAILABLE",
        "Slot %d is available, so hold it instead of joining the waiting list".formatted(slotId));
  }
}
