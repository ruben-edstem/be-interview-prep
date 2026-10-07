package com.edstem.interviewprep.booking.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edstem.interviewprep.booking.entity.Booking;
import com.edstem.interviewprep.booking.entity.Doctor;
import com.edstem.interviewprep.booking.entity.Slot;
import com.edstem.interviewprep.booking.entity.WaitingEntry;
import com.edstem.interviewprep.booking.entity.WaitingStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class WaitingEntryRepositoryTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 8);
  private static final Instant NOW = Instant.parse("2026-10-07T09:00:00Z");
  private static final Duration HOLD = Duration.ofMinutes(5);

  @Autowired private WaitingEntryRepository waitingEntryRepository;
  @Autowired private SlotRepository slotRepository;
  @Autowired private TestEntityManager entityManager;

  private Doctor doctor;
  private Slot slot;

  @BeforeEach
  void setUp() {
    doctor = entityManager.persist(Doctor.named("Dr Rao"));
    slot = newSlot(DAY.atTime(9, 0));
  }

  @Test
  void findFirstReturnsTheLongestWaitingPatient() {
    persistEntry(slot, "first");
    persistEntry(slot, "second");
    persistEntry(slot, "third");

    Optional<WaitingEntry> next =
        waitingEntryRepository.findFirstBySlotIdAndStatusOrderByIdAsc(
            slot.getId(), WaitingStatus.WAITING);

    assertThat(next).isPresent();
    assertThat(next.get().getPatientName()).isEqualTo("first");
  }

  @Test
  void findFirstSkipsOfferedEntriesAndOtherSlots() {
    WaitingEntry offered = persistEntry(slot, "offered");
    persistEntry(slot, "waiting");
    Slot other = newSlot(DAY.atTime(10, 0));
    persistEntry(other, "elsewhere");
    waitingEntryRepository.offer(offered.getId(), 7L);

    Optional<WaitingEntry> next =
        waitingEntryRepository.findFirstBySlotIdAndStatusOrderByIdAsc(
            slot.getId(), WaitingStatus.WAITING);

    assertThat(next.orElseThrow().getPatientName()).isEqualTo("waiting");
  }

  @Test
  void findFirstReturnsNothingWhenNobodyWaits() {
    Optional<WaitingEntry> next =
        waitingEntryRepository.findFirstBySlotIdAndStatusOrderByIdAsc(
            slot.getId(), WaitingStatus.WAITING);

    assertThat(next).isEmpty();
  }

  @Test
  void existsFindsAnEntryOfThatPatientOnThatSlot() {
    persistEntry(slot, "Asha");

    assertThat(waitingEntryRepository.existsBySlotIdAndPatientName(slot.getId(), "Asha")).isTrue();
    assertThat(waitingEntryRepository.existsBySlotIdAndPatientName(slot.getId(), "Ben")).isFalse();
  }

  @Test
  void aPatientCannotWaitTwiceForTheSameSlot() {
    persistEntry(slot, "Asha");

    assertThatThrownBy(
            () -> waitingEntryRepository.saveAndFlush(WaitingEntry.waiting(slot, "Asha")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void offerMarksAWaitingEntryOnlyOnce() {
    WaitingEntry entry = persistEntry(slot, "Asha");

    int first = waitingEntryRepository.offer(entry.getId(), 7L);
    int second = waitingEntryRepository.offer(entry.getId(), 8L);

    WaitingEntry reloaded = waitingEntryRepository.findById(entry.getId()).orElseThrow();
    assertThat(first).isEqualTo(1);
    assertThat(second).isZero();
    assertThat(reloaded.getStatus()).isEqualTo(WaitingStatus.OFFERED);
    assertThat(reloaded.getOfferedBookingId()).isEqualTo(7L);
  }

  @Test
  void deleteWaitingRemovesOnlyWaitingEntries() {
    WaitingEntry waiting = persistEntry(slot, "waiting");
    WaitingEntry offered = persistEntry(slot, "offered");
    waitingEntryRepository.offer(offered.getId(), 7L);

    int removedWaiting = waitingEntryRepository.deleteWaiting(waiting.getId());
    int removedOffered = waitingEntryRepository.deleteWaiting(offered.getId());

    assertThat(removedWaiting).isEqualTo(1);
    assertThat(removedOffered).isZero();
    assertThat(waitingEntryRepository.findById(offered.getId())).isPresent();
  }

  @Test
  void deleteOfferedRemovesOnlyOfferedEntries() {
    WaitingEntry waiting = persistEntry(slot, "waiting");
    WaitingEntry offered = persistEntry(slot, "offered");
    waitingEntryRepository.offer(offered.getId(), 7L);

    int removedWaiting = waitingEntryRepository.deleteOffered(waiting.getId());
    int removedOffered = waitingEntryRepository.deleteOffered(offered.getId());

    assertThat(removedWaiting).isZero();
    assertThat(removedOffered).isEqualTo(1);
    assertThat(waitingEntryRepository.findById(waiting.getId())).isPresent();
  }

  @Test
  void deleteByOfferedBookingRemovesTheEntryOfThatOffer() {
    WaitingEntry offered = persistEntry(slot, "offered");
    WaitingEntry other = persistEntry(slot, "other");
    waitingEntryRepository.offer(offered.getId(), 7L);
    waitingEntryRepository.offer(other.getId(), 8L);

    int removed = waitingEntryRepository.deleteByOfferedBooking(7L);

    assertThat(removed).isEqualTo(1);
    assertThat(waitingEntryRepository.findById(offered.getId())).isEmpty();
    assertThat(waitingEntryRepository.findById(other.getId())).isPresent();
  }

  @Test
  void deleteExpiredOffersKeepsLiveOffersAndWaitingEntries() {
    WaitingEntry expired = persistEntry(slot, "expired");
    WaitingEntry live = persistEntry(slot, "live");
    WaitingEntry waiting = persistEntry(slot, "waiting");
    Booking expiredOffer = persistBooking(slot, "expired", NOW.plus(HOLD));
    Booking liveOffer = persistBooking(slot, "live", NOW.plus(HOLD).plusSeconds(60));
    waitingEntryRepository.offer(expired.getId(), expiredOffer.getId());
    waitingEntryRepository.offer(live.getId(), liveOffer.getId());

    int removed = waitingEntryRepository.deleteExpiredOffers(NOW.plus(HOLD));

    assertThat(removed).isEqualTo(1);
    assertThat(waitingEntryRepository.findById(expired.getId())).isEmpty();
    assertThat(waitingEntryRepository.findById(live.getId())).isPresent();
    assertThat(waitingEntryRepository.findById(waiting.getId())).isPresent();
  }

  @Test
  void findSlotIdsReadyToOfferIncludesAvailableSlotsAndExpiredHolds() {
    Slot availableSlot = newSlot(DAY.atTime(10, 0));
    Slot expiredHoldSlot = newSlot(DAY.atTime(10, 30));
    slotRepository.offer(expiredHoldSlot.getId(), 7L, NOW, NOW.plus(HOLD));
    persistEntry(availableSlot, "one");
    persistEntry(availableSlot, "two");
    persistEntry(expiredHoldSlot, "three");

    List<Long> ready = waitingEntryRepository.findSlotIdsReadyToOffer(NOW.plus(HOLD));

    assertThat(ready).containsExactlyInAnyOrder(availableSlot.getId(), expiredHoldSlot.getId());
  }

  @Test
  void findSlotIdsReadyToOfferSkipsHeldAndBookedSlots() {
    Slot heldSlot = newSlot(DAY.atTime(10, 0));
    Slot bookedSlot = newSlot(DAY.atTime(10, 30));
    slotRepository.offer(heldSlot.getId(), 7L, NOW, NOW.plus(HOLD));
    slotRepository.offer(bookedSlot.getId(), 8L, NOW, NOW.plus(HOLD));
    slotRepository.book(bookedSlot.getId(), 8L);
    persistEntry(heldSlot, "held");
    persistEntry(bookedSlot, "booked");

    List<Long> ready = waitingEntryRepository.findSlotIdsReadyToOffer(NOW.plusSeconds(60));

    assertThat(ready).isEmpty();
  }

  @Test
  void findSlotIdsReadyToOfferIgnoresSlotsWithoutAWaitingPatient() {
    WaitingEntry offered = persistEntry(slot, "offered");
    waitingEntryRepository.offer(offered.getId(), 7L);

    List<Long> ready = waitingEntryRepository.findSlotIdsReadyToOffer(NOW);

    assertThat(ready).isEmpty();
  }

  private Slot newSlot(LocalDateTime start) {
    return entityManager.persist(Slot.available(doctor, start, start.plusMinutes(30)));
  }

  private WaitingEntry persistEntry(Slot forSlot, String patient) {
    WaitingEntry entry = entityManager.persistAndFlush(WaitingEntry.waiting(forSlot, patient));
    entityManager.clear();
    return entry;
  }

  private Booking persistBooking(Slot forSlot, String patient, Instant expiresAt) {
    Booking booking = entityManager.persistAndFlush(Booking.hold(forSlot, patient, expiresAt));
    entityManager.clear();
    return booking;
  }
}
