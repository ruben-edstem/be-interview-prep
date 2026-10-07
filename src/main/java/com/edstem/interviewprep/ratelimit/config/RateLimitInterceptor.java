package com.edstem.interviewprep.ratelimit.config;

import com.edstem.interviewprep.ratelimit.exception.MissingApiKeyException;
import com.edstem.interviewprep.ratelimit.service.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

  public static final String API_KEY_HEADER = "X-API-Key";

  private final RateLimiter rateLimiter;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    String apiKey = request.getHeader(API_KEY_HEADER);
    if (apiKey == null || apiKey.isBlank()) {
      throw new MissingApiKeyException(API_KEY_HEADER);
    }

    rateLimiter.checkRequest(apiKey.strip());
    return true;
  }
}
