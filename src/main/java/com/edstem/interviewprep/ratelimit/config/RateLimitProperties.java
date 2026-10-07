package com.edstem.interviewprep.ratelimit.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ratelimit")
public record RateLimitProperties(int maxRequests, Duration window) {

  public RateLimitProperties {
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("ratelimit.max-requests must be positive");
    }
    if (window == null || window.isZero() || window.isNegative()) {
      throw new IllegalArgumentException("ratelimit.window must be positive");
    }
  }
}
