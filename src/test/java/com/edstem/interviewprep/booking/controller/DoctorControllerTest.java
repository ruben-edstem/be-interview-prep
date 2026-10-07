package com.edstem.interviewprep.booking.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.booking.dto.request.DoctorRequest;
import com.edstem.interviewprep.booking.dto.request.SlotGenerationRequest;
import com.edstem.interviewprep.booking.dto.response.DoctorResponse;
import com.edstem.interviewprep.booking.dto.response.SlotResponse;
import com.edstem.interviewprep.booking.exception.DoctorNotFoundException;
import com.edstem.interviewprep.booking.exception.InvalidSlotRangeException;
import com.edstem.interviewprep.booking.service.DoctorService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DoctorController.class)
class DoctorControllerTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 8);

  @Autowired private MockMvc mockMvc;
  @MockitoBean private DoctorService doctorService;

  @Test
  void createReturnsTheNewDoctor() throws Exception {
    when(doctorService.create(new DoctorRequest("Dr Rao")))
        .thenReturn(new DoctorResponse(3L, "Dr Rao"));

    mockMvc
        .perform(
            post("/doctors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Dr Rao\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(3))
        .andExpect(jsonPath("$.name").value("Dr Rao"));
  }

  @Test
  void createRejectsABlankName() throws Exception {
    mockMvc
        .perform(post("/doctors").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors.name").value("must not be blank"));

    verifyNoInteractions(doctorService);
  }

  @Test
  void generateSlotsReturnsTheCreatedSlots() throws Exception {
    SlotGenerationRequest request =
        new SlotGenerationRequest(DAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
    when(doctorService.generateSlots(3L, request))
        .thenReturn(
            List.of(
                new SlotResponse(10L, 3L, DAY.atTime(9, 0), DAY.atTime(9, 30)),
                new SlotResponse(11L, 3L, DAY.atTime(9, 30), DAY.atTime(10, 0))));

    mockMvc
        .perform(
            post("/doctors/3/slots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-10-08\",\"startTime\":\"09:00\",\"endTime\":\"10:00\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].startTime").value("2026-10-08T09:00:00"))
        .andExpect(jsonPath("$[1].id").value(11));
  }

  @Test
  void generateSlotsRejectsAMissingField() throws Exception {
    mockMvc
        .perform(
            post("/doctors/3/slots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-10-08\",\"startTime\":\"09:00\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.endTime").value("must not be null"));

    verifyNoInteractions(doctorService);
  }

  @Test
  void generateSlotsReturnsBadRequestForAnInvalidRange() throws Exception {
    SlotGenerationRequest request =
        new SlotGenerationRequest(DAY, LocalTime.of(9, 0), LocalTime.of(9, 45));
    when(doctorService.generateSlots(3L, request))
        .thenThrow(new InvalidSlotRangeException(Duration.ofMinutes(30)));

    mockMvc
        .perform(
            post("/doctors/3/slots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-10-08\",\"startTime\":\"09:00\",\"endTime\":\"09:45\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_SLOT_RANGE"));
  }

  @Test
  void availableSlotsReturnsTheDaysFreeSlots() throws Exception {
    when(doctorService.findAvailableSlots(3L, DAY))
        .thenReturn(List.of(new SlotResponse(10L, 3L, DAY.atTime(9, 0), DAY.atTime(9, 30))));

    mockMvc
        .perform(get("/doctors/3/slots").param("date", "2026-10-08"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(10))
        .andExpect(jsonPath("$[0].doctorId").value(3));
  }

  @Test
  void availableSlotsRequiresTheDate() throws Exception {
    mockMvc.perform(get("/doctors/3/slots")).andExpect(status().isBadRequest());

    verifyNoInteractions(doctorService);
  }

  @Test
  void availableSlotsRejectsAMalformedDate() throws Exception {
    mockMvc
        .perform(get("/doctors/3/slots").param("date", "tomorrow"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(doctorService);
  }

  @Test
  void availableSlotsReturnsNotFoundForAnUnknownDoctor() throws Exception {
    when(doctorService.findAvailableSlots(3L, DAY)).thenThrow(new DoctorNotFoundException(3L));

    mockMvc
        .perform(get("/doctors/3/slots").param("date", "2026-10-08"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("DOCTOR_NOT_FOUND"));
  }
}
