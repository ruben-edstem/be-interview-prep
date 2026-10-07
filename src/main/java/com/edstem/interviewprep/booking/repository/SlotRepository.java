package com.edstem.interviewprep.booking.repository;

import com.edstem.interviewprep.booking.entity.Slot;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlotRepository extends JpaRepository<Slot, Long> {

  boolean existsByDoctorIdAndStartTimeBetween(
      Long doctorId, LocalDateTime firstStart, LocalDateTime lastStart);

  @Query(
      """
      select s from Slot s
      where s.doctor.id = :doctorId
        and s.startTime >= :from and s.startTime < :to
        and (s.status = AVAILABLE or (s.status = HELD and s.heldUntil <= :now))
      order by s.startTime
      """)
  List<Slot> findAvailable(
      @Param("doctorId") Long doctorId,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("now") Instant now);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update Slot s
      set s.status = HELD, s.activeBookingId = :bookingId, s.heldUntil = :heldUntil
      where s.id = :slotId
        and (s.status = AVAILABLE or (s.status = HELD and s.heldUntil <= :now))
      """)
  int hold(
      @Param("slotId") Long slotId,
      @Param("bookingId") Long bookingId,
      @Param("now") Instant now,
      @Param("heldUntil") Instant heldUntil);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update Slot s
      set s.status = BOOKED, s.heldUntil = null
      where s.id = :slotId and s.status = HELD and s.activeBookingId = :bookingId
      """)
  int book(@Param("slotId") Long slotId, @Param("bookingId") Long bookingId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update Slot s
      set s.status = AVAILABLE, s.activeBookingId = null, s.heldUntil = null
      where s.id = :slotId and s.status = BOOKED and s.activeBookingId = :bookingId
      """)
  int release(@Param("slotId") Long slotId, @Param("bookingId") Long bookingId);
}
