package com.edstem.interviewprep.booking.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.booking.dto.request.JoinWaitingListRequest;
import com.edstem.interviewprep.booking.dto.response.WaitingEntryResponse;
import com.edstem.interviewprep.booking.entity.WaitingStatus;
import com.edstem.interviewprep.booking.exception.AlreadyWaitingException;
import com.edstem.interviewprep.booking.exception.SlotAvailableException;
import com.edstem.interviewprep.booking.exception.SlotNotFoundException;
import com.edstem.interviewprep.booking.exception.WaitingEntryNotFoundException;
import com.edstem.interviewprep.booking.service.WaitingListService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WaitingListController.class)
class WaitingListControllerTest {

  private static final String JOIN_BODY = "{\"patientName\":\"Asha\"}";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private WaitingListService waitingListService;

  @Test
  void joinReturnsTheNewWaitingEntry() throws Exception {
    when(waitingListService.join(10L, new JoinWaitingListRequest("Asha")))
        .thenReturn(new WaitingEntryResponse(40L, 10L, "Asha", WaitingStatus.WAITING));

    mockMvc
        .perform(
            post("/slots/10/waiting-list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JOIN_BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(40))
        .andExpect(jsonPath("$.slotId").value(10))
        .andExpect(jsonPath("$.patientName").value("Asha"))
        .andExpect(jsonPath("$.status").value("WAITING"));
  }

  @Test
  void joinRejectsABlankPatientName() throws Exception {
    mockMvc
        .perform(
            post("/slots/10/waiting-list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"patientName\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors.patientName").value("must not be blank"));

    verifyNoInteractions(waitingListService);
  }

  @Test
  void joinReturnsConflictForAnAvailableSlot() throws Exception {
    when(waitingListService.join(10L, new JoinWaitingListRequest("Asha")))
        .thenThrow(new SlotAvailableException(10L));

    mockMvc
        .perform(
            post("/slots/10/waiting-list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JOIN_BODY))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("SLOT_AVAILABLE"));
  }

  @Test
  void joinReturnsConflictWhenTheyAreAlreadyWaiting() throws Exception {
    when(waitingListService.join(10L, new JoinWaitingListRequest("Asha")))
        .thenThrow(new AlreadyWaitingException(10L));

    mockMvc
        .perform(
            post("/slots/10/waiting-list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JOIN_BODY))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("ALREADY_WAITING"));
  }

  @Test
  void joinReturnsNotFoundForAnUnknownSlot() throws Exception {
    when(waitingListService.join(10L, new JoinWaitingListRequest("Asha")))
        .thenThrow(new SlotNotFoundException(10L));

    mockMvc
        .perform(
            post("/slots/10/waiting-list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JOIN_BODY))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("SLOT_NOT_FOUND"));
  }

  @Test
  void leaveReturnsNoContent() throws Exception {
    mockMvc.perform(delete("/waiting-list/40")).andExpect(status().isNoContent());

    verify(waitingListService).leave(40L);
  }

  @Test
  void leaveReturnsNotFoundForAnUnknownEntry() throws Exception {
    doThrow(new WaitingEntryNotFoundException(40L)).when(waitingListService).leave(40L);

    mockMvc
        .perform(delete("/waiting-list/40"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("WAITING_ENTRY_NOT_FOUND"));
  }
}
