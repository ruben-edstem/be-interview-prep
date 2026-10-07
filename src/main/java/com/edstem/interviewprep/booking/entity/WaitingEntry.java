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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "waiting_entries",
    uniqueConstraints = @UniqueConstraint(columnNames = {"slot_id", "patient_name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WaitingEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "slot_id")
  private Slot slot;

  @Column(nullable = false)
  private String patientName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private WaitingStatus status;

  private Long offeredBookingId;

  @CreationTimestamp private Instant createdAt;

  public static WaitingEntry waiting(Slot slot, String patientName) {
    WaitingEntry entry = new WaitingEntry();
    entry.slot = slot;
    entry.patientName = patientName;
    entry.status = WaitingStatus.WAITING;
    return entry;
  }
}
