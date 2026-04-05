package com.budget.tracker.dto.summary;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DailySpending {
    private int day;
    private BigDecimal amount;
}
