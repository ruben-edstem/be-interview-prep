package com.edstem.interviewprep.booking.repository;

import com.edstem.interviewprep.booking.entity.WaitingEntry;
import com.edstem.interviewprep.booking.entity.WaitingStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WaitingEntryRepository extends JpaRepository<WaitingEntry, Long> {

  boolean existsBySlotIdAndPatientName(Long slotId, String patientName);

  Optional<WaitingEntry> findFirstBySlotIdAndStatusOrderByIdAsc(Long slotId, WaitingStatus status);

  @Query(
      """
      select distinct w.slot.id from WaitingEntry w
      where w.status = WAITING
        and exists (
          select 1 from Slot s
          where s.id = w.slot.id
            and (s.status = AVAILABLE or (s.status = HELD and s.heldUntil <= :now)))
      """)
  List<Long> findSlotIdsReadyToOffer(@Param("now") Instant now);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update WaitingEntry w set w.status = OFFERED, w.offeredBookingId = :bookingId
      where w.id = :id and w.status = WAITING
      """)
  int offer(@Param("id") Long id, @Param("bookingId") Long bookingId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from WaitingEntry w where w.id = :id and w.status = WAITING")
  int deleteWaiting(@Param("id") Long id);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from WaitingEntry w where w.id = :id and w.status = OFFERED")
  int deleteOffered(@Param("id") Long id);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from WaitingEntry w where w.offeredBookingId = :bookingId")
  int deleteByOfferedBooking(@Param("bookingId") Long bookingId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      delete from WaitingEntry w
      where w.status = OFFERED
        and exists (
          select 1 from Booking b where b.id = w.offeredBookingId and b.expiresAt <= :now)
      """)
  int deleteExpiredOffers(@Param("now") Instant now);
}
