package com.edstem.interviewprep.library.controller;

import com.edstem.interviewprep.library.dto.response.OverdueLoanResponse;
import com.edstem.interviewprep.library.service.LoanService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/loans/overdue")
@RequiredArgsConstructor
public class OverdueLoanController {

  private final LoanService loanService;

  @GetMapping
  public List<OverdueLoanResponse> findOverdue() {
    return loanService.findOverdue();
  }
}
