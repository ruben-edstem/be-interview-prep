package com.edstem.interviewprep.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.edstem.interviewprep.booking.dto.request.HoldRequest;
import com.edstem.interviewprep.booking.dto.response.BookingResponse;
import com.edstem.interviewprep.booking.dto.response.SlotResponse;
import com.edstem.interviewprep.booking.entity.BookingStatus;
import com.edstem.interviewprep.booking.entity.Doctor;
import com.edstem.interviewprep.booking.entity.Slot;
import com.edstem.interviewprep.booking.entity.SlotStatus;
import com.edstem.interviewprep.booking.event.BookingConfirmedEvent;
import com.edstem.interviewprep.booking.exception.HoldExpiredException;
import com.edstem.interviewprep.booking.exception.InvalidBookingStateException;
import com.edstem.interviewprep.booking.exception.SlotUnavailableException;
import com.edstem.interviewprep.booking.notification.NotificationSender;
import com.edstem.interviewprep.booking.repository.BookingRepository;
import com.edstem.interviewprep.booking.repository.DoctorRepository;
import com.edstem.interviewprep.booking.repository.SlotRepository;
import com.edstem.interviewprep.booking.service.BookingService;
import com.edstem.interviewprep.booking.service.DoctorService;
import com.edstem.interviewprep.booking.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(BookingFlowIntegrationTest.ClockConfig.class)
class BookingFlowIntegrationTest {

  private static final Instant START = Instant.parse("2026-10-07T09:00:00Z");
  private static final LocalDate DAY = LocalDate.of(2026, 10, 8);
  private static final int PATIENTS = 20;

  @TestConfiguration
  static class ClockConfig {

    @Bean
    @Primary
    MutableClock mutableClock() {
      return new MutableClock(START);
    }
  }

  @Autowired private BookingService bookingService;
  @Autowired private DoctorService doctorService;
  @Autowired private BookingRepository bookingRepository;
  @Autowired private SlotRepository slotRepository;
  @Autowired private DoctorRepository doctorRepository;
  @Autowired private MutableClock clock;
  @MockitoBean private NotificationSender notificationSender;

  private Doctor doctor;
  private Slot slot;

  @BeforeEach
  void setUp() {
    bookingRepository.deleteAll();
    slotRepository.deleteAll();
    doctorRepository.deleteAll();
    clock.set(START);
    doctor = doctorRepository.save(Doctor.named("Dr Rao"));
    LocalDateTime nineAm = DAY.atTime(9, 0);
    slot = slotRepository.save(Slot.available(doctor, nineAm, nineAm.plusMinutes(30)));
  }

  @Test
  void twentyPatientsBookingTheSameSlotAtTheSameMomentLeavesExactlyOneWinner() throws Exception {
    CountDownLatch ready = new CountDownLatch(PATIENTS);
    CountDownLatch go = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(PATIENTS);
    List<Future<Boolean>> attempts = new ArrayList<>();

    for (int i = 0; i < PATIENTS; i++) {
      String patient = "patient-" + i;
      attempts.add(
          pool.submit(
              () -> {
                ready.countDown();
                go.await();
                return tryToBook(patient);
              }));
    }
    ready.await();
    go.countDown();
    int successes = 0;
    for (Future<Boolean> attempt : attempts) {
      if (attempt.get(30, TimeUnit.SECONDS)) {
        successes++;
      }
    }
    pool.shutdown();

    Slot reloaded = slotRepository.findById(slot.getId()).orElseThrow();
    long confirmed =
        bookingRepository.findAll().stream()
            .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
            .count();
    assertThat(successes).isEqualTo(1);
    assertThat(confirmed).isEqualTo(1);
    assertThat(reloaded.getStatus()).isEqualTo(SlotStatus.BOOKED);
    assertThat(bookingRepository.count()).isEqualTo(1);
  }

  @Test
  void anExpiredHoldMakesTheSlotAvailableAgain() {
    bookingService.hold(slot.getId(), new HoldRequest("first"));
    List<SlotResponse> whileHeld = doctorService.findAvailableSlots(doctor.getId(), DAY);
    clock.advance(Duration.ofMinutes(5).plusSeconds(1));

    List<SlotResponse> afterExpiry = doctorService.findAvailableSlots(doctor.getId(), DAY);

    assertThat(whileHeld).isEmpty();
    assertThat(afterExpiry).extracting(SlotResponse::id).containsExactly(slot.getId());
  }

  @Test
  void anotherPatientCanHoldAndConfirmAfterTheFirstHoldExpired() {
    BookingResponse first = bookingService.hold(slot.getId(), new HoldRequest("first"));
    clock.advance(Duration.ofMinutes(5).plusSeconds(1));

    BookingResponse second = bookingService.hold(slot.getId(), new HoldRequest("second"));
    BookingResponse confirmed = bookingService.confirm(second.id());

    assertThat(confirmed.status()).isEqualTo(BookingStatus.CONFIRMED);
    assertThatThrownBy(() -> bookingService.confirm(first.id()))
        .isInstanceOf(HoldExpiredException.class);
  }

