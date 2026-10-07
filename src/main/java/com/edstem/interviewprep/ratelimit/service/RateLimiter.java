package com.edstem.interviewprep.ratelimit.service;

import com.edstem.interviewprep.ratelimit.config.RateLimitProperties;
import com.edstem.interviewprep.ratelimit.exception.RateLimitExceededException;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimiter {

  private final RateLimitProperties properties;
  private final Clock clock;
  private final RateLimitStore store;
  private final AtomicLong nextSweepMillis = new AtomicLong();

  public void checkRequest(String apiKey) {
    long now = clock.millis();
    long windowMillis = properties.window().toMillis();
    sweepExpired(now, windowMillis);

    RateLimitDecision decision = store.record(apiKey, now, properties.maxRequests(), windowMillis);

    if (!decision.allowed()) {
      throw new RateLimitExceededException(
          retryAfterSeconds(decision.windowStartMillis(), now, windowMillis));
    }
  }

  private long retryAfterSeconds(long windowStartMillis, long now, long windowMillis) {
    long remainingMillis = windowStartMillis + windowMillis - now;
    return Math.max(1, (remainingMillis + 999) / 1000);
  }

  private void sweepExpired(long now, long windowMillis) {
    long due = nextSweepMillis.get();
    if (now >= due && nextSweepMillis.compareAndSet(due, now + windowMillis)) {
      store.deleteExpired(now - windowMillis);
    }
  }
}
