package com.edstem.interviewprep.expenses.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.expenses.dto.response.CsvFile;
import com.edstem.interviewprep.expenses.dto.response.ExpenseResponse;
import com.edstem.interviewprep.expenses.dto.response.MonthlySummaryResponse;
import com.edstem.interviewprep.expenses.entity.ExpenseCategory;
import com.edstem.interviewprep.expenses.exception.ExpenseNotFoundException;
import com.edstem.interviewprep.expenses.exception.InvalidDateRangeException;
import com.edstem.interviewprep.expenses.service.ExpenseService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
        .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
  }

  @Test
  void createRejectsMissingBody() throws Exception {
    mockMvc
        .perform(post("/expenses").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
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
        .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
  }

  @Test
  void listRejectsUnknownCategory() throws Exception {
    mockMvc
        .perform(get("/expenses").param("category", "GAMBLING"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
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
  void summaryReturnsTotalsPerCategoryAndOverall() throws Exception {
    Map<ExpenseCategory, BigDecimal> totals = new EnumMap<>(ExpenseCategory.class);
    totals.put(ExpenseCategory.FOOD, new BigDecimal("0.10"));
    totals.put(ExpenseCategory.TRAVEL, new BigDecimal("0.20"));
    totals.put(ExpenseCategory.BILLS, new BigDecimal("0.00"));
    totals.put(ExpenseCategory.OTHER, new BigDecimal("0.00"));
    when(service.summarize(YearMonth.of(2026, 3)))
        .thenReturn(
            new MonthlySummaryResponse(YearMonth.of(2026, 3), totals, new BigDecimal("0.30")));

    mockMvc
        .perform(get("/expenses/summary").param("month", "2026-03"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.month").value("2026-03"))
        .andExpect(jsonPath("$.totals.FOOD").value(0.10))
        .andExpect(jsonPath("$.totals.TRAVEL").value(0.20))
        .andExpect(jsonPath("$.total").value(0.30));
  }

  @Test
  void summaryCsvIsDownloadedAsNamedCsvFile() throws Exception {
    when(service.exportSummary(YearMonth.of(2026, 3)))
        .thenReturn(new CsvFile("expense-summary-2026-03.csv", "category,total\r\nTOTAL,0.00\r\n"));

    mockMvc
        .perform(get("/expenses/summary/csv").param("month", "2026-03"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/csv"))
        .andExpect(
            header()
                .string(
                    "Content-Disposition", "attachment; filename=\"expense-summary-2026-03.csv\""))
        .andExpect(content().string("category,total\r\nTOTAL,0.00\r\n"));
  }

  @Test
  void summaryCsvRejectsInvalidMonthWithJsonError() throws Exception {
    mockMvc
        .perform(get("/expenses/summary/csv").param("month", "2026-13"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
  }

  @Test
  void summaryCsvRequiresMonth() throws Exception {
    mockMvc
        .perform(get("/expenses/summary/csv"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
  }

  @Test
  void summaryRejectsInvalidMonth() throws Exception {
    mockMvc
        .perform(get("/expenses/summary").param("month", "2026-13"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
  }

  @Test
  void summaryRequiresMonth() throws Exception {
    mockMvc
        .perform(get("/expenses/summary"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
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
  void unsupportedMethodReturnsMethodNotAllowed() throws Exception {
    mockMvc
        .perform(patch("/expenses"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.status").value(405))
        .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
  }

  @Test
  void unknownPathReturnsNotFound() throws Exception {
    mockMvc
        .perform(get("/unknown"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
  }

  @Test
  void unsupportedContentTypeReturnsUnsupportedMediaType() throws Exception {
    mockMvc
        .perform(post("/expenses").contentType(MediaType.TEXT_PLAIN).content("x"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.status").value(415))
        .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"));
  }

  @Test
  void unexpectedFailureReturnsGenericErrorWithoutDetails() throws Exception {
    when(service.list(any(), any(), any())).thenThrow(new IllegalStateException("db password"));

    mockMvc
        .perform(get("/expenses"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.message").value("Unexpected error"));
  }

  private static ExpenseResponse response(
      Long id, String amount, ExpenseCategory category, LocalDate date) {
    return new ExpenseResponse(id, new BigDecimal(amount), category, date, null);
  }
}
