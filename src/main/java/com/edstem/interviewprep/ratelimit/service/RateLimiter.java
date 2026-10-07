package com.edstem.interviewprep.ratelimit.service;

import com.edstem.interviewprep.ratelimit.config.RateLimitProperties;
import com.edstem.interviewprep.ratelimit.exception.RateLimitExceededException;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimiter {

  private final RateLimitProperties properties;
  private final Clock clock;
  private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
  private final AtomicLong nextSweepMillis = new AtomicLong();

  public void checkRequest(String apiKey) {
    long now = clock.millis();
    long windowMillis = properties.window().toMillis();
    sweepExpired(now, windowMillis);

    Window window = windows.compute(apiKey, (key, current) -> next(current, now, windowMillis));

    if (window.count() > properties.maxRequests()) {
      throw new RateLimitExceededException(retryAfterSeconds(window, now, windowMillis));
    }
  }

  int trackedKeyCount() {
    return windows.size();
  }

  private Window next(Window current, long now, long windowMillis) {
    if (current == null || isExpired(current, now, windowMillis)) {
      return new Window(now, 1);
    }
    return new Window(current.startMillis(), Math.min(current.count() + 1, limitPlusOne()));
  }

  private int limitPlusOne() {
    return properties.maxRequests() + 1;
  }

  private boolean isExpired(Window window, long now, long windowMillis) {
    return now - window.startMillis() >= windowMillis;
  }

  private long retryAfterSeconds(Window window, long now, long windowMillis) {
    long remainingMillis = window.startMillis() + windowMillis - now;
    return Math.max(1, (remainingMillis + 999) / 1000);
  }

  private void sweepExpired(long now, long windowMillis) {
    long due = nextSweepMillis.get();
    if (now >= due && nextSweepMillis.compareAndSet(due, now + windowMillis)) {
      windows.values().removeIf(window -> isExpired(window, now, windowMillis));
    }
  }

  private record Window(long startMillis, int count) {}
}
