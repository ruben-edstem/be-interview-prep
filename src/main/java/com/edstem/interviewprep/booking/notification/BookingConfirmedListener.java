package com.edstem.interviewprep.booking.notification;

import com.edstem.interviewprep.booking.event.BookingConfirmedEvent;
import com.edstem.interviewprep.booking.event.SlotOfferedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingConfirmedListener {

  private final NotificationSender notificationSender;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onBookingConfirmed(BookingConfirmedEvent event) {
    try {
      notificationSender.send(event);
    } catch (RuntimeException ex) {
      log.error("Failed to send confirmation for booking {}", event.getBookingId(), ex);
    }
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onSlotOffered(SlotOfferedEvent event) {
    try {
      notificationSender.send(event);
    } catch (RuntimeException ex) {
      log.error("Failed to send offer for booking {}", event.getBookingId(), ex);
    }
  }
}
