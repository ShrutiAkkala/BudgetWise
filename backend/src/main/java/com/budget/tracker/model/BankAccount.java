package com.budget.tracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String plaidItemId;

    @Column(nullable = false)
    private String plaidAccessToken;

    @Column(nullable = false)
    private String institutionName;

    private String accountName;
    private String accountMask;
    private String accountType;

    // Plaid cursor for transaction sync
    private String nextCursor;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime connectedAt = LocalDateTime.now();
}
