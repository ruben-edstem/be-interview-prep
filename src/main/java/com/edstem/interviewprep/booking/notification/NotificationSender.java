package com.edstem.interviewprep.booking.notification;

import com.edstem.interviewprep.booking.event.BookingConfirmedEvent;

public interface NotificationSender {

  void send(BookingConfirmedEvent event);
}
