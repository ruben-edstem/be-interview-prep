package com.edstem.interviewprep.library.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.common.exception.GlobalExceptionHandler;
import com.edstem.interviewprep.library.dto.response.LoanResponse;
import com.edstem.interviewprep.library.exception.BookAlreadyBorrowedException;
import com.edstem.interviewprep.library.exception.BookNotBorrowedException;
import com.edstem.interviewprep.library.exception.BookNotFoundException;
import com.edstem.interviewprep.library.service.LoanService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LoanController.class)
@Import(GlobalExceptionHandler.class)
class LoanControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LoanService loanService;

  @Test
  void borrowReturnsCreatedWithTheLoan() throws Exception {
    when(loanService.borrow(eq(1L), any()))
        .thenReturn(
            new LoanResponse(5L, 1L, "member-7", Instant.parse("2026-10-07T10:00:00Z"), null));

    mockMvc
        .perform(
            post("/books/1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"member-7\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.bookId").value(1))
        .andExpect(jsonPath("$.memberId").value("member-7"))
        .andExpect(jsonPath("$.returnedAt").isEmpty());
  }

  @Test
  void borrowRejectsAMissingMemberId() throws Exception {
    mockMvc
        .perform(post("/books/1/loans").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors.memberId").exists());

    verifyNoInteractions(loanService);
  }

  @Test
  void borrowingAnUnavailableBookReturnsConflictWithAClearMessage() throws Exception {
    when(loanService.borrow(eq(1L), any())).thenThrow(new BookAlreadyBorrowedException(1L));

    mockMvc
        .perform(
            post("/books/1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"member-7\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.errorCode").value("BOOK_ALREADY_BORROWED"))
        .andExpect(jsonPath("$.message").value(containsString("already borrowed")));
  }

  @Test
  void borrowingAnUnknownBookReturnsNotFound() throws Exception {
    when(loanService.borrow(eq(9L), any())).thenThrow(new BookNotFoundException(9L));

    mockMvc
        .perform(
            post("/books/9/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"member-7\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("BOOK_NOT_FOUND"));
  }

  @Test
  void returnReturnsTheClosedLoan() throws Exception {
    Instant borrowedAt = Instant.parse("2026-10-07T10:00:00Z");
    when(loanService.returnBook(1L))
        .thenReturn(new LoanResponse(5L, 1L, "member-7", borrowedAt, borrowedAt.plusSeconds(60)));

    mockMvc
        .perform(post("/books/1/returns"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.returnedAt").exists());
  }

  @Test
  void returningABookThatIsNotBorrowedReturnsConflict() throws Exception {
    when(loanService.returnBook(1L)).thenThrow(new BookNotBorrowedException(1L));

    mockMvc
        .perform(post("/books/1/returns"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("BOOK_NOT_BORROWED"));
  }
}
