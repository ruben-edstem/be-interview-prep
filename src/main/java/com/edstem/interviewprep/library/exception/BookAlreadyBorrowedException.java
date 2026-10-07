package com.edstem.interviewprep.library.exception;

import org.springframework.http.HttpStatus;

public class BookAlreadyBorrowedException extends LibraryException {

  public BookAlreadyBorrowedException(Long id) {
    super(
        HttpStatus.CONFLICT,
        "BOOK_ALREADY_BORROWED",
        "Book " + id + " is already borrowed and must be returned before it can be borrowed again");
  }
}
