package com.edstem.interviewprep.library.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.common.exception.GlobalExceptionHandler;
import com.edstem.interviewprep.library.dto.response.OverdueLoanResponse;
import com.edstem.interviewprep.library.service.LoanService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OverdueLoanController.class)
@Import(GlobalExceptionHandler.class)
class OverdueLoanControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LoanService loanService;

  @Test
  void listsTheOverdueLoansWithBookMemberBorrowTimeAndDaysOverdue() throws Exception {
    when(loanService.findOverdue())
        .thenReturn(
            List.of(
                new OverdueLoanResponse(
                    5L, 1L, "Clean Code", "member-7", Instant.parse("2026-09-30T10:00:00Z"), 6)));

    mockMvc
        .perform(get("/loans/overdue"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].loanId").value(5))
        .andExpect(jsonPath("$[0].bookId").value(1))
        .andExpect(jsonPath("$[0].bookTitle").value("Clean Code"))
        .andExpect(jsonPath("$[0].memberId").value("member-7"))
        .andExpect(jsonPath("$[0].borrowedAt").value("2026-09-30T10:00:00Z"))
        .andExpect(jsonPath("$[0].daysOverdue").value(6));
  }

  @Test
  void returnsAnEmptyListWhenNothingIsOverdue() throws Exception {
    when(loanService.findOverdue()).thenReturn(List.of());

    mockMvc
        .perform(get("/loans/overdue"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }
}
