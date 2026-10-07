package com.edstem.interviewprep.booking.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@EnableConfigurationProperties(BookingProperties.class)
public class BookingConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
