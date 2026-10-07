package com.edstem.interviewprep.library.service;

import com.edstem.interviewprep.library.dto.request.BorrowRequest;
import com.edstem.interviewprep.library.dto.response.LoanResponse;
import com.edstem.interviewprep.library.entity.Loan;
import com.edstem.interviewprep.library.exception.BookAlreadyBorrowedException;
import com.edstem.interviewprep.library.exception.BookNotBorrowedException;
import com.edstem.interviewprep.library.exception.BookNotFoundException;
import com.edstem.interviewprep.library.mapper.LoanMapper;
import com.edstem.interviewprep.library.repository.BookRepository;
import com.edstem.interviewprep.library.repository.LoanRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoanService {

  private final BookRepository bookRepository;
  private final LoanRepository loanRepository;
  private final LoanMapper loanMapper;

  @Transactional
  public LoanResponse borrow(Long bookId, BorrowRequest request) {
    if (bookRepository.markBorrowed(bookId) == 0) {
      throw bookRepository.existsById(bookId)
          ? new BookAlreadyBorrowedException(bookId)
          : new BookNotFoundException(bookId);
    }
    Loan loan =
        Loan.builder()
            .book(bookRepository.getReferenceById(bookId))
            .memberId(request.memberId().trim())
            .build();
    return loanMapper.toResponse(loanRepository.save(loan));
  }

  @Transactional
  public LoanResponse returnBook(Long bookId) {
    if (bookRepository.markReturned(bookId) == 0) {
      throw bookRepository.existsById(bookId)
          ? new BookNotBorrowedException(bookId)
          : new BookNotFoundException(bookId);
    }
    Loan loan =
        loanRepository
            .findByBookIdAndReturnedAtIsNull(bookId)
            .orElseThrow(() -> new BookNotBorrowedException(bookId));
    loan.setReturnedAt(Instant.now());
    return loanMapper.toResponse(loan);
  }
}
