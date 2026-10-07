package com.edstem.interviewprep.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.edstem.interviewprep.booking.dto.request.HoldRequest;
import com.edstem.interviewprep.booking.dto.request.JoinWaitingListRequest;
import com.edstem.interviewprep.booking.dto.response.BookingResponse;
import com.edstem.interviewprep.booking.dto.response.SlotResponse;
import com.edstem.interviewprep.booking.dto.response.WaitingEntryResponse;
import com.edstem.interviewprep.booking.entity.Booking;
import com.edstem.interviewprep.booking.entity.BookingStatus;
import com.edstem.interviewprep.booking.entity.Doctor;
import com.edstem.interviewprep.booking.entity.Slot;
import com.edstem.interviewprep.booking.entity.SlotStatus;
import com.edstem.interviewprep.booking.entity.WaitingEntry;
import com.edstem.interviewprep.booking.entity.WaitingStatus;
import com.edstem.interviewprep.booking.event.BookingConfirmedEvent;
import com.edstem.interviewprep.booking.event.SlotOfferedEvent;
import com.edstem.interviewprep.booking.exception.AlreadyWaitingException;
import com.edstem.interviewprep.booking.exception.InvalidBookingStateException;
import com.edstem.interviewprep.booking.exception.SlotAvailableException;
import com.edstem.interviewprep.booking.exception.SlotUnavailableException;
import com.edstem.interviewprep.booking.notification.NotificationSender;
import com.edstem.interviewprep.booking.repository.BookingRepository;
import com.edstem.interviewprep.booking.repository.DoctorRepository;
import com.edstem.interviewprep.booking.repository.SlotRepository;
import com.edstem.interviewprep.booking.repository.WaitingEntryRepository;
import com.edstem.interviewprep.booking.service.BookingService;
import com.edstem.interviewprep.booking.service.DoctorService;
import com.edstem.interviewprep.booking.service.WaitingListService;
import com.edstem.interviewprep.booking.service.WaitingListSweeper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "booking.sweep-interval=PT24H")
@Import(WaitingListIntegrationTest.ClockConfig.class)
class WaitingListIntegrationTest {

  private static final Instant START = Instant.parse("2026-10-07T09:00:00Z");
  private static final LocalDate DAY = LocalDate.of(2026, 10, 8);
  private static final Duration PAST_HOLD = Duration.ofMinutes(5).plusSeconds(1);
  private static final int NEW_PATIENTS = 20;

  @TestConfiguration
  static class ClockConfig {

    @Bean
    @Primary
    MutableClock mutableClock() {
      return new MutableClock(START);
    }
  }

  @Autowired private BookingService bookingService;
  @Autowired private WaitingListService waitingListService;
  @Autowired private WaitingListSweeper sweeper;
  @Autowired private DoctorService doctorService;
  @Autowired private BookingRepository bookingRepository;
  @Autowired private SlotRepository slotRepository;
  @Autowired private DoctorRepository doctorRepository;
  @Autowired private WaitingEntryRepository waitingEntryRepository;
  @Autowired private MutableClock clock;
  @Autowired private PlatformTransactionManager transactionManager;
  @MockitoBean private NotificationSender notificationSender;

  private Doctor doctor;
  private Slot slot;
  private BookingResponse holder;

  @BeforeEach
  void setUp() {
    waitingEntryRepository.deleteAll();
    bookingRepository.deleteAll();
    slotRepository.deleteAll();
    doctorRepository.deleteAll();
    clock.set(START);
    doctor = doctorRepository.save(Doctor.named("Dr Rao"));
    LocalDateTime nineAm = DAY.atTime(9, 0);
    slot = slotRepository.save(Slot.available(doctor, nineAm, nineAm.plusMinutes(30)));
    holder = confirmedBooking("holder");
  }

