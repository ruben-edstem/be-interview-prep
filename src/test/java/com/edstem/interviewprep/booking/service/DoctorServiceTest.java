package com.edstem.interviewprep.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-07T09:00:00Z");
  private static final LocalDate DAY = LocalDate.of(2026, 10, 8);

  @Mock private DoctorRepository doctorRepository;
  @Mock private SlotRepository slotRepository;

  private DoctorService doctorService;
  private Doctor doctor;

  @BeforeEach
  void setUp() {
    BookingProperties properties =
        new BookingProperties(Duration.ofMinutes(5), Duration.ofMinutes(30));
    doctorService =
        new DoctorService(
            doctorRepository,
            slotRepository,
            new BookingMapper(),
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC));
    doctor = Doctor.named("Dr Rao");
    ReflectionTestUtils.setField(doctor, "id", 3L);
  }

  @Test
  void createSavesTheDoctor() {
    when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);

    DoctorResponse response = doctorService.create(new DoctorRequest("Dr Rao"));

    assertThat(response.id()).isEqualTo(3L);
    assertThat(response.name()).isEqualTo("Dr Rao");
  }

  @Test
  void generateSlotsSplitsTheWindowIntoThirtyMinuteSlots() {
    when(doctorRepository.findById(3L)).thenReturn(Optional.of(doctor));
    when(slotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

    List<SlotResponse> slots =
        doctorService.generateSlots(
            3L, new SlotGenerationRequest(DAY, LocalTime.of(9, 0), LocalTime.of(11, 0)));

    assertThat(slots).hasSize(4);
    assertThat(slots)
        .extracting(SlotResponse::startTime)
        .containsExactly(
            DAY.atTime(9, 0), DAY.atTime(9, 30), DAY.atTime(10, 0), DAY.atTime(10, 30));
    assertThat(slots.get(3).endTime()).isEqualTo(DAY.atTime(11, 0));
    assertThat(slots).allSatisfy(slot -> assertThat(slot.doctorId()).isEqualTo(3L));
  }

  @Test
  void generateSlotsRejectsAWindowThatEndsBeforeItStarts() {
    when(doctorRepository.findById(3L)).thenReturn(Optional.of(doctor));

    assertThatThrownBy(
            () ->
                doctorService.generateSlots(
                    3L, new SlotGenerationRequest(DAY, LocalTime.of(11, 0), LocalTime.of(9, 0))))
        .isInstanceOf(InvalidSlotRangeException.class);
  }

  @Test
  void generateSlotsRejectsAnEmptyWindow() {
    when(doctorRepository.findById(3L)).thenReturn(Optional.of(doctor));

    assertThatThrownBy(
            () ->
                doctorService.generateSlots(
                    3L, new SlotGenerationRequest(DAY, LocalTime.of(9, 0), LocalTime.of(9, 0))))
        .isInstanceOf(InvalidSlotRangeException.class);
  }

  @Test
  void generateSlotsRejectsAWindowThatIsNotWholeSlots() {
    when(doctorRepository.findById(3L)).thenReturn(Optional.of(doctor));

    assertThatThrownBy(
            () ->
                doctorService.generateSlots(
                    3L, new SlotGenerationRequest(DAY, LocalTime.of(9, 0), LocalTime.of(9, 45))))
        .isInstanceOf(InvalidSlotRangeException.class);

    verify(slotRepository, never()).saveAll(anyList());
  }

  @Test
  void generateSlotsRejectsAWindowThatOverlapsExistingSlots() {
    when(doctorRepository.findById(3L)).thenReturn(Optional.of(doctor));
    when(slotRepository.existsByDoctorIdAndStartTimeBetween(
            3L, DAY.atTime(9, 0), DAY.atTime(10, 30)))
        .thenReturn(true);

    assertThatThrownBy(
            () ->
                doctorService.generateSlots(
                    3L, new SlotGenerationRequest(DAY, LocalTime.of(9, 0), LocalTime.of(11, 0))))
        .isInstanceOf(SlotAlreadyExistsException.class);

    verify(slotRepository, never()).saveAll(anyList());
  }

  @Test
  void generateSlotsFailsForAnUnknownDoctor() {
    when(doctorRepository.findById(3L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                doctorService.generateSlots(
                    3L, new SlotGenerationRequest(DAY, LocalTime.of(9, 0), LocalTime.of(10, 0))))
        .isInstanceOf(DoctorNotFoundException.class);
  }

  @Test
  void findAvailableSlotsQueriesTheWholeDayAtTheCurrentTime() {
    LocalDateTime nineAm = DAY.atTime(9, 0);
    Slot slot = Slot.available(doctor, nineAm, nineAm.plusMinutes(30));
    ReflectionTestUtils.setField(slot, "id", 10L);
    when(doctorRepository.findById(3L)).thenReturn(Optional.of(doctor));
    when(slotRepository.findAvailable(3L, DAY.atStartOfDay(), DAY.plusDays(1).atStartOfDay(), NOW))
        .thenReturn(List.of(slot));

    List<SlotResponse> slots = doctorService.findAvailableSlots(3L, DAY);

    assertThat(slots).extracting(SlotResponse::id).containsExactly(10L);
  }

  @Test
  void findAvailableSlotsFailsForAnUnknownDoctor() {
    when(doctorRepository.findById(3L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> doctorService.findAvailableSlots(3L, DAY))
        .isInstanceOf(DoctorNotFoundException.class);
  }
}
