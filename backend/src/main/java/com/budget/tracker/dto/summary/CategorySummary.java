package com.budget.tracker.dto.summary;

import com.budget.tracker.model.Category;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CategorySummary {
    private Category category;
    private BigDecimal total;
}
