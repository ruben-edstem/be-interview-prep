package com.edstem.interviewprep.booking.service;

import com.edstem.interviewprep.booking.config.BookingProperties;
import com.edstem.interviewprep.booking.dto.request.HoldRequest;
import com.edstem.interviewprep.booking.dto.response.BookingResponse;
import com.edstem.interviewprep.booking.entity.Booking;
import com.edstem.interviewprep.booking.entity.BookingStatus;
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
import com.edstem.interviewprep.booking.repository.WaitingEntryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingService {

  private final BookingRepository bookingRepository;
  private final SlotRepository slotRepository;
  private final BookingMapper bookingMapper;
  private final BookingProperties properties;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;
  private final WaitingEntryRepository waitingEntryRepository;
  private final WaitingListService waitingListService;

  @Transactional
  public BookingResponse hold(Long slotId, HoldRequest request) {
    Slot slot =
        slotRepository.findById(slotId).orElseThrow(() -> new SlotNotFoundException(slotId));
    Instant now = clock.instant();
    Instant heldUntil = now.plus(properties.holdDuration());
    Booking booking =
        bookingRepository.saveAndFlush(Booking.hold(slot, request.patientName(), heldUntil));

    if (slotRepository.hold(slotId, booking.getId(), now, heldUntil) == 0) {
      throw new SlotUnavailableException(slotId);
    }
    return bookingMapper.toResponse(booking);
  }

  @Transactional
  public BookingResponse confirm(Long bookingId) {
    Booking booking = findBooking(bookingId);
    Slot slot = booking.getSlot();
    Long slotId = slot.getId();
    Long doctorId = slot.getDoctor().getId();
    LocalDateTime slotStart = slot.getStartTime();
    BookingStatus statusBefore = booking.getStatus();

    if (bookingRepository.confirm(bookingId, clock.instant()) == 0) {
      throw statusBefore == BookingStatus.HELD
          ? new HoldExpiredException(bookingId)
          : new InvalidBookingStateException(bookingId, "confirmed");
    }
    if (slotRepository.book(slotId, bookingId) == 0) {
      throw new HoldExpiredException(bookingId);
    }
    waitingEntryRepository.deleteByOfferedBooking(bookingId);

    eventPublisher.publishEvent(BookingConfirmedEvent.of(bookingId, slotId, doctorId, slotStart));
    return bookingMapper.toResponse(findBooking(bookingId));
  }

  @Transactional
  public void cancel(Long bookingId) {
    Booking booking = findBooking(bookingId);
    Long slotId = booking.getSlot().getId();

    if (bookingRepository.cancel(bookingId) == 0
        || slotRepository.release(slotId, bookingId) == 0) {
      throw new InvalidBookingStateException(bookingId, "cancelled");
    }
    waitingListService.offerNext(slotId);
  }

  private Booking findBooking(Long bookingId) {
    return bookingRepository
        .findById(bookingId)
        .orElseThrow(() -> new BookingNotFoundException(bookingId));
  }
}
