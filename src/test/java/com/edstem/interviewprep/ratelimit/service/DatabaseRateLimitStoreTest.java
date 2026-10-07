package com.edstem.interviewprep.ratelimit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import(DatabaseRateLimitStore.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DatabaseRateLimitStoreTest {

  private static final long WINDOW = 60_000L;

  @Autowired private DatabaseRateLimitStore store;

  @Test
  void allowsUpToTheLimitThenRejectsWithTheWindowStart() {
    String apiKey = UUID.randomUUID().toString();
    for (int request = 1; request <= 3; request++) {
      assertTrue(store.record(apiKey, 1_000L, 3, WINDOW).allowed());
    }

    RateLimitDecision decision = store.record(apiKey, 2_000L, 3, WINDOW);

    assertFalse(decision.allowed());
    assertEquals(1_000L, decision.windowStartMillis());
  }

  @Test
  void startsANewWindowOnceTheOldOneEnds() {
    String apiKey = UUID.randomUUID().toString();
    store.record(apiKey, 0L, 1, WINDOW);

    RateLimitDecision stillInside = store.record(apiKey, 59_999L, 1, WINDOW);
    RateLimitDecision afterEnd = store.record(apiKey, 60_000L, 1, WINDOW);
    RateLimitDecision insideNewWindow = store.record(apiKey, 60_001L, 1, WINDOW);

    assertFalse(stillInside.allowed());
    assertTrue(afterEnd.allowed());
    assertFalse(insideNewWindow.allowed());
    assertEquals(60_000L, insideNewWindow.windowStartMillis());
  }

  @Test
  void keepsApiKeysApart() {
    String busyKey = UUID.randomUUID().toString();
    String quietKey = UUID.randomUUID().toString();
    store.record(busyKey, 0L, 1, WINDOW);

    RateLimitDecision busy = store.record(busyKey, 0L, 1, WINDOW);
    RateLimitDecision quiet = store.record(quietKey, 0L, 1, WINDOW);

    assertFalse(busy.allowed());
    assertTrue(quiet.allowed());
  }

  @Test
  void acceptsAVeryLongApiKey() {
    String apiKey = "k".repeat(5_000);

    RateLimitDecision first = store.record(apiKey, 0L, 1, WINDOW);
    RateLimitDecision second = store.record(apiKey, 1L, 1, WINDOW);

    assertTrue(first.allowed());
    assertFalse(second.allowed());
  }

  @Test
  void deleteExpiredForgetsFinishedWindows() {
    String apiKey = UUID.randomUUID().toString();
    store.record(apiKey, 0L, 1, WINDOW);

    store.deleteExpired(0L);
    RateLimitDecision afterDelete = store.record(apiKey, 1_000L, 1, WINDOW);

    assertTrue(afterDelete.allowed());
  }

  @Test
  void deleteExpiredKeepsRunningWindows() {
    String apiKey = UUID.randomUUID().toString();
    store.record(apiKey, 50_000L, 1, WINDOW);

    store.deleteExpired(0L);
    RateLimitDecision afterDelete = store.record(apiKey, 51_000L, 1, WINDOW);

    assertFalse(afterDelete.allowed());
  }
}
