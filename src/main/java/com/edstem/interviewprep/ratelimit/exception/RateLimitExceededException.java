package com.edstem.interviewprep.ratelimit.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@Getter
public class RateLimitExceededException extends ApiException {

  private final long retryAfterSeconds;

  public RateLimitExceededException(long retryAfterSeconds) {
    super(
        HttpStatus.TOO_MANY_REQUESTS,
        "RATE_LIMIT_EXCEEDED",
        "Rate limit exceeded. Try again in " + retryAfterSeconds + " seconds.");
    this.retryAfterSeconds = retryAfterSeconds;
  }

  @Override
  public HttpHeaders headers() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
    return headers;
  }
}
