package com.budget.tracker.dto.transaction;

import com.budget.tracker.model.Category;
import com.budget.tracker.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotBlank
    private String description;

    @NotNull
    private Category category;

    @NotNull
    private TransactionType type;

    @NotNull
    private LocalDate date;
}
