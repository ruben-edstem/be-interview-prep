package com.edstem.interviewprep.ratelimit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edstem.interviewprep.ratelimit.entity.RateLimitWindow;
import com.edstem.interviewprep.ratelimit.repository.RateLimitWindowRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class DatabaseRateLimitStoreRaceTest {

  private static final long NOW = 100_000L;
  private static final long WINDOW = 60_000L;
  private static final long CUTOFF = NOW - WINDOW;
  private static final int MAX = 10;

  @Mock private RateLimitWindowRepository repository;
  @InjectMocks private DatabaseRateLimitStore store;

  @Test
  void insertsTheFirstRequestOfAKey() {
    when(repository.increment(anyString(), anyLong(), anyInt())).thenReturn(0);
    when(repository.restart(anyString(), anyLong(), anyLong())).thenReturn(0);
    when(repository.findById(anyString())).thenReturn(Optional.empty());
    when(repository.insertFirst(anyString(), anyLong())).thenReturn(1);

    RateLimitDecision decision = store.record("client-a", NOW, MAX, WINDOW);

    assertTrue(decision.allowed());
    verify(repository).insertFirst(anyString(), anyLong());
  }

  @Test
  void retriesWhenAnotherInstanceInsertsTheFirstRowAtTheSameTime() {
    when(repository.increment(anyString(), anyLong(), anyInt())).thenReturn(0, 1);
    when(repository.restart(anyString(), anyLong(), anyLong())).thenReturn(0);
    when(repository.findById(anyString())).thenReturn(Optional.empty());
    when(repository.insertFirst(anyString(), anyLong()))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    RateLimitDecision decision = store.record("client-a", NOW, MAX, WINDOW);

    assertTrue(decision.allowed());
    verify(repository, times(2)).increment(anyString(), anyLong(), anyInt());
  }

  @Test
  void retriesWhenTheWindowEndedAfterTheFailedIncrement() {
    when(repository.increment(anyString(), anyLong(), anyInt())).thenReturn(0);
    when(repository.restart(anyString(), anyLong(), anyLong())).thenReturn(0, 1);
    when(repository.findById(anyString()))
        .thenReturn(Optional.of(new RateLimitWindow("key", CUTOFF, MAX)));

    RateLimitDecision decision = store.record("client-a", NOW, MAX, WINDOW);

    assertTrue(decision.allowed());
    verify(repository, times(2)).restart(anyString(), anyLong(), anyLong());
  }

  @Test
  void retriesWhenAnotherRequestJustStartedTheWindowAndItIsNotFull() {
    when(repository.increment(anyString(), anyLong(), anyInt())).thenReturn(0, 1);
    when(repository.restart(anyString(), anyLong(), anyLong())).thenReturn(0);
    when(repository.findById(anyString()))
        .thenReturn(Optional.of(new RateLimitWindow("key", NOW - 10L, 1)));

    RateLimitDecision decision = store.record("client-a", NOW, MAX, WINDOW);

    assertTrue(decision.allowed());
    verify(repository, times(2)).increment(anyString(), anyLong(), anyInt());
  }

  @Test
  void rejectsWhileTheStoredWindowIsRunningAndFull() {
    when(repository.increment(anyString(), anyLong(), anyInt())).thenReturn(0);
    when(repository.restart(anyString(), anyLong(), anyLong())).thenReturn(0);
    when(repository.findById(anyString()))
        .thenReturn(Optional.of(new RateLimitWindow("key", NOW - 1_000L, MAX)));

    RateLimitDecision decision = store.record("client-a", NOW, MAX, WINDOW);

    assertFalse(decision.allowed());
    assertEquals(NOW - 1_000L, decision.windowStartMillis());
  }

  @Test
  void givesUpAfterTheMaximumNumberOfAttempts() {
    when(repository.increment(anyString(), anyLong(), anyInt())).thenReturn(0);
    when(repository.restart(anyString(), anyLong(), anyLong())).thenReturn(0);
    when(repository.findById(anyString())).thenReturn(Optional.empty());
    when(repository.insertFirst(anyString(), anyLong()))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    assertThrows(IllegalStateException.class, () -> store.record("client-a", NOW, MAX, WINDOW));

    verify(repository, times(DatabaseRateLimitStore.MAX_ATTEMPTS))
        .insertFirst(anyString(), anyLong());
  }

  @Test
  void deleteExpiredPassesTheCutoffToTheRepository() {
    store.deleteExpired(5L);

    verify(repository).deleteExpired(5L);
  }
}
