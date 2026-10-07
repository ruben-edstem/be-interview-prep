package com.edstem.interviewprep.ratelimit.repository;

import com.edstem.interviewprep.ratelimit.entity.RateLimitWindow;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RateLimitWindowRepository extends JpaRepository<RateLimitWindow, String> {

  @Transactional
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update RateLimitWindow w set w.requestCount = w.requestCount + 1
      where w.apiKeyHash = :key and w.windowStartMillis > :cutoff and w.requestCount < :max
      """)
  int increment(@Param("key") String key, @Param("cutoff") long cutoff, @Param("max") int max);

  @Transactional
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update RateLimitWindow w set w.windowStartMillis = :now, w.requestCount = 1
      where w.apiKeyHash = :key and w.windowStartMillis <= :cutoff
      """)
  int restart(@Param("key") String key, @Param("now") long now, @Param("cutoff") long cutoff);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          insert into rate_limit_windows (api_key_hash, window_start_millis, request_count)
          values (:key, :now, 1)
          """,
      nativeQuery = true)
  int insertFirst(@Param("key") String key, @Param("now") long now);

  @Query("select w.windowStartMillis from RateLimitWindow w where w.apiKeyHash = :key")
  Optional<Long> findWindowStart(@Param("key") String key);

  @Transactional
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from RateLimitWindow w where w.windowStartMillis <= :cutoff")
  int deleteExpired(@Param("cutoff") long cutoff);
}
