package com.edstem.interviewprep.booking.mapper;

import com.edstem.interviewprep.booking.dto.response.BookingResponse;
import com.edstem.interviewprep.booking.dto.response.DoctorResponse;
import com.edstem.interviewprep.booking.dto.response.SlotResponse;
import com.edstem.interviewprep.booking.dto.response.WaitingEntryResponse;
import com.edstem.interviewprep.booking.entity.Booking;
import com.edstem.interviewprep.booking.entity.Doctor;
import com.edstem.interviewprep.booking.entity.Slot;
import com.edstem.interviewprep.booking.entity.WaitingEntry;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

  public DoctorResponse toResponse(Doctor doctor) {
    return new DoctorResponse(doctor.getId(), doctor.getName());
  }

  public SlotResponse toResponse(Slot slot) {
    return new SlotResponse(
        slot.getId(), slot.getDoctor().getId(), slot.getStartTime(), slot.getEndTime());
  }

  public BookingResponse toResponse(Booking booking) {
    return new BookingResponse(
        booking.getId(),
        booking.getSlot().getId(),
        booking.getPatientName(),
        booking.getStatus(),
        booking.getExpiresAt());
  }

  public WaitingEntryResponse toResponse(WaitingEntry entry) {
    return new WaitingEntryResponse(
        entry.getId(), entry.getSlot().getId(), entry.getPatientName(), entry.getStatus());
  }
}
