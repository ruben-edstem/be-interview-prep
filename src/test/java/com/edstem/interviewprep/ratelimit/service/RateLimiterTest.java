package com.edstem.interviewprep.ratelimit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.edstem.interviewprep.ratelimit.config.RateLimitProperties;
import com.edstem.interviewprep.ratelimit.exception.RateLimitExceededException;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateLimiterTest {

  private static final int MAX_REQUESTS = 10;
  private static final Duration WINDOW = Duration.ofMinutes(1);

  @Mock private Clock clock;

  private RateLimiter rateLimiter;

  @BeforeEach
  void setUp() {
    rateLimiter = new RateLimiter(new RateLimitProperties(MAX_REQUESTS, WINDOW), clock);
  }

  @Test
  void rejectsTheEleventhRequestInAMinute() {
    when(clock.millis()).thenReturn(0L);
    for (int request = 1; request <= MAX_REQUESTS; request++) {
      assertDoesNotThrow(() -> rateLimiter.checkRequest("client-a"));
    }

    RateLimitExceededException exception =
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRequest("client-a"));

    assertEquals(60, exception.getRetryAfterSeconds());
  }

  @Test
  void keepsRejectingUntilTheWindowEnds() {
    when(clock.millis()).thenReturn(0L, 0L, 59_000L, 59_999L);
    rateLimiter = new RateLimiter(new RateLimitProperties(2, WINDOW), clock);
    rateLimiter.checkRequest("client-a");
    rateLimiter.checkRequest("client-a");

    RateLimitExceededException early =
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRequest("client-a"));
    RateLimitExceededException late =
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRequest("client-a"));

    assertEquals(1, early.getRetryAfterSeconds());
    assertEquals(1, late.getRetryAfterSeconds());
  }

  @Test
  void allowsRequestsAgainOnceTheWindowHasPassed() {
    when(clock.millis()).thenReturn(0L, 0L, 0L, 60_000L);
    rateLimiter = new RateLimiter(new RateLimitProperties(2, WINDOW), clock);
    rateLimiter.checkRequest("client-a");
    rateLimiter.checkRequest("client-a");
    assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRequest("client-a"));

    assertDoesNotThrow(() -> rateLimiter.checkRequest("client-a"));
  }

  @Test
  void doesNotLetOneApiKeyAffectAnother() {
    when(clock.millis()).thenReturn(0L);
    for (int request = 1; request <= MAX_REQUESTS; request++) {
      rateLimiter.checkRequest("client-a");
    }
    assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRequest("client-a"));

    for (int request = 1; request <= MAX_REQUESTS; request++) {
      assertDoesNotThrow(() -> rateLimiter.checkRequest("client-b"));
    }
  }

  @Test
  void forgetsKeysWhoseWindowHasExpired() {
    when(clock.millis()).thenReturn(0L, 0L, 120_000L);
    rateLimiter.checkRequest("client-a");
    rateLimiter.checkRequest("client-b");
    assertEquals(2, rateLimiter.trackedKeyCount());

    rateLimiter.checkRequest("client-c");

    assertEquals(1, rateLimiter.trackedKeyCount());
  }
}
