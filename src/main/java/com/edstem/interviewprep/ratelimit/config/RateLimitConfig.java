package com.edstem.interviewprep.ratelimit.config;

import com.edstem.interviewprep.ratelimit.service.RateLimiter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

  @Bean
  public WebMvcConfigurer rateLimitWebMvcConfigurer(RateLimiter rateLimiter) {
    RateLimitInterceptor interceptor = new RateLimitInterceptor(rateLimiter);
    return new WebMvcConfigurer() {
      @Override
      public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/quotes/**");
      }
    };
  }
}
