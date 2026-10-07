package com.edstem.interviewprep.booking.controller;

import com.edstem.interviewprep.booking.dto.request.JoinWaitingListRequest;
import com.edstem.interviewprep.booking.dto.response.WaitingEntryResponse;
import com.edstem.interviewprep.booking.service.WaitingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WaitingListController {

  private final WaitingListService waitingListService;

  @PostMapping("/slots/{slotId}/waiting-list")
  @ResponseStatus(HttpStatus.CREATED)
  public WaitingEntryResponse join(
      @PathVariable Long slotId, @Valid @RequestBody JoinWaitingListRequest request) {
    return waitingListService.join(slotId, request);
  }

  @DeleteMapping("/waiting-list/{entryId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void leave(@PathVariable Long entryId) {
    waitingListService.leave(entryId);
  }
}
