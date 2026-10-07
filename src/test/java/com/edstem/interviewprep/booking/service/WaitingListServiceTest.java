package com.edstem.interviewprep.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edstem.interviewprep.booking.config.BookingProperties;
import com.edstem.interviewprep.booking.dto.request.JoinWaitingListRequest;
import com.edstem.interviewprep.booking.dto.response.WaitingEntryResponse;
import com.edstem.interviewprep.booking.entity.Booking;
import com.edstem.interviewprep.booking.entity.Doctor;
import com.edstem.interviewprep.booking.entity.Slot;
import com.edstem.interviewprep.booking.entity.WaitingEntry;
import com.edstem.interviewprep.booking.entity.WaitingStatus;
import com.edstem.interviewprep.booking.event.SlotOfferedEvent;
import com.edstem.interviewprep.booking.exception.AlreadyWaitingException;
import com.edstem.interviewprep.booking.exception.SlotAvailableException;
import com.edstem.interviewprep.booking.exception.SlotNotFoundException;
import com.edstem.interviewprep.booking.exception.WaitingEntryNotFoundException;
import com.edstem.interviewprep.booking.mapper.BookingMapper;
import com.edstem.interviewprep.booking.repository.BookingRepository;
import com.edstem.interviewprep.booking.repository.SlotRepository;
import com.edstem.interviewprep.booking.repository.WaitingEntryRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WaitingListServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-07T09:00:00Z");
  private static final Duration HOLD = Duration.ofMinutes(5);
  private static final LocalDateTime SLOT_START = LocalDateTime.of(2026, 10, 8, 9, 0);

  @Mock private WaitingEntryRepository waitingEntryRepository;
  @Mock private SlotRepository slotRepository;
  @Mock private BookingRepository bookingRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  private WaitingListService waitingListService;
  private Slot slot;

  @BeforeEach
  void setUp() {
    BookingProperties properties = new BookingProperties(HOLD, Duration.ofMinutes(30));
    waitingListService =
        new WaitingListService(
            waitingEntryRepository,
            slotRepository,
            bookingRepository,
            new BookingMapper(),
            properties,
            eventPublisher,
            Clock.fixed(NOW, ZoneOffset.UTC));
    Doctor doctor = Doctor.named("Dr Rao");
    ReflectionTestUtils.setField(doctor, "id", 3L);
    slot = Slot.available(doctor, SLOT_START, SLOT_START.plusMinutes(30));
    ReflectionTestUtils.setField(slot, "id", 10L);
  }

  @Test
  void joinAddsThePatientToTheWaitingListOfATakenSlot() {
    when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));
    when(slotRepository.isAvailable(10L, NOW)).thenReturn(false);
    when(waitingEntryRepository.existsBySlotIdAndPatientName(10L, "Asha")).thenReturn(false);
    when(waitingEntryRepository.save(any(WaitingEntry.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), 40L));

    WaitingEntryResponse response =
        waitingListService.join(10L, new JoinWaitingListRequest("Asha"));

    assertThat(response.id()).isEqualTo(40L);
    assertThat(response.slotId()).isEqualTo(10L);
    assertThat(response.patientName()).isEqualTo("Asha");
    assertThat(response.status()).isEqualTo(WaitingStatus.WAITING);
  }

  @Test
  void joinIsRefusedForAnAvailableSlot() {
    when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));
    when(slotRepository.isAvailable(10L, NOW)).thenReturn(true);

    assertThatThrownBy(() -> waitingListService.join(10L, new JoinWaitingListRequest("Asha")))
        .isInstanceOf(SlotAvailableException.class);

    verify(waitingEntryRepository, never()).save(any());
  }

  @Test
  void joinIsRefusedWhenThePatientIsAlreadyWaiting() {
    when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));
    when(slotRepository.isAvailable(10L, NOW)).thenReturn(false);
    when(waitingEntryRepository.existsBySlotIdAndPatientName(10L, "Asha")).thenReturn(true);

    assertThatThrownBy(() -> waitingListService.join(10L, new JoinWaitingListRequest("Asha")))
        .isInstanceOf(AlreadyWaitingException.class);

    verify(waitingEntryRepository, never()).save(any());
  }

  @Test
  void joinFailsForAnUnknownSlot() {
    when(slotRepository.findById(10L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> waitingListService.join(10L, new JoinWaitingListRequest("Asha")))
        .isInstanceOf(SlotNotFoundException.class);
  }

  @Test
  void leaveRemovesAWaitingEntry() {
    when(waitingEntryRepository.deleteWaiting(40L)).thenReturn(1);

    waitingListService.leave(40L);

    verify(waitingEntryRepository, never()).findById(anyLong());
    verify(bookingRepository, never()).cancelHold(anyLong());
  }

  @Test
  void leaveWithAnOpenOfferDeclinesItAndOffersTheSlotToTheNextPatient() {
    WaitingEntry offered = entry(40L, "Asha", WaitingStatus.OFFERED, 77L);
    when(waitingEntryRepository.deleteWaiting(40L)).thenReturn(0);
    when(waitingEntryRepository.findById(40L)).thenReturn(Optional.of(offered));
    when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));
    when(waitingEntryRepository.findFirstBySlotIdAndStatusOrderByIdAsc(10L, WaitingStatus.WAITING))
        .thenReturn(Optional.empty());

    waitingListService.leave(40L);

    InOrder order = inOrder(bookingRepository, slotRepository, waitingEntryRepository);
    order.verify(bookingRepository).cancelHold(77L);
    order.verify(slotRepository).freeHold(10L, 77L);
    order.verify(waitingEntryRepository).deleteOffered(40L);
    order
        .verify(waitingEntryRepository)
        .findFirstBySlotIdAndStatusOrderByIdAsc(10L, WaitingStatus.WAITING);
  }

  @Test
  void leaveFailsForAnUnknownEntry() {
    when(waitingEntryRepository.deleteWaiting(40L)).thenReturn(0);
    when(waitingEntryRepository.findById(40L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> waitingListService.leave(40L))
        .isInstanceOf(WaitingEntryNotFoundException.class);
  }

  @Test
  void offerNextHoldsTheSlotForTheLongestWaitingPatientAndPublishesTheOffer() {
    WaitingEntry first = entry(40L, "Asha", WaitingStatus.WAITING, null);
    stubSlotAndQueue(first);
    stubOfferBooking(77L);
    when(slotRepository.offer(10L, 77L, NOW, NOW.plus(HOLD))).thenReturn(1);
    when(waitingEntryRepository.offer(40L, 77L)).thenReturn(1);

    boolean offered = waitingListService.offerNext(10L);

    ArgumentCaptor<SlotOfferedEvent> event = ArgumentCaptor.forClass(SlotOfferedEvent.class);
    verify(eventPublisher).publishEvent(event.capture());
    assertThat(offered).isTrue();
    assertThat(event.getValue().getBookingId()).isEqualTo(77L);
    assertThat(event.getValue().getSlotId()).isEqualTo(10L);
    assertThat(event.getValue().getDoctorId()).isEqualTo(3L);
    assertThat(event.getValue().getSlotStart()).isEqualTo(SLOT_START);
    assertThat(event.getValue().getOfferExpiresAt()).isEqualTo(NOW.plus(HOLD));
  }

  @Test
  void offerNextDoesNothingWhenNobodyIsWaiting() {
    when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));
    when(waitingEntryRepository.findFirstBySlotIdAndStatusOrderByIdAsc(10L, WaitingStatus.WAITING))
        .thenReturn(Optional.empty());

    boolean offered = waitingListService.offerNext(10L);

    assertThat(offered).isFalse();
    verify(bookingRepository, never()).saveAndFlush(any());
    verify(eventPublisher, never()).publishEvent(any(Object.class));
  }

  @Test
  void offerNextDoesNothingForAnUnknownSlot() {
    when(slotRepository.findById(10L)).thenReturn(Optional.empty());

    boolean offered = waitingListService.offerNext(10L);

    assertThat(offered).isFalse();
    verify(waitingEntryRepository, never())
        .findFirstBySlotIdAndStatusOrderByIdAsc(anyLong(), any());
  }

  @Test
  void offerNextBacksOutWhenTheSlotCannotBeClaimed() {
    WaitingEntry first = entry(40L, "Asha", WaitingStatus.WAITING, null);
    stubSlotAndQueue(first);
    stubOfferBooking(77L);
    when(slotRepository.offer(10L, 77L, NOW, NOW.plus(HOLD))).thenReturn(0);

    boolean offered = waitingListService.offerNext(10L);

    assertThat(offered).isFalse();
    verify(bookingRepository).deleteById(77L);
    verify(waitingEntryRepository, never()).offer(anyLong(), anyLong());
    verify(eventPublisher, never()).publishEvent(any(Object.class));
  }

  @Test
  void offerNextMovesToTheNextPatientWhenTheFirstEntryWasJustRemoved() {
    WaitingEntry first = entry(40L, "Asha", WaitingStatus.WAITING, null);
    WaitingEntry second = entry(41L, "Ben", WaitingStatus.WAITING, null);
    when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));
    when(slotRepository.getReferenceById(10L)).thenReturn(slot);
    when(waitingEntryRepository.findFirstBySlotIdAndStatusOrderByIdAsc(10L, WaitingStatus.WAITING))
        .thenReturn(Optional.of(first), Optional.of(second));
    when(bookingRepository.saveAndFlush(any(Booking.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), 77L))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), 78L));
    when(slotRepository.offer(anyLong(), anyLong(), any(), any())).thenReturn(1);
    when(waitingEntryRepository.offer(40L, 77L)).thenReturn(0);
    when(waitingEntryRepository.offer(41L, 78L)).thenReturn(1);

    boolean offered = waitingListService.offerNext(10L);

    assertThat(offered).isTrue();
    verify(slotRepository).freeHold(10L, 77L);
    verify(bookingRepository).deleteById(77L);
    verify(eventPublisher).publishEvent(any(SlotOfferedEvent.class));
  }

  @Test
  void removeExpiredOffersDeletesOffersThatRanOut() {
    when(waitingEntryRepository.deleteExpiredOffers(NOW)).thenReturn(2);

    int removed = waitingListService.removeExpiredOffers();

    assertThat(removed).isEqualTo(2);
  }

  @Test
  void findSlotsReadyToOfferListsSlotsThatCanBeOffered() {
    when(waitingEntryRepository.findSlotIdsReadyToOffer(NOW)).thenReturn(List.of(10L, 11L));

    List<Long> slotIds = waitingListService.findSlotsReadyToOffer();

    assertThat(slotIds).containsExactly(10L, 11L);
  }

  private void stubSlotAndQueue(WaitingEntry first) {
    when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));
    when(slotRepository.getReferenceById(10L)).thenReturn(slot);
    when(waitingEntryRepository.findFirstBySlotIdAndStatusOrderByIdAsc(10L, WaitingStatus.WAITING))
        .thenReturn(Optional.of(first));
  }

  private void stubOfferBooking(Long bookingId) {
    when(bookingRepository.saveAndFlush(any(Booking.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), bookingId));
  }

  private WaitingEntry entry(Long id, String patient, WaitingStatus status, Long bookingId) {
    WaitingEntry entry = WaitingEntry.waiting(slot, patient);
    ReflectionTestUtils.setField(entry, "id", id);
    ReflectionTestUtils.setField(entry, "status", status);
    ReflectionTestUtils.setField(entry, "offeredBookingId", bookingId);
    return entry;
  }

  private <T> T withId(T target, Long id) {
    ReflectionTestUtils.setField(target, "id", id);
    return target;
  }
}
