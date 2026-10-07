package com.edstem.interviewprep.booking.event;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class BookingConfirmedEvent {

  private final Long bookingId;
  private final Long slotId;
  private final Long doctorId;
  private final LocalDateTime slotStart;

  public static BookingConfirmedEvent of(
      Long bookingId, Long slotId, Long doctorId, LocalDateTime slotStart) {
    return new BookingConfirmedEvent(bookingId, slotId, doctorId, slotStart);
  }
}
