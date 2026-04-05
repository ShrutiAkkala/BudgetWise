package com.budget.tracker.dto.plaid;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SyncResponse {
    private int imported;
    private int skipped;
    private String message;
}
