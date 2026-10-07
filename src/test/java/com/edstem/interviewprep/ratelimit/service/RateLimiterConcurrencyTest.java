package com.edstem.interviewprep.ratelimit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.edstem.interviewprep.ratelimit.config.RateLimitProperties;
import com.edstem.interviewprep.ratelimit.exception.RateLimitExceededException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RateLimiterConcurrencyTest {

  private static final int MAX_REQUESTS = 10;
  private static final int THREADS = 100;

  @Test
  void admitsExactlyTheLimitWhenManyRequestsArriveTogether() throws Exception {
    RateLimiter rateLimiter =
        new RateLimiter(
            new RateLimitProperties(MAX_REQUESTS, Duration.ofHours(1)),
            Clock.systemUTC(),
            new InMemoryRateLimitStore());
    AtomicInteger allowed = new AtomicInteger();
    AtomicInteger rejected = new AtomicInteger();
    CountDownLatch ready = new CountDownLatch(THREADS);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(THREADS);

    List<Future<?>> futures = new ArrayList<>();
    for (int thread = 0; thread < THREADS; thread++) {
      futures.add(
          executor.submit(
              () -> {
                ready.countDown();
                start.await();
                try {
                  rateLimiter.checkRequest("client-a");
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

    assertEquals(MAX_REQUESTS, allowed.get());
    assertEquals(THREADS - MAX_REQUESTS, rejected.get());
  }
}
