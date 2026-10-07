package com.edstem.interviewprep.booking.exception;

import java.time.Duration;
import org.springframework.http.HttpStatus;

public class InvalidSlotRangeException extends BookingException {

  public InvalidSlotRangeException(Duration slotDuration) {
    super(
        HttpStatus.BAD_REQUEST,
        "INVALID_SLOT_RANGE",
        "The end time must be after the start time and cover whole %d-minute slots"
            .formatted(slotDuration.toMinutes()));
  }
}
