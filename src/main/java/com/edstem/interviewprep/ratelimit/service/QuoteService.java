package com.edstem.interviewprep.ratelimit.service;

import com.edstem.interviewprep.ratelimit.dto.response.QuoteResponse;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class QuoteService {

  private static final List<QuoteResponse> QUOTES =
      List.of(
          new QuoteResponse("Simplicity is prerequisite for reliability.", "Edsger W. Dijkstra"),
          new QuoteResponse("Premature optimization is the root of all evil.", "Donald Knuth"),
          new QuoteResponse("Make it work, make it right, make it fast.", "Kent Beck"),
          new QuoteResponse("Talk is cheap. Show me the code.", "Linus Torvalds"),
          new QuoteResponse(
              "Programs must be written for people to read, and only incidentally for machines"
                  + " to execute.",
              "Harold Abelson"));

  public QuoteResponse getRandomQuote() {
    return QUOTES.get(ThreadLocalRandom.current().nextInt(QUOTES.size()));
  }
}
