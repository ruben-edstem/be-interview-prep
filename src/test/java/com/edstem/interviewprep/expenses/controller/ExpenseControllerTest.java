package com.edstem.interviewprep.expenses.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.expenses.dto.response.ExpenseResponse;
import com.edstem.interviewprep.expenses.entity.ExpenseCategory;
import com.edstem.interviewprep.expenses.exception.ExpenseNotFoundException;
import com.edstem.interviewprep.expenses.exception.InvalidDateRangeException;
import com.edstem.interviewprep.expenses.service.ExpenseService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExpenseController.class)
class ExpenseControllerTest {

  private static final String VALID_BODY =
      """
      {"amount": 12.50, "category": "FOOD", "date": "2026-03-01", "note": "Lunch"}
      """;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ExpenseService service;

  @Test
  void createReturnsCreatedExpense() throws Exception {
    when(service.create(any()))
        .thenReturn(response(1L, "12.50", ExpenseCategory.FOOD, LocalDate.of(2026, 3, 1)));

    mockMvc
        .perform(post("/expenses").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.amount").value(12.50))
        .andExpect(jsonPath("$.category").value("FOOD"))
        .andExpect(jsonPath("$.date").value("2026-03-01"));
  }

  @Test
  void createRejectsNonPositiveAmount() throws Exception {
    String body = VALID_BODY.replace("12.50", "0");

    mockMvc
        .perform(post("/expenses").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void createRejectsMoreThanTwoDecimalPlaces() throws Exception {
    String body = VALID_BODY.replace("12.50", "12.505");

    mockMvc
        .perform(post("/expenses").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void createRejectsMissingDate() throws Exception {
    String body =
        """
        {"amount": 5.00, "category": "FOOD"}
        """;

    mockMvc
        .perform(post("/expenses").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void createRejectsUnknownCategory() throws Exception {
    String body = VALID_BODY.replace("FOOD", "GAMBLING");

    mockMvc
        .perform(post("/expenses").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
  }

  @Test
  void createRejectsMissingBody() throws Exception {
    mockMvc
        .perform(post("/expenses").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
  }

  @Test
  void listPassesFiltersToService() throws Exception {
    LocalDate date = LocalDate.of(2026, 3, 5);
    when(service.list(date, LocalDate.of(2026, 3, 31), ExpenseCategory.TRAVEL))
        .thenReturn(List.of(response(2L, "40.00", ExpenseCategory.TRAVEL, date)));

    mockMvc
        .perform(
            get("/expenses")
                .param("from", "2026-03-05")
                .param("to", "2026-03-31")
                .param("category", "TRAVEL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(2))
        .andExpect(jsonPath("$[0].category").value("TRAVEL"));
  }

  @Test
  void listRejectsInvalidDate() throws Exception {
    mockMvc
        .perform(get("/expenses").param("from", "not-a-date"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"));
  }

  @Test
  void listRejectsUnknownCategory() throws Exception {
    mockMvc
        .perform(get("/expenses").param("category", "GAMBLING"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"));
  }

  @Test
  void listRejectsReversedRange() throws Exception {
    when(service.list(any(), any(), any())).thenThrow(new InvalidDateRangeException());

    mockMvc
        .perform(get("/expenses").param("from", "2026-03-31").param("to", "2026-03-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_DATE_RANGE"));
  }

  @Test
  void updateReturnsUpdatedExpense() throws Exception {
    when(service.update(any(), any()))
        .thenReturn(response(5L, "12.50", ExpenseCategory.FOOD, LocalDate.of(2026, 3, 1)));

    mockMvc
        .perform(put("/expenses/5").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(5));
  }

  @Test
  void updateOfMissingExpenseReturnsNotFound() throws Exception {
    when(service.update(any(), any())).thenThrow(new ExpenseNotFoundException(99L));

    mockMvc
        .perform(put("/expenses/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.errorCode").value("EXPENSE_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Expense 99 not found"));
  }

  @Test
  void deleteReturnsNoContent() throws Exception {
    mockMvc.perform(delete("/expenses/4")).andExpect(status().isNoContent());

    verify(service).delete(4L);
  }

  @Test
  void deleteOfMissingExpenseReturnsNotFound() throws Exception {
    doThrow(new ExpenseNotFoundException(99L)).when(service).delete(99L);

    mockMvc
        .perform(delete("/expenses/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("EXPENSE_NOT_FOUND"));
  }

  @Test
  void unexpectedFailureReturnsGenericErrorWithoutDetails() throws Exception {
    when(service.list(any(), any(), any())).thenThrow(new IllegalStateException("db password"));

    mockMvc
        .perform(get("/expenses"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.message").value("Unexpected error"));
  }

  private static ExpenseResponse response(
      Long id, String amount, ExpenseCategory category, LocalDate date) {
    return new ExpenseResponse(id, new BigDecimal(amount), category, date, null);
  }
}
