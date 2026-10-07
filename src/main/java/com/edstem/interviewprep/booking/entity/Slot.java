package com.edstem.interviewprep.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "slots",
    uniqueConstraints = @UniqueConstraint(columnNames = {"doctor_id", "start_time"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Slot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "doctor_id")
  private Doctor doctor;

  @Column(nullable = false)
  private LocalDateTime startTime;

  @Column(nullable = false)
  private LocalDateTime endTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SlotStatus status;

  private Instant heldUntil;

  private Long activeBookingId;

  public static Slot available(Doctor doctor, LocalDateTime startTime, LocalDateTime endTime) {
    Slot slot = new Slot();
    slot.doctor = doctor;
    slot.startTime = startTime;
    slot.endTime = endTime;
    slot.status = SlotStatus.AVAILABLE;
    return slot;
  }
}
