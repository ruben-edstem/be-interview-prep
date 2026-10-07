package com.edstem.interviewprep.library.exception;

import org.springframework.http.HttpStatus;

public class BookNotBorrowedException extends LibraryException {

  public BookNotBorrowedException(Long id) {
    super(HttpStatus.CONFLICT, "BOOK_NOT_BORROWED", "Book " + id + " is not currently borrowed");
  }
}
