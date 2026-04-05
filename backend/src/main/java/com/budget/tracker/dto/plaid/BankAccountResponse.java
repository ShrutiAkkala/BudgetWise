package com.budget.tracker.dto.plaid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountResponse {
    private Long id;
    private String institutionName;
    private String accountName;
    private String accountMask;
    private String accountType;
    private LocalDateTime connectedAt;
}