  @Test
  void aHoldCannotBeTakenOverBeforeItExpires() {
    bookingService.hold(slot.getId(), new HoldRequest("first"));
    clock.advance(Duration.ofMinutes(4));

    assertThatThrownBy(() -> bookingService.hold(slot.getId(), new HoldRequest("second")))
        .isInstanceOf(SlotUnavailableException.class);
    assertThat(bookingRepository.count()).isEqualTo(1);
  }

  @Test
  void confirmingAnExpiredHoldIsRejectedAndLeavesTheSlotUnbooked() {
    BookingResponse hold = bookingService.hold(slot.getId(), new HoldRequest("first"));
    clock.advance(Duration.ofMinutes(5));

    assertThatThrownBy(() -> bookingService.confirm(hold.id()))
        .isInstanceOf(HoldExpiredException.class);

    Slot reloaded = slotRepository.findById(slot.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isNotEqualTo(SlotStatus.BOOKED);
  }

  @Test
  void cancellingAConfirmedBookingFreesTheSlot() {
    BookingResponse hold = bookingService.hold(slot.getId(), new HoldRequest("first"));
    bookingService.confirm(hold.id());

    bookingService.cancel(hold.id());

    Slot reloaded = slotRepository.findById(slot.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
    assertThat(bookingRepository.findById(hold.id()).orElseThrow().getStatus())
        .isEqualTo(BookingStatus.CANCELLED);
    assertThat(doctorService.findAvailableSlots(doctor.getId(), DAY)).hasSize(1);
  }

  @Test
  void aFreedSlotCanBeBookedByAnotherPatient() {
    BookingResponse first = bookingService.hold(slot.getId(), new HoldRequest("first"));
    bookingService.confirm(first.id());
    bookingService.cancel(first.id());

    BookingResponse second = bookingService.hold(slot.getId(), new HoldRequest("second"));
    BookingResponse confirmed = bookingService.confirm(second.id());

    assertThat(confirmed.status()).isEqualTo(BookingStatus.CONFIRMED);
  }

  @Test
  void aBookingCannotBeCancelledTwiceOrBeforeItIsConfirmed() {
    BookingResponse hold = bookingService.hold(slot.getId(), new HoldRequest("first"));

    assertThatThrownBy(() -> bookingService.cancel(hold.id()))
        .isInstanceOf(InvalidBookingStateException.class);
    bookingService.confirm(hold.id());
    bookingService.cancel(hold.id());
    assertThatThrownBy(() -> bookingService.cancel(hold.id()))
        .isInstanceOf(InvalidBookingStateException.class);
  }

  @Test
  void aConfirmedBookingCannotBeConfirmedAgain() {
    BookingResponse hold = bookingService.hold(slot.getId(), new HoldRequest("first"));
    bookingService.confirm(hold.id());

    assertThatThrownBy(() -> bookingService.confirm(hold.id()))
        .isInstanceOf(InvalidBookingStateException.class);
  }

  @Test
  void confirmingSendsTheNotificationOffTheCallingThread() {
    AtomicReference<String> senderThread = new AtomicReference<>();
    doAnswer(
            invocation -> {
              senderThread.set(Thread.currentThread().getName());
              return null;
            })
        .when(notificationSender)
        .send(any(BookingConfirmedEvent.class));
    BookingResponse hold = bookingService.hold(slot.getId(), new HoldRequest("first"));

    bookingService.confirm(hold.id());

    verify(notificationSender, timeout(5000)).send(any(BookingConfirmedEvent.class));
    assertThat(senderThread.get()).isNotEqualTo(Thread.currentThread().getName());
  }

  @Test
  void noNotificationIsSentWhenTheBookingWasNotSaved() {
    BookingResponse hold = bookingService.hold(slot.getId(), new HoldRequest("first"));
    clock.advance(Duration.ofMinutes(6));

    assertThatThrownBy(() -> bookingService.confirm(hold.id()))
        .isInstanceOf(HoldExpiredException.class);

    verify(notificationSender, after(500).never()).send(any(BookingConfirmedEvent.class));
  }

  @Test
  void holdingASlotDoesNotSendANotification() {
    BookingResponse hold = bookingService.hold(slot.getId(), new HoldRequest("first"));

    verify(notificationSender, after(300).never()).send(any(BookingConfirmedEvent.class));
    assertThat(hold.status()).isEqualTo(BookingStatus.HELD);
  }

  private boolean tryToBook(String patient) {
    try {
      BookingResponse hold = bookingService.hold(slot.getId(), new HoldRequest(patient));
      bookingService.confirm(hold.id());
      return true;
    } catch (SlotUnavailableException ex) {
      return false;
    }
  }
}
