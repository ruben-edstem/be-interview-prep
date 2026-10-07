package com.edstem.interviewprep.library.mapper;

import com.edstem.interviewprep.library.dto.response.LoanResponse;
import com.edstem.interviewprep.library.dto.response.OverdueLoanResponse;
import com.edstem.interviewprep.library.entity.Loan;
import org.springframework.stereotype.Component;

@Component
public class LoanMapper {

  public LoanResponse toResponse(Loan loan) {
    return new LoanResponse(
        loan.getId(),
        loan.getBook().getId(),
        loan.getMemberId(),
        loan.getBorrowedAt(),
        loan.getReturnedAt());
  }

  public OverdueLoanResponse toOverdueResponse(Loan loan, long daysOverdue) {
    return new OverdueLoanResponse(
        loan.getId(),
        loan.getBook().getId(),
        loan.getBook().getTitle(),
        loan.getMemberId(),
        loan.getBorrowedAt(),
        daysOverdue);
  }
}
