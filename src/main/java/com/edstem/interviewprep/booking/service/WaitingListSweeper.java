package com.edstem.interviewprep.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingListSweeper {

  private final WaitingListService waitingListService;

  @Scheduled(
      initialDelayString = "${booking.sweep-interval}",
      fixedDelayString = "${booking.sweep-interval}")
  public void advanceQueues() {
    waitingListService.removeExpiredOffers();
    for (Long slotId : waitingListService.findSlotsReadyToOffer()) {
      try {
        waitingListService.offerNext(slotId);
      } catch (RuntimeException ex) {
        log.error("Failed to offer slot {} to the waiting list", slotId, ex);
      }
    }
  }
}