  @Test
  void aPatientCanOnlyJoinTheWaitingListOfASlotThatIsTaken() {
    Slot freeSlot =
        slotRepository.save(Slot.available(doctor, DAY.atTime(10, 0), DAY.atTime(10, 30)));

    assertThatThrownBy(
            () -> waitingListService.join(freeSlot.getId(), new JoinWaitingListRequest("Asha")))
        .isInstanceOf(SlotAvailableException.class);
    WaitingEntryResponse joined = join("Asha");

    assertThat(joined.status()).isEqualTo(WaitingStatus.WAITING);
    assertThatThrownBy(() -> join("Asha")).isInstanceOf(AlreadyWaitingException.class);
  }

  @Test
  void cancellingOffersTheSlotToTheLongestWaitingPatient() {
    join("Asha");
    join("Ben");
    join("Cara");

    bookingService.cancel(holder.id());

    Booking offer = heldBookingFor("Asha");
    Slot reloaded = slotRepository.findById(slot.getId()).orElseThrow();
    assertThat(entryOf("Asha").getStatus()).isEqualTo(WaitingStatus.OFFERED);
    assertThat(entryOf("Asha").getOfferedBookingId()).isEqualTo(offer.getId());
    assertThat(entryOf("Ben").getStatus()).isEqualTo(WaitingStatus.WAITING);
    assertThat(entryOf("Cara").getStatus()).isEqualTo(WaitingStatus.WAITING);
    assertThat(reloaded.getStatus()).isEqualTo(SlotStatus.HELD);
    assertThat(reloaded.getActiveBookingId()).isEqualTo(offer.getId());
    assertThat(lastOffer().getBookingId()).isEqualTo(offer.getId());
  }

  @Test
  void anOfferThatIsNotTakenPassesToTheNextPatientUntilTheListIsEmpty() {
    join("Asha");
    join("Ben");
    bookingService.cancel(holder.id());

    clock.advance(PAST_HOLD);
    sweeper.advanceQueues();

    assertThat(waitingEntryRepository.findAll())
        .extracting(WaitingEntry::getPatientName)
        .containsExactly("Ben");
    assertThat(entryOf("Ben").getStatus()).isEqualTo(WaitingStatus.OFFERED);
    assertThat(slotRepository.findById(slot.getId()).orElseThrow().getActiveBookingId())
        .isEqualTo(heldBookingFor("Ben").getId());

    clock.advance(PAST_HOLD);
    sweeper.advanceQueues();

    assertThat(waitingEntryRepository.findAll()).isEmpty();
    assertThat(doctorService.findAvailableSlots(doctor.getId(), DAY))
        .extracting(SlotResponse::id)
        .contains(slot.getId());
    BookingResponse newcomer = bookingService.hold(slot.getId(), new HoldRequest("Newcomer"));
    assertThat(newcomer.status()).isEqualTo(BookingStatus.HELD);
  }

  @Test
  void theOfferedPatientConfirmsAndGetsTheSlotWhileOthersKeepWaiting() {
    join("Asha");
    join("Ben");
    bookingService.cancel(holder.id());
    Booking offer = heldBookingFor("Asha");

    BookingResponse confirmed = bookingService.confirm(offer.getId());

    Slot reloaded = slotRepository.findById(slot.getId()).orElseThrow();
    assertThat(confirmed.status()).isEqualTo(BookingStatus.CONFIRMED);
    assertThat(confirmed.patientName()).isEqualTo("Asha");
    assertThat(reloaded.getStatus()).isEqualTo(SlotStatus.BOOKED);
    assertThat(waitingEntryRepository.findAll())
        .extracting(WaitingEntry::getPatientName)
        .containsExactly("Ben");
    ArgumentCaptor<BookingConfirmedEvent> sent =
        ArgumentCaptor.forClass(BookingConfirmedEvent.class);
    verify(notificationSender, timeout(5000).atLeast(2)).send(sent.capture());
    assertThat(sent.getAllValues())
        .extracting(BookingConfirmedEvent::getBookingId)
        .contains(offer.getId());
  }

