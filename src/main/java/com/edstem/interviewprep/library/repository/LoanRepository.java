package com.edstem.interviewprep.library.repository;

import com.edstem.interviewprep.library.entity.Loan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {

  Optional<Loan> findByBookIdAndReturnedAtIsNull(Long bookId);
}
