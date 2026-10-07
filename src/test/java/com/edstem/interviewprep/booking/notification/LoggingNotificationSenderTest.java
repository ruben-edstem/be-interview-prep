package com.edstem.interviewprep.booking.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.booking.event.BookingConfirmedEvent;
import com.edstem.interviewprep.booking.event.SlotOfferedEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class LoggingNotificationSenderTest {

  @Test
  void logsTheConfirmedBookingWithoutPatientDetails(CapturedOutput output) {
    BookingConfirmedEvent event =
        BookingConfirmedEvent.of(99L, 10L, 3L, LocalDateTime.of(2026, 10, 8, 9, 0));

    new LoggingNotificationSender().send(event);

    assertThat(output.getOut())
        .contains("Booking 99 confirmed for slot 10 with doctor 3 at 2026-10-08T09:00");
  }

  @Test
  void logsTheOfferWithTheBookingToConfirm(CapturedOutput output) {
    SlotOfferedEvent event =
        SlotOfferedEvent.of(
            77L,
            10L,
            3L,
            LocalDateTime.of(2026, 10, 8, 9, 0),
            Instant.parse("2026-10-07T09:05:00Z"));

    new LoggingNotificationSender().send(event);

    assertThat(output.getOut())
        .contains(
            "Slot 10 with doctor 3 at 2026-10-08T09:00 offered through booking 77, "
                + "confirm before 2026-10-07T09:05:00Z");
  }
}