  @Test
  void aNewPatientCannotJumpTheQueueEvenAfterAnOfferRanOut() {
    join("Asha");
    bookingService.cancel(holder.id());

    assertThatThrownBy(() -> bookingService.hold(slot.getId(), new HoldRequest("Newcomer")))
        .isInstanceOf(SlotUnavailableException.class);
    clock.advance(PAST_HOLD);

    assertThatThrownBy(() -> bookingService.hold(slot.getId(), new HoldRequest("Newcomer")))
        .isInstanceOf(SlotUnavailableException.class);
    assertThat(doctorService.findAvailableSlots(doctor.getId(), DAY)).isEmpty();
  }

  @Test
  void aPatientWhoLeavesWhileWaitingIsSkipped() {
    join("Asha");
    WaitingEntryResponse ben = join("Ben");
    join("Cara");

    waitingListService.leave(entryOf("Asha").getId());
    waitingListService.leave(ben.id());
    bookingService.cancel(holder.id());

    assertThat(entryOf("Cara").getStatus()).isEqualTo(WaitingStatus.OFFERED);
    assertThat(waitingEntryRepository.count()).isEqualTo(1);
  }

  @Test
  void aPatientWhoDeclinesAnOfferPassesItOnAndCannotConfirmIt() {
    join("Asha");
    join("Ben");
    bookingService.cancel(holder.id());
    Booking declined = heldBookingFor("Asha");

    waitingListService.leave(entryOf("Asha").getId());

    assertThat(entryOf("Ben").getStatus()).isEqualTo(WaitingStatus.OFFERED);
    assertThat(bookingRepository.findById(declined.getId()).orElseThrow().getStatus())
        .isEqualTo(BookingStatus.CANCELLED);
    assertThat(slotRepository.findById(slot.getId()).orElseThrow().getActiveBookingId())
        .isEqualTo(heldBookingFor("Ben").getId());
    assertThatThrownBy(() -> bookingService.confirm(declined.getId()))
        .isInstanceOf(InvalidBookingStateException.class);
  }

  @Test
  void aPatientWhoDeclinesTheLastOfferFreesTheSlotForEveryone() {
    join("Asha");
    bookingService.cancel(holder.id());

    waitingListService.leave(entryOf("Asha").getId());

    assertThat(waitingEntryRepository.count()).isZero();
    assertThat(doctorService.findAvailableSlots(doctor.getId(), DAY))
        .extracting(SlotResponse::id)
        .containsExactly(slot.getId());
  }

