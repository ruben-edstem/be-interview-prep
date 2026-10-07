package com.edstem.interviewprep.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edstem.interviewprep.booking.config.BookingProperties;
import com.edstem.interviewprep.booking.dto.request.HoldRequest;
import com.edstem.interviewprep.booking.dto.response.BookingResponse;
import com.edstem.interviewprep.booking.entity.Booking;
import com.edstem.interviewprep.booking.entity.BookingStatus;
import com.edstem.interviewprep.booking.entity.Doctor;
import com.edstem.interviewprep.booking.entity.Slot;
import com.edstem.interviewprep.booking.event.BookingConfirmedEvent;
import com.edstem.interviewprep.booking.exception.BookingNotFoundException;
import com.edstem.interviewprep.booking.exception.HoldExpiredException;
import com.edstem.interviewprep.booking.exception.InvalidBookingStateException;
import com.edstem.interviewprep.booking.exception.SlotNotFoundException;
import com.edstem.interviewprep.booking.exception.SlotUnavailableException;
import com.edstem.interviewprep.booking.mapper.BookingMapper;
import com.edstem.interviewprep.booking.repository.BookingRepository;
import com.edstem.interviewprep.booking.repository.SlotRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-07T09:00:00Z");
  private static final Duration HOLD = Duration.ofMinutes(5);
  private static final LocalDateTime SLOT_START = LocalDateTime.of(2026, 10, 8, 9, 0);

  @Mock private BookingRepository bookingRepository;
  @Mock private SlotRepository slotRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  private BookingService bookingService;
  private Slot slot;

  @BeforeEach
  void setUp() {
    BookingProperties properties = new BookingProperties(HOLD, Duration.ofMinutes(30));
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    bookingService =
        new BookingService(
            bookingRepository,
            slotRepository,
            new BookingMapper(),
            properties,
            eventPublisher,
            clock);
    Doctor doctor = Doctor.named("Dr Rao");
    ReflectionTestUtils.setField(doctor, "id", 3L);
    slot = Slot.available(doctor, SLOT_START, SLOT_START.plusMinutes(30));
    ReflectionTestUtils.setField(slot, "id", 10L);
  }

  @Test
  void holdCreatesAHoldThatExpiresAfterTheConfiguredDuration() {
    when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));
    when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(this::withId);
    when(slotRepository.hold(10L, 99L, NOW, NOW.plus(HOLD))).thenReturn(1);

    BookingResponse response = bookingService.hold(10L, new HoldRequest("Asha"));

    assertThat(response.id()).isEqualTo(99L);
    assertThat(response.slotId()).isEqualTo(10L);
    assertThat(response.patientName()).isEqualTo("Asha");
    assertThat(response.status()).isEqualTo(BookingStatus.HELD);
    assertThat(response.expiresAt()).isEqualTo(NOW.plus(HOLD));
  }

  @Test
  void holdFailsWhenTheSlotCouldNotBeClaimed() {
    when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));
    when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(this::withId);
    when(slotRepository.hold(10L, 99L, NOW, NOW.plus(HOLD))).thenReturn(0);

    assertThatThrownBy(() -> bookingService.hold(10L, new HoldRequest("Asha")))
        .isInstanceOf(SlotUnavailableException.class);
  }

  @Test
  void holdFailsForAnUnknownSlot() {
    when(slotRepository.findById(10L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> bookingService.hold(10L, new HoldRequest("Asha")))
        .isInstanceOf(SlotNotFoundException.class);

    verify(bookingRepository, never()).saveAndFlush(any());
  }

  @Test
  void confirmBooksTheSlotAndPublishesTheEvent() {
    Booking held = booking(BookingStatus.HELD);
    Booking confirmed = booking(BookingStatus.CONFIRMED);
    when(bookingRepository.findById(99L)).thenReturn(Optional.of(held), Optional.of(confirmed));
    when(bookingRepository.confirm(99L, NOW)).thenReturn(1);
    when(slotRepository.book(10L, 99L)).thenReturn(1);

    BookingResponse response = bookingService.confirm(99L);

    ArgumentCaptor<BookingConfirmedEvent> event =
        ArgumentCaptor.forClass(BookingConfirmedEvent.class);
    verify(eventPublisher).publishEvent(event.capture());
    assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
    assertThat(event.getValue().getBookingId()).isEqualTo(99L);
    assertThat(event.getValue().getSlotId()).isEqualTo(10L);
    assertThat(event.getValue().getDoctorId()).isEqualTo(3L);
    assertThat(event.getValue().getSlotStart()).isEqualTo(SLOT_START);
  }

  @Test
  void confirmOfAnExpiredHoldFailsWithoutPublishing() {
    when(bookingRepository.findById(99L)).thenReturn(Optional.of(booking(BookingStatus.HELD)));
    when(bookingRepository.confirm(99L, NOW)).thenReturn(0);

    assertThatThrownBy(() -> bookingService.confirm(99L)).isInstanceOf(HoldExpiredException.class);

    verify(slotRepository, never()).book(anyLong(), anyLong());
    verify(eventPublisher, never()).publishEvent(any(Object.class));
  }

  @Test
  void confirmOfAnAlreadyConfirmedBookingFailsWithoutPublishing() {
    when(bookingRepository.findById(99L)).thenReturn(Optional.of(booking(BookingStatus.CONFIRMED)));
    when(bookingRepository.confirm(99L, NOW)).thenReturn(0);

    assertThatThrownBy(() -> bookingService.confirm(99L))
        .isInstanceOf(InvalidBookingStateException.class);

    verify(eventPublisher, never()).publishEvent(any(Object.class));
  }

  @Test
  void confirmFailsWhenTheSlotWasTakenOverInTheMeantime() {
    when(bookingRepository.findById(99L)).thenReturn(Optional.of(booking(BookingStatus.HELD)));
    when(bookingRepository.confirm(99L, NOW)).thenReturn(1);
    when(slotRepository.book(10L, 99L)).thenReturn(0);

    assertThatThrownBy(() -> bookingService.confirm(99L)).isInstanceOf(HoldExpiredException.class);

    verify(eventPublisher, never()).publishEvent(any(Object.class));
  }

  @Test
  void confirmFailsForAnUnknownBooking() {
    when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> bookingService.confirm(99L))
        .isInstanceOf(BookingNotFoundException.class);
  }

  @Test
  void cancelFreesTheSlot() {
    when(bookingRepository.findById(99L)).thenReturn(Optional.of(booking(BookingStatus.CONFIRMED)));
    when(bookingRepository.cancel(99L)).thenReturn(1);
    when(slotRepository.release(10L, 99L)).thenReturn(1);

    bookingService.cancel(99L);

    verify(slotRepository).release(10L, 99L);
  }

  @Test
  void cancelOfAnUnconfirmedBookingFailsAndLeavesTheSlotAlone() {
    when(bookingRepository.findById(99L)).thenReturn(Optional.of(booking(BookingStatus.HELD)));
    when(bookingRepository.cancel(99L)).thenReturn(0);

    assertThatThrownBy(() -> bookingService.cancel(99L))
        .isInstanceOf(InvalidBookingStateException.class);

    verify(slotRepository, never()).release(anyLong(), anyLong());
  }

  @Test
  void cancelFailsWhenTheSlotIsNotBookedByThatBooking() {
    when(bookingRepository.findById(99L)).thenReturn(Optional.of(booking(BookingStatus.CONFIRMED)));
    when(bookingRepository.cancel(99L)).thenReturn(1);
    when(slotRepository.release(10L, 99L)).thenReturn(0);

    assertThatThrownBy(() -> bookingService.cancel(99L))
        .isInstanceOf(InvalidBookingStateException.class);
  }

  @Test
  void cancelFailsForAnUnknownBooking() {
    when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> bookingService.cancel(99L))
        .isInstanceOf(BookingNotFoundException.class);
  }

  private Booking booking(BookingStatus status) {
    Booking booking = Booking.hold(slot, "Asha", NOW.plus(HOLD));
    ReflectionTestUtils.setField(booking, "id", 99L);
    ReflectionTestUtils.setField(booking, "status", status);
    return booking;
  }

  private Booking withId(InvocationOnMock invocation) {
    Booking booking = invocation.getArgument(0);
    ReflectionTestUtils.setField(booking, "id", 99L);
    return booking;
  }
}
