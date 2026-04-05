package com.budget.tracker.service;

import com.budget.tracker.dto.summary.DailySpending;
import com.budget.tracker.dto.summary.MonthlySummaryResponse;
import com.budget.tracker.dto.transaction.TransactionRequest;
import com.budget.tracker.dto.transaction.TransactionResponse;
import com.budget.tracker.exception.ResourceNotFoundException;
import com.budget.tracker.model.*;
import com.budget.tracker.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private TransactionService transactionService;

    private User buildUser(Long id, String username) {
        return User.builder().id(id).username(username).email(username + "@test.com")
                .password("enc").role(Role.USER).build();
    }

    private Transaction buildTransaction(Long id, User user) {
        return Transaction.builder()
                .id(id).user(user)
                .amount(new BigDecimal("50.00"))
                .description("Lunch")
                .category(Category.FOOD)
                .type(TransactionType.EXPENSE)
                .date(LocalDate.of(2026, 3, 10))
                .build();
    }

    private TransactionRequest buildRequest() {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("50.00"));
        req.setDescription("Lunch");
        req.setCategory(Category.FOOD);
        req.setType(TransactionType.EXPENSE);
        req.setDate(LocalDate.of(2026, 3, 10));
        return req;
    }

    @Test
    void create_savesAndReturnsTransactionResponse() {
        User user = buildUser(1L, "alice");
        Transaction saved = buildTransaction(1L, user);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        TransactionResponse response = transactionService.create(buildRequest(), user);

        verify(transactionRepository).save(any(Transaction.class));
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getDescription()).isEqualTo("Lunch");
        assertThat(response.getCategory()).isEqualTo(Category.FOOD);
    }

    @Test
    void getAll_withMonthFilter_callsDateBetweenQuery() {
        User user = buildUser(1L, "alice");
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(eq(1L), any(), any()))
                .thenReturn(List.of(buildTransaction(1L, user)));

        List<TransactionResponse> result = transactionService.getAll(user, 2026, 3);

        assertThat(result).hasSize(1);
        verify(transactionRepository).findByUserIdAndDateBetweenOrderByDateDesc(eq(1L), any(), any());
        verify(transactionRepository, never()).findByUserIdOrderByDateDesc(any());
    }

    @Test
    void getAll_withoutFilter_callsFullListQuery() {
        User user = buildUser(1L, "alice");
        when(transactionRepository.findByUserIdOrderByDateDesc(1L))
                .thenReturn(List.of(buildTransaction(1L, user)));

        List<TransactionResponse> result = transactionService.getAll(user, null, null);

        assertThat(result).hasSize(1);
        verify(transactionRepository).findByUserIdOrderByDateDesc(1L);
    }

    @Test
    void update_withOwner_updatesAndReturnsResponse() {
        User user = buildUser(1L, "alice");
        Transaction existing = buildTransaction(10L, user);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(any())).thenReturn(existing);

        TransactionRequest req = buildRequest();
        req.setDescription("Updated Lunch");
        TransactionResponse response = transactionService.update(10L, req, user);

        assertThat(response).isNotNull();
        verify(transactionRepository).save(any());
    }

    @Test
    void update_withDifferentUser_throwsSecurityException() {
        User owner = buildUser(1L, "alice");
        User other = buildUser(2L, "bob");
        Transaction existing = buildTransaction(10L, owner);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> transactionService.update(10L, buildRequest(), other))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void delete_withOwner_deletesTransaction() {
        User user = buildUser(1L, "alice");
        Transaction existing = buildTransaction(10L, user);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(existing));

        transactionService.delete(10L, user);

        verify(transactionRepository).delete(existing);
    }

    @Test
    void delete_withDifferentUser_throwsSecurityException() {
        User owner = buildUser(1L, "alice");
        User other = buildUser(2L, "bob");
        Transaction existing = buildTransaction(10L, owner);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> transactionService.delete(10L, other))
                .isInstanceOf(SecurityException.class);

        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void delete_withNonExistentId_throwsResourceNotFoundException() {
        User user = buildUser(1L, "alice");
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(99L, user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSummary_returnsCorrectTotals() {
        User user = buildUser(1L, "alice");
        when(transactionRepository.sumByUserIdAndYearAndMonthAndType(1L, 2026, 3, TransactionType.INCOME))
                .thenReturn(new BigDecimal("3000.00"));
        when(transactionRepository.sumByUserIdAndYearAndMonthAndType(1L, 2026, 3, TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("1200.00"));
        when(transactionRepository.findCategoryTotals(eq(1L), eq(2026), eq(3), eq(TransactionType.EXPENSE)))
                .thenReturn(List.of(new Object[]{Category.FOOD, new BigDecimal("500.00")}));
        when(transactionRepository.findCategoryTotals(eq(1L), eq(2026), eq(3), eq(TransactionType.INCOME)))
                .thenReturn(List.of());

        MonthlySummaryResponse summary = transactionService.getSummary(user, 2026, 3);

        assertThat(summary.getTotalIncome()).isEqualByComparingTo("3000.00");
        assertThat(summary.getTotalExpenses()).isEqualByComparingTo("1200.00");
        assertThat(summary.getNetBalance()).isEqualByComparingTo("1800.00");
        assertThat(summary.getExpensesByCategory()).hasSize(1);
        assertThat(summary.getExpensesByCategory().get(0).getCategory()).isEqualTo(Category.FOOD);
    }

    @Test
    void getSummary_withNullTotals_returnsZero() {
        User user = buildUser(1L, "alice");
        when(transactionRepository.sumByUserIdAndYearAndMonthAndType(any(), anyInt(), anyInt(), any()))
                .thenReturn(null);
        when(transactionRepository.findCategoryTotals(any(), anyInt(), anyInt(), any()))
                .thenReturn(List.of());

        MonthlySummaryResponse summary = transactionService.getSummary(user, 2026, 3);

        assertThat(summary.getTotalIncome()).isEqualByComparingTo("0");
        assertThat(summary.getTotalExpenses()).isEqualByComparingTo("0");
        assertThat(summary.getNetBalance()).isEqualByComparingTo("0");
    }

    @Test
    void getDailySpending_returnsMappedList() {
        User user = buildUser(1L, "alice");
        when(transactionRepository.findDailyExpenses(1L, 2026, 3))
                .thenReturn(List.of(
                        new Object[]{5, new BigDecimal("120.00")},
                        new Object[]{15, new BigDecimal("80.00")}
                ));

        List<DailySpending> result = transactionService.getDailySpending(user, 2026, 3);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDay()).isEqualTo(5);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("120.00");
        assertThat(result.get(1).getDay()).isEqualTo(15);
    }
}
