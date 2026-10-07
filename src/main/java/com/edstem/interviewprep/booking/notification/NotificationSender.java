package com.edstem.interviewprep.booking.notification;

import com.edstem.interviewprep.booking.event.BookingConfirmedEvent;
import com.edstem.interviewprep.booking.event.SlotOfferedEvent;

public interface NotificationSender {

  void send(BookingConfirmedEvent event);

  void send(SlotOfferedEvent event);
}
