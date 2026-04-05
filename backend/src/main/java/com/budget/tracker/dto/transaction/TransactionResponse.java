package com.budget.tracker.dto.transaction;

import com.budget.tracker.model.Category;
import com.budget.tracker.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private BigDecimal amount;
    private String description;
    private Category category;
    private TransactionType type;
    private LocalDate date;
    private Boolean plaidImported;
    private String bankAccountName;
}
