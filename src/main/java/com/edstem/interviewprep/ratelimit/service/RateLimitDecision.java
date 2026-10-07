package com.edstem.interviewprep.ratelimit.service;

public record RateLimitDecision(boolean allowed, long windowStartMillis) {

  public static RateLimitDecision accepted() {
    return new RateLimitDecision(true, 0);
  }

  public static RateLimitDecision rejected(long windowStartMillis) {
    return new RateLimitDecision(false, windowStartMillis);
  }
}
