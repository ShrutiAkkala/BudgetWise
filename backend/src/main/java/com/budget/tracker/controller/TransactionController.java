package com.budget.tracker.controller;

import com.budget.tracker.dto.summary.DailySpending;
import com.budget.tracker.dto.summary.MonthlySummaryResponse;
import com.budget.tracker.dto.transaction.TransactionRequest;
import com.budget.tracker.dto.transaction.TransactionResponse;
import com.budget.tracker.model.User;
import com.budget.tracker.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.create(request, user));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAll(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(transactionService.getAll(user, year, month));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(transactionService.update(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        transactionService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/daily")
    public ResponseEntity<List<DailySpending>> getDailySpending(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal User user) {
        int y = (year != null) ? year : LocalDate.now().getYear();
        int m = (month != null) ? month : LocalDate.now().getMonthValue();
        return ResponseEntity.ok(transactionService.getDailySpending(user, y, m));
    }

    @GetMapping("/top-expenses")
    public ResponseEntity<List<com.budget.tracker.dto.summary.CategorySummary>> getTopExpenses(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(transactionService.getTopExpenses(user));
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<DailySpending>> getMonthlySpending(
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal User user) {
        int y = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(transactionService.getMonthlySpending(user, y));
    }

    @GetMapping("/summary")
    public ResponseEntity<MonthlySummaryResponse> getSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal User user) {
        int y = (year != null) ? year : LocalDate.now().getYear();
        int m = (month != null) ? month : LocalDate.now().getMonthValue();
        return ResponseEntity.ok(transactionService.getSummary(user, y, m));
    }
}
