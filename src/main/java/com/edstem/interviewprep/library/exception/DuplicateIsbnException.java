package com.edstem.interviewprep.library.exception;

import org.springframework.http.HttpStatus;

public class DuplicateIsbnException extends LibraryException {

  public DuplicateIsbnException(String isbn) {
    super(HttpStatus.CONFLICT, "DUPLICATE_ISBN", "A book with ISBN " + isbn + " already exists");
  }
}
