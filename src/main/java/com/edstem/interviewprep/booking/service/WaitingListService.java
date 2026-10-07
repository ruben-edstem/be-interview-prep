package com.edstem.interviewprep.booking.service;

import com.edstem.interviewprep.booking.config.BookingProperties;
import com.edstem.interviewprep.booking.dto.request.JoinWaitingListRequest;
import com.edstem.interviewprep.booking.dto.response.WaitingEntryResponse;
import com.edstem.interviewprep.booking.entity.Booking;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WaitingListService {

  private final WaitingEntryRepository waitingEntryRepository;
  private final SlotRepository slotRepository;
  private final BookingRepository bookingRepository;
  private final BookingMapper bookingMapper;
  private final BookingProperties properties;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  @Transactional
  public WaitingEntryResponse join(Long slotId, JoinWaitingListRequest request) {
    Slot slot =
        slotRepository.findById(slotId).orElseThrow(() -> new SlotNotFoundException(slotId));
    if (slotRepository.isAvailable(slotId, clock.instant())) {
      throw new SlotAvailableException(slotId);
    }
    if (waitingEntryRepository.existsBySlotIdAndPatientName(slotId, request.patientName())) {
      throw new AlreadyWaitingException(slotId);
    }

    WaitingEntry entry =
        waitingEntryRepository.save(WaitingEntry.waiting(slot, request.patientName()));
    return bookingMapper.toResponse(entry);
  }

  @Transactional
  public void leave(Long entryId) {
    if (waitingEntryRepository.deleteWaiting(entryId) == 1) {
      return;
    }
    WaitingEntry offered =
        waitingEntryRepository
            .findById(entryId)
            .orElseThrow(() -> new WaitingEntryNotFoundException(entryId));
    Long slotId = offered.getSlot().getId();
    Long bookingId = offered.getOfferedBookingId();

    bookingRepository.cancelHold(bookingId);
    slotRepository.freeHold(slotId, bookingId);
    waitingEntryRepository.deleteOffered(entryId);
    offerNext(slotId);
  }

  @Transactional
  public boolean offerNext(Long slotId) {
    Slot slot = slotRepository.findById(slotId).orElse(null);
    if (slot == null) {
      return false;
    }
    Long doctorId = slot.getDoctor().getId();
    LocalDateTime slotStart = slot.getStartTime();
    Instant now = clock.instant();
    Instant expiresAt = now.plus(properties.holdDuration());

    while (true) {
      Optional<WaitingEntry> next =
          waitingEntryRepository.findFirstBySlotIdAndStatusOrderByIdAsc(
              slotId, WaitingStatus.WAITING);
      if (next.isEmpty()) {
        return false;
      }
      WaitingEntry entry = next.get();
      Booking offer =
          bookingRepository.saveAndFlush(
              Booking.hold(
                  slotRepository.getReferenceById(slotId), entry.getPatientName(), expiresAt));

      if (slotRepository.offer(slotId, offer.getId(), now, expiresAt) == 0) {
        bookingRepository.deleteById(offer.getId());
        return false;
      }
      if (waitingEntryRepository.offer(entry.getId(), offer.getId()) == 1) {
        eventPublisher.publishEvent(
            SlotOfferedEvent.of(offer.getId(), slotId, doctorId, slotStart, expiresAt));
        return true;
      }
      slotRepository.freeHold(slotId, offer.getId());
      bookingRepository.deleteById(offer.getId());
    }
  }

  @Transactional
  public int removeExpiredOffers() {
    return waitingEntryRepository.deleteExpiredOffers(clock.instant());
  }

  @Transactional(readOnly = true)
  public List<Long> findSlotsReadyToOffer() {
    return waitingEntryRepository.findSlotIdsReadyToOffer(clock.instant());
  }
}
