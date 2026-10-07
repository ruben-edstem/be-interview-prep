package com.edstem.interviewprep.booking.notification;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.edstem.interviewprep.booking.event.BookingConfirmedEvent;
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
}
