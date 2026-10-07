package com.edstem.interviewprep.booking.repository;

import com.edstem.interviewprep.booking.entity.Booking;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update Booking b set b.status = CONFIRMED
      where b.id = :id and b.status = HELD and b.expiresAt > :now
      """)
  int confirm(@Param("id") Long id, @Param("now") Instant now);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("update Booking b set b.status = CANCELLED where b.id = :id and b.status = CONFIRMED")
  int cancel(@Param("id") Long id);
}
