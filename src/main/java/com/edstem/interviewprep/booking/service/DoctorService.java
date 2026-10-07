package com.edstem.interviewprep.booking.service;

import com.edstem.interviewprep.booking.config.BookingProperties;
import com.edstem.interviewprep.booking.dto.request.DoctorRequest;
import com.edstem.interviewprep.booking.dto.request.SlotGenerationRequest;
import com.edstem.interviewprep.booking.dto.response.DoctorResponse;
import com.edstem.interviewprep.booking.dto.response.SlotResponse;
import com.edstem.interviewprep.booking.entity.Doctor;
import com.edstem.interviewprep.booking.entity.Slot;
import com.edstem.interviewprep.booking.exception.DoctorNotFoundException;
import com.edstem.interviewprep.booking.exception.InvalidSlotRangeException;
import com.edstem.interviewprep.booking.exception.SlotAlreadyExistsException;
import com.edstem.interviewprep.booking.mapper.BookingMapper;
import com.edstem.interviewprep.booking.repository.DoctorRepository;
import com.edstem.interviewprep.booking.repository.SlotRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DoctorService {

  private final DoctorRepository doctorRepository;
  private final SlotRepository slotRepository;
  private final BookingMapper bookingMapper;
  private final BookingProperties properties;
  private final Clock clock;

  @Transactional
  public DoctorResponse create(DoctorRequest request) {
    Doctor doctor = doctorRepository.save(Doctor.named(request.name()));
    return bookingMapper.toResponse(doctor);
  }

  @Transactional
  public List<SlotResponse> generateSlots(Long doctorId, SlotGenerationRequest request) {
    Doctor doctor = findDoctor(doctorId);
    Duration slotDuration = properties.slotDuration();
    LocalDateTime start = request.date().atTime(request.startTime());
    LocalDateTime end = request.date().atTime(request.endTime());
    Duration window = Duration.between(start, end);
    if (window.isNegative()
        || window.isZero()
        || window.toMinutes() % slotDuration.toMinutes() != 0) {
      throw new InvalidSlotRangeException(slotDuration);
    }
    if (slotRepository.existsByDoctorIdAndStartTimeBetween(
        doctorId, start, end.minus(slotDuration))) {
      throw new SlotAlreadyExistsException(doctorId);
    }

    List<Slot> slots = new ArrayList<>();
    for (LocalDateTime slotStart = start;
        slotStart.isBefore(end);
        slotStart = slotStart.plus(slotDuration)) {
      slots.add(Slot.available(doctor, slotStart, slotStart.plus(slotDuration)));
    }
    return slotRepository.saveAll(slots).stream().map(bookingMapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<SlotResponse> findAvailableSlots(Long doctorId, LocalDate date) {
    findDoctor(doctorId);
    return slotRepository
        .findAvailable(
            doctorId, date.atStartOfDay(), date.plusDays(1).atStartOfDay(), clock.instant())
        .stream()
        .map(bookingMapper::toResponse)
        .toList();
  }

  private Doctor findDoctor(Long doctorId) {
    return doctorRepository
        .findById(doctorId)
        .orElseThrow(() -> new DoctorNotFoundException(doctorId));
  }
}
