package com.edstem.interviewprep.ratelimit.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.ratelimit.config.RateLimitConfig;
import com.edstem.interviewprep.ratelimit.dto.response.QuoteResponse;
import com.edstem.interviewprep.ratelimit.exception.RateLimitExceededException;
import com.edstem.interviewprep.ratelimit.service.QuoteService;
import com.edstem.interviewprep.ratelimit.service.RateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QuoteController.class)
@Import(RateLimitConfig.class)
class QuoteControllerTest {

  private static final String RANDOM_QUOTE_URL = "/api/quotes/random";
  private static final String API_KEY_HEADER = "X-API-Key";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private QuoteService quoteService;
  @MockitoBean private RateLimiter rateLimiter;

  @Test
  void returnsAQuoteForARequestWithinTheLimit() throws Exception {
    when(quoteService.getRandomQuote()).thenReturn(new QuoteResponse("Hello", "Someone"));

    mockMvc
        .perform(get(RANDOM_QUOTE_URL).header(API_KEY_HEADER, "client-a"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.text").value("Hello"))
        .andExpect(jsonPath("$.author").value("Someone"));

    verify(rateLimiter).checkRequest("client-a");
  }

  @Test
  void returns429WithRetryAfterWhenTheLimitIsExceeded() throws Exception {
    doThrow(new RateLimitExceededException(42)).when(rateLimiter).checkRequest("client-a");

    mockMvc
        .perform(get(RANDOM_QUOTE_URL).header(API_KEY_HEADER, "client-a"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "42"))
        .andExpect(jsonPath("$.status").value(429))
        .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
        .andExpect(jsonPath("$.message").isNotEmpty());

    verify(quoteService, never()).getRandomQuote();
  }

  @Test
  void returns400WhenTheApiKeyHeaderIsMissing() throws Exception {
    mockMvc
        .perform(get(RANDOM_QUOTE_URL))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errorCode").value("MISSING_API_KEY"));

    verifyNoInteractions(rateLimiter);
  }

  @Test
  void returns400WhenTheApiKeyHeaderIsBlank() throws Exception {
    mockMvc
        .perform(get(RANDOM_QUOTE_URL).header(API_KEY_HEADER, "   "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("MISSING_API_KEY"));

    verify(rateLimiter, never()).checkRequest(anyString());
  }
}
