package com.edstem.interviewprep.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edstem.interviewprep.booking.support.MutableClock;
import com.edstem.interviewprep.library.config.LibraryProperties;
import com.edstem.interviewprep.library.dto.request.BorrowRequest;
import com.edstem.interviewprep.library.dto.response.LoanResponse;
import com.edstem.interviewprep.library.dto.response.OverdueLoanResponse;
import com.edstem.interviewprep.library.entity.Book;
import com.edstem.interviewprep.library.entity.Loan;
import com.edstem.interviewprep.library.exception.BookAlreadyBorrowedException;
import com.edstem.interviewprep.library.exception.BookNotBorrowedException;
import com.edstem.interviewprep.library.exception.BookNotFoundException;
import com.edstem.interviewprep.library.mapper.LoanMapper;
import com.edstem.interviewprep.library.repository.BookRepository;
import com.edstem.interviewprep.library.repository.LoanRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

  @Mock private BookRepository bookRepository;
  @Mock private LoanRepository loanRepository;

  private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
  private static final Duration LOAN_PERIOD = Duration.ofDays(14);

  private LoanService loanService;

  @BeforeEach
  void setUp() {
    loanService =
        new LoanService(
            bookRepository,
            loanRepository,
            new LoanMapper(),
            new LibraryProperties(LOAN_PERIOD),
            new MutableClock(NOW));
  }

  @Test
  void borrowRecordsALoanForTheMember() {
    Book book = Book.builder().id(1L).build();
    when(bookRepository.markBorrowed(1L)).thenReturn(1);
    when(bookRepository.getReferenceById(1L)).thenReturn(book);
    when(loanRepository.save(any(Loan.class)))
        .thenAnswer(
            invocation -> {
              Loan loan = invocation.getArgument(0);
              loan.setId(5L);
              return loan;
            });

    LoanResponse response = loanService.borrow(1L, new BorrowRequest(" member-7 "));

    assertThat(response.id()).isEqualTo(5L);
    assertThat(response.bookId()).isEqualTo(1L);
    assertThat(response.memberId()).isEqualTo("member-7");
    assertThat(response.returnedAt()).isNull();
  }

  @Test
  void borrowRejectsABookThatIsAlreadyBorrowed() {
    when(bookRepository.markBorrowed(1L)).thenReturn(0);
    when(bookRepository.existsById(1L)).thenReturn(true);

    assertThatThrownBy(() -> loanService.borrow(1L, new BorrowRequest("member-7")))
        .isInstanceOf(BookAlreadyBorrowedException.class);

    verify(loanRepository, never()).save(any());
  }

  @Test
  void borrowThrowsWhenTheBookDoesNotExist() {
    when(bookRepository.markBorrowed(9L)).thenReturn(0);
    when(bookRepository.existsById(9L)).thenReturn(false);

    assertThatThrownBy(() -> loanService.borrow(9L, new BorrowRequest("member-7")))
        .isInstanceOf(BookNotFoundException.class);
  }

  @Test
  void returnClosesTheOpenLoan() {
    Loan loan =
        Loan.builder().id(5L).book(Book.builder().id(1L).build()).memberId("member-7").build();
    when(bookRepository.markReturned(1L)).thenReturn(1);
    when(loanRepository.findByBookIdAndReturnedAtIsNull(1L)).thenReturn(Optional.of(loan));

    LoanResponse response = loanService.returnBook(1L);

    assertThat(response.returnedAt()).isNotNull();
    assertThat(loan.getReturnedAt()).isEqualTo(response.returnedAt());
  }

  @Test
  void returnRejectsABookThatIsNotBorrowed() {
    when(bookRepository.markReturned(1L)).thenReturn(0);
    when(bookRepository.existsById(1L)).thenReturn(true);

    assertThatThrownBy(() -> loanService.returnBook(1L))
        .isInstanceOf(BookNotBorrowedException.class);
  }

  @Test
  void returnThrowsWhenTheBookDoesNotExist() {
    when(bookRepository.markReturned(9L)).thenReturn(0);
    when(bookRepository.existsById(9L)).thenReturn(false);

    assertThatThrownBy(() -> loanService.returnBook(9L)).isInstanceOf(BookNotFoundException.class);
  }

  @Test
  void returnFailsWhenTheBorrowedBookHasNoOpenLoan() {
    when(bookRepository.markReturned(1L)).thenReturn(1);
    when(loanRepository.findByBookIdAndReturnedAtIsNull(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> loanService.returnBook(1L))
        .isInstanceOf(BookNotBorrowedException.class);
  }

  @Test
  void overdueSearchStartsExactlyOneLoanPeriodBack() {
    when(loanRepository.findOverdue(NOW.minus(LOAN_PERIOD))).thenReturn(List.of());

    List<OverdueLoanResponse> overdue = loanService.findOverdue();

    assertThat(overdue).isEmpty();
    verify(loanRepository).findOverdue(Instant.parse("2026-10-06T12:00:00Z"));
  }

  @Test
  void overdueLoanShowsTheBookTheMemberAndTheBorrowTime() {
    Instant borrowedAt = NOW.minus(Duration.ofDays(20));
    Loan loan = openLoan(5L, "member-7", borrowedAt);
    when(loanRepository.findOverdue(NOW.minus(LOAN_PERIOD))).thenReturn(List.of(loan));

    List<OverdueLoanResponse> overdue = loanService.findOverdue();

    assertThat(overdue)
        .containsExactly(new OverdueLoanResponse(5L, 1L, "Clean Code", "member-7", borrowedAt, 6));
  }

  @Test
  void aLoanOverdueByLessThanADayCountsAsOneDay() {
    Loan loan = openLoan(5L, "member-7", NOW.minus(LOAN_PERIOD).minusSeconds(1));
    when(loanRepository.findOverdue(NOW.minus(LOAN_PERIOD))).thenReturn(List.of(loan));

    List<OverdueLoanResponse> overdue = loanService.findOverdue();

    assertThat(overdue).extracting(OverdueLoanResponse::daysOverdue).containsExactly(1L);
  }

  @Test
  void aLoanOverdueByExactlyWholeDaysCountsThoseDays() {
    Loan loan = openLoan(5L, "member-7", NOW.minus(LOAN_PERIOD).minus(Duration.ofDays(3)));
    when(loanRepository.findOverdue(NOW.minus(LOAN_PERIOD))).thenReturn(List.of(loan));

    List<OverdueLoanResponse> overdue = loanService.findOverdue();

    assertThat(overdue).extracting(OverdueLoanResponse::daysOverdue).containsExactly(3L);
  }

  @Test
  void aLoanOverdueByPartOfADayRoundsUp() {
    Instant borrowedAt =
        NOW.minus(LOAN_PERIOD).minus(Duration.ofDays(3)).minus(Duration.ofHours(1));
    Loan loan = openLoan(5L, "member-7", borrowedAt);
    when(loanRepository.findOverdue(NOW.minus(LOAN_PERIOD))).thenReturn(List.of(loan));

    List<OverdueLoanResponse> overdue = loanService.findOverdue();

    assertThat(overdue).extracting(OverdueLoanResponse::daysOverdue).containsExactly(4L);
  }

  private Loan openLoan(Long id, String memberId, Instant borrowedAt) {
    Book book = Book.builder().id(1L).title("Clean Code").build();
    return Loan.builder().id(id).book(book).memberId(memberId).borrowedAt(borrowedAt).build();
  }
}
