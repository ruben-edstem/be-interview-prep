package com.edstem.interviewprep.library.exception;

import org.springframework.http.HttpStatus;

public class BookNotFoundException extends LibraryException {

  public BookNotFoundException(Long id) {
    super(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Book " + id + " was not found");
  }
}
