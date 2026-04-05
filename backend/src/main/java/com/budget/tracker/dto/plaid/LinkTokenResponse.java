package com.budget.tracker.dto.plaid;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LinkTokenResponse {
    private String linkToken;
}