  @Test
  void aSlotThatBecameFreeWithoutAnOfferIsOfferedBySweeper() {
    join("Asha");
    bookingService.cancel(holder.id());
    join("Late");
    Booking ashasOffer = heldBookingFor("Asha");
    Long ashasEntryId = entryOf("Asha").getId();
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              slotRepository.freeHold(slot.getId(), ashasOffer.getId());
              waitingEntryRepository.deleteOffered(ashasEntryId);
            });
    assertThat(entryOf("Late").getStatus()).isEqualTo(WaitingStatus.WAITING);
    assertThat(slotRepository.findById(slot.getId()).orElseThrow().getStatus())
        .isEqualTo(SlotStatus.AVAILABLE);

    sweeper.advanceQueues();

    assertThat(entryOf("Late").getStatus()).isEqualTo(WaitingStatus.OFFERED);
  }

  @Test
  void aWaitingPatientAndTwentyNewPatientsRacingForAFreedSlotLeaveOnlyTheOffer() throws Exception {
    join("Asha");
    CountDownLatch ready = new CountDownLatch(NEW_PATIENTS + 1);
    CountDownLatch go = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(NEW_PATIENTS + 1);
    List<Future<Boolean>> attempts = new ArrayList<>();

    attempts.add(
        pool.submit(
            () -> {
              ready.countDown();
              go.await();
              bookingService.cancel(holder.id());
              return false;
            }));
    for (int i = 0; i < NEW_PATIENTS; i++) {
      String patient = "new-" + i;
      attempts.add(
          pool.submit(
              () -> {
                ready.countDown();
                go.await();
                return tryToHold(patient);
              }));
    }
    ready.await();
    go.countDown();
    int newPatientsThatGotTheSlot = 0;
    for (Future<Boolean> attempt : attempts) {
      if (attempt.get(30, TimeUnit.SECONDS)) {
        newPatientsThatGotTheSlot++;
      }
    }
    pool.shutdown();

    Slot reloaded = slotRepository.findById(slot.getId()).orElseThrow();
    Booking offer = heldBookingFor("Asha");
    assertThat(newPatientsThatGotTheSlot).isZero();
    assertThat(reloaded.getActiveBookingId()).isEqualTo(offer.getId());
    assertThat(bookingRepository.findAll())
        .filteredOn(booking -> booking.getStatus() == BookingStatus.HELD)
        .extracting(Booking::getPatientName)
        .containsExactly("Asha");
  }

  @Test
  void aCancelAndTenSweepersOfferingAtTheSameMomentCreateExactlyOneOffer() throws Exception {
    join("Asha");
    join("Ben");
    join("Cara");
    int offerers = 10;
    CountDownLatch ready = new CountDownLatch(offerers + 1);
    CountDownLatch go = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(offerers + 1);
    List<Future<?>> tasks = new ArrayList<>();

    tasks.add(
        pool.submit(
            () -> {
              ready.countDown();
              go.await();
              bookingService.cancel(holder.id());
              return null;
            }));
    for (int i = 0; i < offerers; i++) {
      tasks.add(
          pool.submit(
              () -> {
                ready.countDown();
                go.await();
                return waitingListService.offerNext(slot.getId());
              }));
    }
    ready.await();
    go.countDown();
    for (Future<?> task : tasks) {
      task.get(30, TimeUnit.SECONDS);
    }
    pool.shutdown();

    List<Booking> heldOffers =
        bookingRepository.findAll().stream()
            .filter(booking -> booking.getStatus() == BookingStatus.HELD)
            .toList();
    Slot reloaded = slotRepository.findById(slot.getId()).orElseThrow();
    assertThat(heldOffers).hasSize(1);
    assertThat(heldOffers.get(0).getPatientName()).isEqualTo("Asha");
    assertThat(reloaded.getActiveBookingId()).isEqualTo(heldOffers.get(0).getId());
    assertThat(bookingRepository.count()).isEqualTo(2);
    assertThat(waitingEntryRepository.findAll())
        .filteredOn(entry -> entry.getStatus() == WaitingStatus.OFFERED)
        .extracting(WaitingEntry::getPatientName)
        .containsExactly("Asha");
  }

  private BookingResponse confirmedBooking(String patient) {
    BookingResponse hold = bookingService.hold(slot.getId(), new HoldRequest(patient));
    return bookingService.confirm(hold.id());
  }

  private WaitingEntryResponse join(String patient) {
    return waitingListService.join(slot.getId(), new JoinWaitingListRequest(patient));
  }

  private WaitingEntry entryOf(String patient) {
    return waitingEntryRepository.findAll().stream()
        .filter(entry -> entry.getPatientName().equals(patient))
        .findFirst()
        .orElseThrow();
  }

  private Booking heldBookingFor(String patient) {
    return bookingRepository.findAll().stream()
        .filter(booking -> booking.getPatientName().equals(patient))
        .filter(booking -> booking.getStatus() == BookingStatus.HELD)
        .findFirst()
        .orElseThrow();
  }

  private SlotOfferedEvent lastOffer() {
    ArgumentCaptor<SlotOfferedEvent> offers = ArgumentCaptor.forClass(SlotOfferedEvent.class);
    verify(notificationSender, timeout(5000)).send(offers.capture());
    return offers.getValue();
  }

  private boolean tryToHold(String patient) {
    try {
      bookingService.hold(slot.getId(), new HoldRequest(patient));
      return true;
    } catch (SlotUnavailableException ex) {
      return false;
    }
  }
}
