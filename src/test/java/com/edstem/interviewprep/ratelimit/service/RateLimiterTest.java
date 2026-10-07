package com.edstem.interviewprep.ratelimit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    rateLimiter = limiter(MAX_REQUESTS, new InMemoryRateLimitStore());
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
    rateLimiter = limiter(2, new InMemoryRateLimitStore());
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
    rateLimiter = limiter(2, new InMemoryRateLimitStore());
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
  void sweepsExpiredCountsOncePerWindow() {
    RateLimitStore store = mock(RateLimitStore.class);
    when(store.record(anyString(), anyLong(), anyInt(), anyLong()))
        .thenReturn(RateLimitDecision.accepted());
    when(clock.millis()).thenReturn(0L, 1_000L, 60_000L);
    rateLimiter = limiter(MAX_REQUESTS, store);

    rateLimiter.checkRequest("client-a");
    rateLimiter.checkRequest("client-a");
    rateLimiter.checkRequest("client-a");

    verify(store, times(3)).record(anyString(), anyLong(), anyInt(), anyLong());
    verify(store).deleteExpired(-60_000L);
    verify(store).deleteExpired(0L);
    verify(store, times(2)).deleteExpired(anyLong());
  }

  @Test
  void tellsTheClientHowLongToWaitFromTheStoredWindowStart() {
    RateLimitStore store = mock(RateLimitStore.class);
    when(store.record("client-a", 20_000L, MAX_REQUESTS, 60_000L))
        .thenReturn(RateLimitDecision.rejected(5_000L));
    when(clock.millis()).thenReturn(20_000L);
    rateLimiter = limiter(MAX_REQUESTS, store);

    RateLimitExceededException exception =
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRequest("client-a"));

    assertEquals(45, exception.getRetryAfterSeconds());
  }

  private RateLimiter limiter(int maxRequests, RateLimitStore store) {
    return new RateLimiter(new RateLimitProperties(maxRequests, WINDOW), clock, store);
  }
}
