package com.edstem.interviewprep.booking.event;

import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class SlotOfferedEvent {

  private final Long bookingId;
  private final Long slotId;
  private final Long doctorId;
  private final LocalDateTime slotStart;
  private final Instant offerExpiresAt;

  public static SlotOfferedEvent of(
      Long bookingId, Long slotId, Long doctorId, LocalDateTime slotStart, Instant offerExpiresAt) {
    return new SlotOfferedEvent(bookingId, slotId, doctorId, slotStart, offerExpiresAt);
  }
}
