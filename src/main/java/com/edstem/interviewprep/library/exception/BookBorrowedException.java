package com.edstem.interviewprep.library.exception;

import org.springframework.http.HttpStatus;

public class BookBorrowedException extends LibraryException {

  public BookBorrowedException(Long id) {
    super(
        HttpStatus.CONFLICT,
        "BOOK_CURRENTLY_BORROWED",
        "Book " + id + " is currently borrowed and cannot be deleted");
  }
}
