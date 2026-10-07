package com.edstem.interviewprep.library.controller;

import com.edstem.interviewprep.library.dto.request.BorrowRequest;
import com.edstem.interviewprep.library.dto.response.LoanResponse;
import com.edstem.interviewprep.library.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books/{bookId}")
@RequiredArgsConstructor
public class LoanController {

  private final LoanService loanService;

  @PostMapping("/loans")
  @ResponseStatus(HttpStatus.CREATED)
  public LoanResponse borrow(@PathVariable Long bookId, @Valid @RequestBody BorrowRequest request) {
    return loanService.borrow(bookId, request);
  }

  @PostMapping("/returns")
  public LoanResponse returnBook(@PathVariable Long bookId) {
    return loanService.returnBook(bookId);
  }
}
