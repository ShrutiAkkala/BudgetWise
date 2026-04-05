package com.budget.tracker.controller;

import com.budget.tracker.dto.plaid.*;
import com.budget.tracker.model.User;
import com.budget.tracker.service.PlaidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/plaid")
@RequiredArgsConstructor
public class PlaidController {

    private final PlaidService plaidService;

    @PostMapping("/link-token")
    public ResponseEntity<LinkTokenResponse> createLinkToken(@AuthenticationPrincipal User user) throws IOException {
        return ResponseEntity.ok(plaidService.createLinkToken(user));
    }

    @PostMapping("/exchange-token")
    public ResponseEntity<BankAccountResponse> exchangeToken(
            @RequestBody ExchangeTokenRequest request,
            @AuthenticationPrincipal User user) throws IOException {
        return ResponseEntity.ok(plaidService.exchangePublicToken(request, user));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<BankAccountResponse>> getAccounts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(plaidService.getConnectedAccounts(user));
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Void> disconnectAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) throws IOException {
        plaidService.disconnectAccount(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sync")
    public ResponseEntity<SyncResponse> syncTransactions(@AuthenticationPrincipal User user) throws IOException {
        return ResponseEntity.ok(plaidService.syncAll(user));
    }
}
