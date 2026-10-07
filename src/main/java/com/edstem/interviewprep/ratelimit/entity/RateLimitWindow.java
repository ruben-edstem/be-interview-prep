package com.edstem.interviewprep.ratelimit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "rate_limit_windows",
    indexes = @Index(name = "idx_rate_limit_window_start", columnList = "window_start_millis"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RateLimitWindow {

  @Id
  @Column(name = "api_key_hash", length = 64, nullable = false)
  private String apiKeyHash;

  @Column(name = "window_start_millis", nullable = false)
  private long windowStartMillis;

  @Column(name = "request_count", nullable = false)
  private int requestCount;
}
