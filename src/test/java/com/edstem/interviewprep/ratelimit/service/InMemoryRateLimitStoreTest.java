package com.edstem.interviewprep.ratelimit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InMemoryRateLimitStoreTest {

  private static final long WINDOW = 60_000L;

  private final InMemoryRateLimitStore store = new InMemoryRateLimitStore();

  @Test
  void allowsUpToTheLimitThenRejectsWithTheWindowStart() {
    for (int request = 1; request <= 3; request++) {
      assertTrue(store.record("client-a", 1_000L, 3, WINDOW).allowed());
    }

    RateLimitDecision decision = store.record("client-a", 2_000L, 3, WINDOW);

    assertFalse(decision.allowed());
    assertEquals(1_000L, decision.windowStartMillis());
  }

  @Test
  void startsANewWindowOnceTheOldOneEnds() {
    store.record("client-a", 0L, 1, WINDOW);

    RateLimitDecision stillInside = store.record("client-a", 59_999L, 1, WINDOW);
    RateLimitDecision afterEnd = store.record("client-a", 60_000L, 1, WINDOW);

    assertFalse(stillInside.allowed());
    assertTrue(afterEnd.allowed());
  }

  @Test
  void keepsApiKeysApart() {
    store.record("client-a", 0L, 1, WINDOW);

    RateLimitDecision other = store.record("client-b", 0L, 1, WINDOW);

    assertTrue(other.allowed());
  }

  @Test
  void deleteExpiredRemovesOnlyFinishedWindows() {
    store.record("old", 0L, 1, WINDOW);
    store.record("recent", 50_000L, 1, WINDOW);

    store.deleteExpired(0L);

    assertEquals(1, store.size());
  }
}
