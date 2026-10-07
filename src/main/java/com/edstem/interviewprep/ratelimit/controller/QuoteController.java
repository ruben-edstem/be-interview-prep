package com.edstem.interviewprep.ratelimit.controller;

import com.edstem.interviewprep.ratelimit.dto.response.QuoteResponse;
import com.edstem.interviewprep.ratelimit.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteController {

  private final QuoteService quoteService;

  @GetMapping("/random")
  public QuoteResponse getRandomQuote() {
    return quoteService.getRandomQuote();
  }
}
