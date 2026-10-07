package com.edstem.interviewprep.booking.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.booking.entity.Doctor;
import com.edstem.interviewprep.booking.entity.Slot;
import com.edstem.interviewprep.booking.entity.SlotStatus;
import com.edstem.interviewprep.booking.entity.WaitingEntry;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class SlotRepositoryTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 8);
  private static final Instant NOW = Instant.parse("2026-10-07T09:00:00Z");
  private static final Duration HOLD = Duration.ofMinutes(5);

  @Autowired private SlotRepository slotRepository;
  @Autowired private TestEntityManager entityManager;

  private Doctor doctor;
  private Slot nineAm;
  private Slot nineThirty;

  @BeforeEach
  void setUp() {
    doctor = entityManager.persist(Doctor.named("Dr Rao"));
    nineAm = entityManager.persist(slotAt(doctor, DAY.atTime(9, 0)));
    nineThirty = entityManager.persist(slotAt(doctor, DAY.atTime(9, 30)));
  }

  @Test
  void holdClaimsAnAvailableSlot() {
    int claimed = slotRepository.hold(nineAm.getId(), 7L, NOW, NOW.plus(HOLD));

    Slot reloaded = slotRepository.findById(nineAm.getId()).orElseThrow();
    assertThat(claimed).isEqualTo(1);
    assertThat(reloaded.getStatus()).isEqualTo(SlotStatus.HELD);
    assertThat(reloaded.getActiveBookingId()).isEqualTo(7L);
    assertThat(reloaded.getHeldUntil()).isEqualTo(NOW.plus(HOLD));
  }

  @Test
  void holdIsRefusedWhileAnotherHoldIsStillRunning() {
    slotRepository.hold(nineAm.getId(), 7L, NOW, NOW.plus(HOLD));
    Instant later = NOW.plus(Duration.ofMinutes(4));

    int claimed = slotRepository.hold(nineAm.getId(), 8L, later, later.plus(HOLD));

    Slot reloaded = slotRepository.findById(nineAm.getId()).orElseThrow();
    assertThat(claimed).isZero();
    assertThat(reloaded.getActiveBookingId()).isEqualTo(7L);
  }

  @Test
  void holdTakesOverAnExpiredHold() {
    slotRepository.hold(nineAm.getId(), 7L, NOW, NOW.plus(HOLD));
    Instant later = NOW.plus(HOLD);

    int claimed = slotRepository.hold(nineAm.getId(), 8L, later, later.plus(HOLD));

    Slot reloaded = slotRepository.findById(nineAm.getId()).orElseThrow();
    assertThat(claimed).isEqualTo(1);
    assertThat(reloaded.getActiveBookingId()).isEqualTo(8L);
  }

  @Test
  void holdIsRefusedOnABookedSlot() {
    slotRepository.hold(nineAm.getId(), 7L, NOW, NOW.plus(HOLD));
    slotRepository.book(nineAm.getId(), 7L);
    Instant muchLater = NOW.plus(Duration.ofHours(1));

    int claimed = slotRepository.hold(nineAm.getId(), 8L, muchLater, muchLater.plus(HOLD));

    assertThat(claimed).isZero();
  }

  @Test
  void bookRequiresTheHoldOfTheSameBooking() {
    slotRepository.hold(nineAm.getId(), 7L, NOW, NOW.plus(HOLD));

    int wrongBooking = slotRepository.book(nineAm.getId(), 8L);
    int rightBooking = slotRepository.book(nineAm.getId(), 7L);

    Slot reloaded = slotRepository.findById(nineAm.getId()).orElseThrow();
    assertThat(wrongBooking).isZero();
    assertThat(rightBooking).isEqualTo(1);
    assertThat(reloaded.getStatus()).isEqualTo(SlotStatus.BOOKED);
    assertThat(reloaded.getHeldUntil()).isNull();
  }

  @Test
  void bookIsRefusedOnASlotThatIsNotHeld() {
    int booked = slotRepository.book(nineAm.getId(), 7L);

    assertThat(booked).isZero();
  }

  @Test
  void releaseFreesABookedSlot() {
    slotRepository.hold(nineAm.getId(), 7L, NOW, NOW.plus(HOLD));
    slotRepository.book(nineAm.getId(), 7L);

    int released = slotRepository.release(nineAm.getId(), 7L);

    Slot reloaded = slotRepository.findById(nineAm.getId()).orElseThrow();
    assertThat(released).isEqualTo(1);
    assertThat(reloaded.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
    assertThat(reloaded.getActiveBookingId()).isNull();
  }

  @Test
  void releaseIsRefusedForAnotherBookingOrAnUnbookedSlot() {
    slotRepository.hold(nineAm.getId(), 7L, NOW, NOW.plus(HOLD));

    int unbooked = slotRepository.release(nineAm.getId(), 7L);
    slotRepository.book(nineAm.getId(), 7L);
    int otherBooking = slotRepository.release(nineAm.getId(), 8L);

    assertThat(unbooked).isZero();
    assertThat(otherBooking).isZero();
  }

  @Test
  void findAvailableListsFreeAndExpiredHoldSlotsInTimeOrder() {
    slotRepository.hold(nineAm.getId(), 7L, NOW, NOW.plus(HOLD));
    Instant afterExpiry = NOW.plus(HOLD);

    List<Slot> available = findForDay(DAY, afterExpiry);

    assertThat(available)
        .extracting(Slot::getId)
        .containsExactly(nineAm.getId(), nineThirty.getId());
  }

  @Test
  void findAvailableSkipsHeldAndBookedSlots() {
    slotRepository.hold(nineAm.getId(), 7L, NOW, NOW.plus(HOLD));
    slotRepository.hold(nineThirty.getId(), 8L, NOW, NOW.plus(HOLD));
    slotRepository.book(nineThirty.getId(), 8L);
    Instant beforeExpiry = NOW.plus(Duration.ofMinutes(1));

    List<Slot> available = findForDay(DAY, beforeExpiry);

    assertThat(available).isEmpty();
  }

  @Test
  void findAvailableOnlyReturnsTheRequestedDaysSlotsOfThatDoctor() {
    entityManager.persist(slotAt(doctor, DAY.plusDays(1).atTime(9, 0)));
    Doctor other = entityManager.persist(Doctor.named("Dr Iyer"));
    entityManager.persist(slotAt(other, DAY.atTime(10, 0)));

    List<Slot> available = findForDay(DAY, NOW);

    assertThat(available)
        .extracting(Slot::getId)
        .containsExactly(nineAm.getId(), nineThirty.getId());
  }

  @Test
  void existsByDoctorIdAndStartTimeBetweenDetectsAnOverlap() {
    boolean overlapping =
        slotRepository.existsByDoctorIdAndStartTimeBetween(
            doctor.getId(), DAY.atTime(9, 30), DAY.atTime(10, 0));
    boolean separate =
        slotRepository.existsByDoctorIdAndStartTimeBetween(
            doctor.getId(), DAY.atTime(10, 0), DAY.atTime(11, 0));

    assertThat(overlapping).isTrue();
    assertThat(separate).isFalse();
  }

  @Test
  void holdIsRefusedWhileSomeoneIsWaitingForTheSlot() {
    entityManager.persistAndFlush(WaitingEntry.waiting(nineAm, "Asha"));

    int claimed = slotRepository.hold(nineAm.getId(), 7L, NOW, NOW.plus(HOLD));

    assertThat(claimed).isZero();
  }

  @Test
  void offerClaimsTheSlotEvenWhenPatientsAreWaiting() {
    entityManager.persistAndFlush(WaitingEntry.waiting(nineAm, "Asha"));

    int claimed = slotRepository.offer(nineAm.getId(), 7L, NOW, NOW.plus(HOLD));

    Slot reloaded = slotRepository.findById(nineAm.getId()).orElseThrow();
    assertThat(claimed).isEqualTo(1);
    assertThat(reloaded.getStatus()).isEqualTo(SlotStatus.HELD);
    assertThat(reloaded.getActiveBookingId()).isEqualTo(7L);
  }

  @Test
  void offerIsRefusedWhileAnotherHoldIsStillRunning() {
    slotRepository.offer(nineAm.getId(), 7L, NOW, NOW.plus(HOLD));

    int claimed =
        slotRepository.offer(
            nineAm.getId(), 8L, NOW.plusSeconds(60), NOW.plus(HOLD).plusSeconds(60));

    assertThat(claimed).isZero();
  }

  @Test
  void findAvailableHidesSlotsThatHaveAWaitingList() {
    entityManager.persistAndFlush(WaitingEntry.waiting(nineAm, "Asha"));

    List<Slot> available = findForDay(DAY, NOW);

    assertThat(available).extracting(Slot::getId).containsExactly(nineThirty.getId());
  }

  @Test
  void isAvailableIsTrueOnlyForAClaimableSlotWithoutAWaitingList() {
    slotRepository.hold(nineThirty.getId(), 7L, NOW, NOW.plus(HOLD));
    entityManager.persistAndFlush(WaitingEntry.waiting(nineAm, "Asha"));
    Slot tenAm = entityManager.persist(slotAt(doctor, DAY.atTime(10, 0)));

    boolean free = slotRepository.isAvailable(tenAm.getId(), NOW);
    boolean withWaiter = slotRepository.isAvailable(nineAm.getId(), NOW);
    boolean held = slotRepository.isAvailable(nineThirty.getId(), NOW.plusSeconds(60));
    boolean heldButExpired = slotRepository.isAvailable(nineThirty.getId(), NOW.plus(HOLD));

    assertThat(free).isTrue();
    assertThat(withWaiter).isFalse();
    assertThat(held).isFalse();
    assertThat(heldButExpired).isTrue();
  }

  @Test
  void freeHoldReturnsOnlyTheHoldOfThatBookingToTheSlot() {
    slotRepository.hold(nineAm.getId(), 7L, NOW, NOW.plus(HOLD));

    int wrongBooking = slotRepository.freeHold(nineAm.getId(), 8L);
    int rightBooking = slotRepository.freeHold(nineAm.getId(), 7L);

    Slot reloaded = slotRepository.findById(nineAm.getId()).orElseThrow();
    assertThat(wrongBooking).isZero();
    assertThat(rightBooking).isEqualTo(1);
    assertThat(reloaded.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
    assertThat(reloaded.getActiveBookingId()).isNull();
  }

  @Test
  void freeHoldIsRefusedOnABookedSlot() {
    slotRepository.hold(nineAm.getId(), 7L, NOW, NOW.plus(HOLD));
    slotRepository.book(nineAm.getId(), 7L);

    int freed = slotRepository.freeHold(nineAm.getId(), 7L);

    assertThat(freed).isZero();
  }

  private List<Slot> findForDay(LocalDate day, Instant now) {
    return slotRepository.findAvailable(
        doctor.getId(), day.atStartOfDay(), day.plusDays(1).atStartOfDay(), now);
  }

  private Slot slotAt(Doctor owner, LocalDateTime start) {
    return Slot.available(owner, start, start.plusMinutes(30));
  }
}
