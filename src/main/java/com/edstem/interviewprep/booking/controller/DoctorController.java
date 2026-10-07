package com.edstem.interviewprep.booking.controller;

import com.edstem.interviewprep.booking.dto.request.DoctorRequest;
import com.edstem.interviewprep.booking.dto.request.SlotGenerationRequest;
import com.edstem.interviewprep.booking.dto.response.DoctorResponse;
import com.edstem.interviewprep.booking.dto.response.SlotResponse;
import com.edstem.interviewprep.booking.service.DoctorService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
public class DoctorController {

  private final DoctorService doctorService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DoctorResponse create(@Valid @RequestBody DoctorRequest request) {
    return doctorService.create(request);
  }

  @PostMapping("/{doctorId}/slots")
  @ResponseStatus(HttpStatus.CREATED)
  public List<SlotResponse> generateSlots(
      @PathVariable Long doctorId, @Valid @RequestBody SlotGenerationRequest request) {
    return doctorService.generateSlots(doctorId, request);
  }

  @GetMapping("/{doctorId}/slots")
  public List<SlotResponse> availableSlots(
      @PathVariable Long doctorId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return doctorService.findAvailableSlots(doctorId, date);
  }
}
