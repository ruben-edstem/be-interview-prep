package com.edstem.interviewprep.booking.notification;

import com.edstem.interviewprep.booking.event.BookingConfirmedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingNotificationSender implements NotificationSender {

  @Override
  public void send(BookingConfirmedEvent event) {
    log.info(
        "Booking {} confirmed for slot {} with doctor {} at {}",
        event.getBookingId(),
        event.getSlotId(),
        event.getDoctorId(),
        event.getSlotStart());
  }
}
