package com.edstem.interviewprep.ratelimit.service;

public interface RateLimitStore {

  RateLimitDecision record(String apiKey, long nowMillis, int maxRequests, long windowMillis);

  void deleteExpired(long cutoffMillis);
}
