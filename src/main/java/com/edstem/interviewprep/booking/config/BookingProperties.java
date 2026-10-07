package com.edstem.interviewprep.booking.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "booking")
public record BookingProperties(Duration holdDuration, Duration slotDuration) {}
