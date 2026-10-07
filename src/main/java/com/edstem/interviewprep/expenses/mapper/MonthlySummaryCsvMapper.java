package com.edstem.interviewprep.expenses.mapper;

import com.edstem.interviewprep.expenses.dto.response.MonthlySummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class MonthlySummaryCsvMapper {

  private static final String LINE_END = "\r\n";

  public String toCsv(MonthlySummaryResponse summary) {
    StringBuilder csv = new StringBuilder("category,total").append(LINE_END);
    summary
        .totals()
        .forEach(
            (category, total) ->
                csv.append(category.name())
                    .append(',')
                    .append(total.toPlainString())
                    .append(LINE_END));
    csv.append("TOTAL,").append(summary.total().toPlainString()).append(LINE_END);
    return csv.toString();
  }
}
