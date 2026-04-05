package com.budget.tracker.service;

import com.budget.tracker.dto.summary.CategorySummary;
import com.budget.tracker.dto.summary.DailySpending;
import com.budget.tracker.dto.summary.MonthlySummaryResponse;
import com.budget.tracker.dto.transaction.TransactionRequest;
import com.budget.tracker.dto.transaction.TransactionResponse;
import com.budget.tracker.exception.ResourceNotFoundException;
import com.budget.tracker.model.Category;
import com.budget.tracker.model.Transaction;
import com.budget.tracker.model.TransactionType;
import com.budget.tracker.model.User;
import com.budget.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionResponse create(TransactionRequest request, User user) {
        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(request.getAmount())
                .description(request.getDescription())
                .category(request.getCategory())
                .type(request.getType())
                .date(request.getDate())
                .build();
        transaction = transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    public List<TransactionResponse> getAll(User user, Integer year, Integer month) {
        if (year != null && month != null) {
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            return transactionRepository
                    .findByUserIdAndDateBetweenOrderByDateDesc(user.getId(), start, end)
                    .stream().map(this::toResponse).collect(Collectors.toList());
        }
        return transactionRepository.findByUserIdOrderByDateDesc(user.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public TransactionResponse update(Long id, TransactionRequest request, User user) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Access denied");
        }
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setCategory(request.getCategory());
        transaction.setType(request.getType());
        transaction.setDate(request.getDate());
        return toResponse(transactionRepository.save(transaction));
    }

    public void delete(Long id, User user) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Access denied");
        }
        transactionRepository.delete(transaction);
    }

    public MonthlySummaryResponse getSummary(User user, int year, int month) {
        BigDecimal totalIncome = transactionRepository.sumByUserIdAndYearAndMonthAndType(
                user.getId(), year, month, TransactionType.INCOME);
        BigDecimal totalExpenses = transactionRepository.sumByUserIdAndYearAndMonthAndType(
                user.getId(), year, month, TransactionType.EXPENSE);
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        List<CategorySummary> expensesByCategory = transactionRepository
                .findCategoryTotals(user.getId(), year, month, TransactionType.EXPENSE)
                .stream()
                .map(row -> new CategorySummary((Category) row[0], (BigDecimal) row[1]))
                .collect(Collectors.toList());

        List<CategorySummary> incomeByCategory = transactionRepository
                .findCategoryTotals(user.getId(), year, month, TransactionType.INCOME)
                .stream()
                .map(row -> new CategorySummary((Category) row[0], (BigDecimal) row[1]))
                .collect(Collectors.toList());

        return MonthlySummaryResponse.builder()
                .year(year)
                .month(month)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(totalIncome.subtract(totalExpenses))
                .expensesByCategory(expensesByCategory)
                .incomeByCategory(incomeByCategory)
                .build();
    }

    public List<DailySpending> getDailySpending(User user, int year, int month) {
        return transactionRepository.findDailyExpenses(user.getId(), year, month)
                .stream()
                .map(row -> new DailySpending(((Number) row[0]).intValue(), (BigDecimal) row[1]))
                .collect(Collectors.toList());
    }

    public List<CategorySummary> getTopExpenses(User user) {
        return transactionRepository.findAllTimeCategoryTotals(user.getId())
                .stream()
                .limit(5)
                .map(row -> new CategorySummary((com.budget.tracker.model.Category) row[0], (BigDecimal) row[1]))
                .collect(Collectors.toList());
    }

    public List<DailySpending> getMonthlySpending(User user, int year) {
        return transactionRepository.findMonthlyExpenses(user.getId(), year)
                .stream()
                .map(row -> new DailySpending(((Number) row[0]).intValue(), (BigDecimal) row[1]))
                .collect(Collectors.toList());
    }

    private TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .amount(t.getAmount())
                .description(t.getDescription())
                .category(t.getCategory())
                .type(t.getType())
                .date(t.getDate())
                .plaidImported(t.getPlaidImported())
                .bankAccountName(t.getBankAccount() != null ? t.getBankAccount().getAccountName() : null)
                .build();
    }
}
