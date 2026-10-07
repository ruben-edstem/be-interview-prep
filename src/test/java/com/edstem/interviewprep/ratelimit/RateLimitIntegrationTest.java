package com.edstem.interviewprep.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitIntegrationTest {

  private static final String RANDOM_QUOTE_URL = "/api/quotes/random";
  private static final String API_KEY_HEADER = "X-API-Key";
  private static final int LIMIT = 10;

  @Autowired private MockMvc mockMvc;

  @Test
  void rejectsTheEleventhRequestInAMinute() throws Exception {
    String apiKey = UUID.randomUUID().toString();
    for (int request = 1; request <= LIMIT; request++) {
      mockMvc
          .perform(get(RANDOM_QUOTE_URL).header(API_KEY_HEADER, apiKey))
          .andExpect(status().isOk());
    }

    mockMvc
        .perform(get(RANDOM_QUOTE_URL).header(API_KEY_HEADER, apiKey))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"));
  }

  @Test
  void doesNotLetOneApiKeyAffectAnother() throws Exception {
    String busyKey = UUID.randomUUID().toString();
    String quietKey = UUID.randomUUID().toString();
    for (int request = 1; request <= LIMIT + 1; request++) {
      mockMvc.perform(get(RANDOM_QUOTE_URL).header(API_KEY_HEADER, busyKey));
    }

    mockMvc
        .perform(get(RANDOM_QUOTE_URL).header(API_KEY_HEADER, busyKey))
        .andExpect(status().isTooManyRequests());
    mockMvc
        .perform(get(RANDOM_QUOTE_URL).header(API_KEY_HEADER, quietKey))
        .andExpect(status().isOk());
  }
}
