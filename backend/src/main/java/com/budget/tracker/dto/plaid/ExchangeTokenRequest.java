package com.budget.tracker.dto.plaid;
import lombok.Data;

@Data
public class ExchangeTokenRequest {
    private String publicToken;
    private String institutionName;
    private String accountName;
    private String accountMask;
    private String accountType;
}
