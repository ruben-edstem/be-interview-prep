package com.edstem.interviewprep.library.dto.response;

public record BookResponse(
    Long id, String title, String author, String isbn, int publishedYear, boolean available) {}
