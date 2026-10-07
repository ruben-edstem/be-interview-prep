package com.edstem.interviewprep.ratelimit.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RateLimitPropertiesTest {

  private static final Duration VALID_WINDOW = Duration.ofMinutes(1);

  @Test
  void acceptsAPositiveLimitAndWindow() {
    RateLimitProperties properties =
        assertDoesNotThrow(() -> new RateLimitProperties(10, VALID_WINDOW));

    assertEquals(10, properties.maxRequests());
    assertEquals(VALID_WINDOW, properties.window());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void rejectsALimitThatIsNotPositive(int maxRequests) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new RateLimitProperties(maxRequests, VALID_WINDOW));

    assertEquals("ratelimit.max-requests must be positive", exception.getMessage());
  }

  @Test
  void rejectsAMissingWindow() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new RateLimitProperties(10, null));

    assertEquals("ratelimit.window must be positive", exception.getMessage());
  }

  @Test
  void rejectsAZeroWindow() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new RateLimitProperties(10, Duration.ZERO));

    assertEquals("ratelimit.window must be positive", exception.getMessage());
  }

  @Test
  void rejectsANegativeWindow() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new RateLimitProperties(10, Duration.ofSeconds(-1)));

    assertEquals("ratelimit.window must be positive", exception.getMessage());
  }
}
