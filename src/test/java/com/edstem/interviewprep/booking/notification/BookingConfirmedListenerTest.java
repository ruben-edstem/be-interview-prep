package com.edstem.interviewprep.booking.notification;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.edstem.interviewprep.booking.event.BookingConfirmedEvent;
import com.edstem.interviewprep.booking.event.SlotOfferedEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingConfirmedListenerTest {

  private static final BookingConfirmedEvent EVENT =
      BookingConfirmedEvent.of(99L, 10L, 3L, LocalDateTime.of(2026, 10, 8, 9, 0));

  private static final SlotOfferedEvent OFFER =
      SlotOfferedEvent.of(
          77L, 10L, 3L, LocalDateTime.of(2026, 10, 8, 9, 0), Instant.parse("2026-10-07T09:05:00Z"));

  @Mock private NotificationSender notificationSender;
  @InjectMocks private BookingConfirmedListener listener;

  @Test
  void handsTheEventToTheSender() {
    listener.onBookingConfirmed(EVENT);

    verify(notificationSender).send(EVENT);
  }

  @Test
  void aFailingSenderDoesNotEscapeTheListener() {
    doThrow(new IllegalStateException("smtp down")).when(notificationSender).send(EVENT);

    assertThatCode(() -> listener.onBookingConfirmed(EVENT)).doesNotThrowAnyException();
  }

  @Test
  void handsAnOfferToTheSender() {
    listener.onSlotOffered(OFFER);

    verify(notificationSender).send(OFFER);
  }

  @Test
  void aFailingSenderDoesNotEscapeTheListenerWhenOffering() {
    doThrow(new IllegalStateException("smtp down")).when(notificationSender).send(OFFER);

    assertThatCode(() -> listener.onSlotOffered(OFFER)).doesNotThrowAnyException();
  }
}
