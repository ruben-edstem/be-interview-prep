package com.edstem.interviewprep.ratelimit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.edstem.interviewprep.ratelimit.config.RateLimitProperties;
import com.edstem.interviewprep.ratelimit.exception.RateLimitExceededException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import(DatabaseRateLimitStore.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SharedRateLimitTest {

  private static final int MAX_REQUESTS = 10;
  private static final int THREADS = 100;

  @Autowired private DatabaseRateLimitStore store;

  private RateLimiter firstInstance;
  private RateLimiter secondInstance;

  @BeforeEach
  void setUp() {
    RateLimitProperties properties = new RateLimitProperties(MAX_REQUESTS, Duration.ofHours(1));
    firstInstance = new RateLimiter(properties, Clock.systemUTC(), store);
    secondInstance = new RateLimiter(properties, Clock.systemUTC(), store);
  }

  @Test
  void twoLimitersSharingTheStoreAllowOnlyTenRequestsInTotal() {
    String apiKey = UUID.randomUUID().toString();
    for (int request = 1; request <= MAX_REQUESTS; request++) {
      RateLimiter instance = request % 2 == 0 ? firstInstance : secondInstance;
      assertDoesNotThrow(() -> instance.checkRequest(apiKey));
    }

    RateLimitExceededException onFirst =
        assertThrows(RateLimitExceededException.class, () -> firstInstance.checkRequest(apiKey));
    RateLimitExceededException onSecond =
        assertThrows(RateLimitExceededException.class, () -> secondInstance.checkRequest(apiKey));

    assertTrue(onFirst.getRetryAfterSeconds() > 0);
    assertTrue(onSecond.getRetryAfterSeconds() > 0);
  }

  @Test
  void theEleventhRequestIsRejectedEvenWhenItReachesTheOtherInstance() {
    String apiKey = UUID.randomUUID().toString();
    for (int request = 1; request <= MAX_REQUESTS; request++) {
      firstInstance.checkRequest(apiKey);
    }

    RateLimitExceededException exception =
        assertThrows(RateLimitExceededException.class, () -> secondInstance.checkRequest(apiKey));

    assertTrue(exception.getRetryAfterSeconds() > 3_500);
    assertTrue(exception.getRetryAfterSeconds() <= 3_600);
  }

  @Test
  void differentApiKeysDoNotAffectEachOtherAcrossInstances() {
    String busyKey = UUID.randomUUID().toString();
    String quietKey = UUID.randomUUID().toString();
    for (int request = 1; request <= MAX_REQUESTS; request++) {
      firstInstance.checkRequest(busyKey);
    }

    assertThrows(RateLimitExceededException.class, () -> secondInstance.checkRequest(busyKey));
    assertDoesNotThrow(() -> secondInstance.checkRequest(quietKey));
  }

  @Test
  void manyRequestsAtOnceAcrossTwoInstancesAllowExactlyTheLimit() throws Exception {
    String apiKey = UUID.randomUUID().toString();

    Outcome outcome = fireTogether(apiKey, THREADS);

    assertEquals(MAX_REQUESTS, outcome.allowed());
    assertEquals(THREADS - MAX_REQUESTS, outcome.rejected());
  }

  @Test
  void requestsRacingToStartAWindowAreNeverRejectedBelowTheLimit() throws Exception {
    int rounds = 30;
    int requestsPerRound = MAX_REQUESTS - 2;

    for (int round = 0; round < rounds; round++) {
      Outcome outcome = fireTogether(UUID.randomUUID().toString(), requestsPerRound);

      assertEquals(0, outcome.rejected(), "a request was rejected in round " + round);
    }
  }

  private Outcome fireTogether(String apiKey, int requests) throws Exception {
    AtomicInteger allowed = new AtomicInteger();
    AtomicInteger rejected = new AtomicInteger();
    CountDownLatch ready = new CountDownLatch(requests);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(requests);

    List<Future<?>> futures = new ArrayList<>();
    for (int thread = 0; thread < requests; thread++) {
      RateLimiter instance = thread % 2 == 0 ? firstInstance : secondInstance;
      futures.add(
          executor.submit(
              () -> {
                ready.countDown();
                start.await();
                try {
                  instance.checkRequest(apiKey);
                  allowed.incrementAndGet();
                } catch (RateLimitExceededException exception) {
                  rejected.incrementAndGet();
                }
                return null;
              }));
    }
    ready.await();
    start.countDown();
    for (Future<?> future : futures) {
      future.get();
    }
    executor.shutdown();

    return new Outcome(allowed.get(), rejected.get());
  }

  private record Outcome(int allowed, int rejected) {}
}
