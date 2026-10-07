package com.edstem.interviewprep.ratelimit.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.edstem.interviewprep.ratelimit.dto.response.QuoteResponse;
import org.junit.jupiter.api.Test;

class QuoteServiceTest {

  private final QuoteService quoteService = new QuoteService();

  @Test
  void returnsAQuoteWithTextAndAuthor() {
    QuoteResponse quote = quoteService.getRandomQuote();

    assertNotNull(quote);
    assertFalse(quote.text().isBlank());
    assertFalse(quote.author().isBlank());
  }
}
