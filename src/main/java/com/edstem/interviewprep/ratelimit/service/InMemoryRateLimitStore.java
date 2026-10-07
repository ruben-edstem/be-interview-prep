package com.edstem.interviewprep.ratelimit.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "ratelimit", name = "store", havingValue = "memory")
public class InMemoryRateLimitStore implements RateLimitStore {

  private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

  @Override
  public RateLimitDecision record(
      String apiKey, long nowMillis, int maxRequests, long windowMillis) {
    Window window =
        windows.compute(
            apiKey, (key, current) -> next(current, nowMillis, maxRequests, windowMillis));

    if (window.count() > maxRequests) {
      return RateLimitDecision.rejected(window.startMillis());
    }
    return RateLimitDecision.accepted();
  }

  @Override
  public void deleteExpired(long cutoffMillis) {
    windows.values().removeIf(window -> window.startMillis() <= cutoffMillis);
  }

  int size() {
    return windows.size();
  }

  private Window next(Window current, long nowMillis, int maxRequests, long windowMillis) {
    if (current == null || nowMillis - current.startMillis() >= windowMillis) {
      return new Window(nowMillis, 1);
    }
    return new Window(current.startMillis(), Math.min(current.count() + 1, maxRequests + 1));
  }

  private record Window(long startMillis, int count) {}
}
