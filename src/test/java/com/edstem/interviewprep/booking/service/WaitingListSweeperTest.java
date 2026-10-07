package com.edstem.interviewprep.booking.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WaitingListSweeperTest {

  @Mock private WaitingListService waitingListService;
  @InjectMocks private WaitingListSweeper sweeper;

  @Test
  void removesExpiredOffersBeforeOfferingEachReadySlot() {
    when(waitingListService.findSlotsReadyToOffer()).thenReturn(List.of(10L, 11L));

    sweeper.advanceQueues();

    InOrder order = inOrder(waitingListService);
    order.verify(waitingListService).removeExpiredOffers();
    order.verify(waitingListService).findSlotsReadyToOffer();
    order.verify(waitingListService).offerNext(10L);
    order.verify(waitingListService).offerNext(11L);
  }

  @Test
  void doesNotOfferAnythingWhenNoSlotIsReady() {
    when(waitingListService.findSlotsReadyToOffer()).thenReturn(List.of());

    sweeper.advanceQueues();

    verify(waitingListService, never()).offerNext(10L);
  }

  @Test
  void aSlotThatFailsDoesNotStopTheOthers() {
    when(waitingListService.findSlotsReadyToOffer()).thenReturn(List.of(10L, 11L));
    doThrow(new IllegalStateException("boom")).when(waitingListService).offerNext(10L);

    sweeper.advanceQueues();

    verify(waitingListService).offerNext(11L);
  }
}
