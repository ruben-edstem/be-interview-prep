package com.edstem.interviewprep.ratelimit.service;

import com.edstem.interviewprep.ratelimit.repository.RateLimitWindowRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "ratelimit",
    name = "store",
    havingValue = "database",
    matchIfMissing = true)
public class DatabaseRateLimitStore implements RateLimitStore {

  static final int MAX_ATTEMPTS = 5;

  private final RateLimitWindowRepository repository;

  @Override
  public RateLimitDecision record(
      String apiKey, long nowMillis, int maxRequests, long windowMillis) {
    String key = hash(apiKey);
    long cutoff = nowMillis - windowMillis;

    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      if (repository.increment(key, cutoff, maxRequests) == 1
          || repository.restart(key, nowMillis, cutoff) == 1) {
        return RateLimitDecision.accepted();
      }

      Optional<Long> windowStart = repository.findWindowStart(key);
      if (windowStart.isPresent()) {
        if (windowStart.get() > cutoff) {
          return RateLimitDecision.rejected(windowStart.get());
        }
      } else if (insertFirst(key, nowMillis)) {
        return RateLimitDecision.accepted();
      }
    }
    throw new IllegalStateException("Could not record the request for rate limiting");
  }

  @Override
  public void deleteExpired(long cutoffMillis) {
    repository.deleteExpired(cutoffMillis);
  }

  private boolean insertFirst(String key, long nowMillis) {
    try {
      return repository.insertFirst(key, nowMillis) == 1;
    } catch (DataIntegrityViolationException exception) {
      return false;
    }
  }

  private String hash(String apiKey) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(apiKey.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }
}
