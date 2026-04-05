package com.budget.tracker.repository;

import com.budget.tracker.model.Category;
import com.budget.tracker.model.Role;
import com.budget.tracker.model.Transaction;
import com.budget.tracker.model.TransactionType;
import com.budget.tracker.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA slice tests for TransactionRepository using @DataJpaTest.
 *
 * Notes on JPQL functions:
 *   - The repository uses YEAR(), MONTH(), DAY() in its @Query methods. These are standard
 *     Hibernate HQL date functions that translate to H2-compatible SQL, so @DataJpaTest (which
 *     auto-configures an in-memory H2 database) runs all queries correctly without extra setup.
 *   - DDL is auto-generated from entity annotations (create-drop) by Spring Boot's test slice.
 *   - Each test starts with a clean database state established by @BeforeEach.
 */
@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        userRepository.deleteAll();

        alice = userRepository.save(User.builder()
                .username("alice")
                .email("alice@test.com")
                .password("encoded")
                .role(Role.USER)
                .build());

        bob = userRepository.save(User.builder()
                .username("bob")
                .email("bob@test.com")
                .password("encoded")
                .role(Role.USER)
                .build());

        // Alice — March 2026 transactions
        // Day 5: FOOD 50.00 + TRANSPORT 30.00 = 80.00 total expenses
        transactionRepository.save(Transaction.builder()
                .user(alice)
                .amount(new BigDecimal("50.00"))
                .description("Lunch")
                .category(Category.FOOD)
                .type(TransactionType.EXPENSE)
                .date(LocalDate.of(2026, 3, 5))
                .build());

        transactionRepository.save(Transaction.builder()
                .user(alice)
                .amount(new BigDecimal("30.00"))
                .description("Bus pass")
                .category(Category.TRANSPORT)
                .type(TransactionType.EXPENSE)
                .date(LocalDate.of(2026, 3, 5))
                .build());

        // Day 15: FOOD 100.00
        transactionRepository.save(Transaction.builder()
                .user(alice)
                .amount(new BigDecimal("100.00"))
                .description("Groceries")
                .category(Category.FOOD)
                .type(TransactionType.EXPENSE)
                .date(LocalDate.of(2026, 3, 15))
                .build());

        // Day 1: INCOME 3000.00 (should NOT appear in expense queries)
        transactionRepository.save(Transaction.builder()
                .user(alice)
                .amount(new BigDecimal("3000.00"))
                .description("Salary")
                .category(Category.OTHER)
                .type(TransactionType.INCOME)
                .date(LocalDate.of(2026, 3, 1))
                .build());

        // Bob — should NOT appear in Alice's results
        transactionRepository.save(Transaction.builder()
                .user(bob)
                .amount(new BigDecimal("200.00"))
                .description("Bob's groceries")
                .category(Category.FOOD)
                .type(TransactionType.EXPENSE)
                .date(LocalDate.of(2026, 3, 10))
                .build());
    }

    // -------------------------------------------------------------------------
    // findByUserIdAndDateBetweenOrderByDateDesc
    // -------------------------------------------------------------------------

    @Test
    void findByUserIdAndDateBetween_returnsOnlyAlicesTransactions() {
        List<Transaction> result = transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                alice.getId(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        );

        // Alice has 4 transactions in March (3 expenses + 1 income); Bob's transaction excluded.
        assertThat(result).hasSize(4);
        assertThat(result).extracting(t -> t.getUser().getId())
                .containsOnly(alice.getId());
    }

    @Test
    void findByUserIdAndDateBetween_respectsDateBoundaries() {
        // Only ask for days 5–10; should return 3 transactions (day 5 ×2, no day 10 for alice)
        List<Transaction> result = transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                alice.getId(),
                LocalDate.of(2026, 3, 5),
                LocalDate.of(2026, 3, 10)
        );

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Transaction::getDate)
                .allMatch(d -> !d.isBefore(LocalDate.of(2026, 3, 5))
                        && !d.isAfter(LocalDate.of(2026, 3, 10)));
    }

    @Test
    void findByUserIdAndDateBetween_returnsOrderedByDateDescending() {
        List<Transaction> result = transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                alice.getId(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        );

        // Dates in descending order: 15, 5, 5, 1
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getDate())
                    .isAfterOrEqualTo(result.get(i + 1).getDate());
        }
    }

    @Test
    void findByUserIdAndDateBetween_emptyResultForDifferentMonth() {
        List<Transaction> result = transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                alice.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // findDailyExpenses
    // -------------------------------------------------------------------------

    @Test
    void findDailyExpenses_groupsByDayAndSumsAmounts() {
        List<Object[]> result = transactionRepository.findDailyExpenses(alice.getId(), 2026, 3);

        // Expenses only: Day 5 (50+30=80), Day 15 (100). Income on Day 1 excluded.
        assertThat(result).hasSize(2);

        // Results are ordered by DAY(t.date) ascending per the JPQL ORDER BY clause
        assertThat(((Number) result.get(0)[0]).intValue()).isEqualTo(5);
        assertThat((BigDecimal) result.get(0)[1]).isEqualByComparingTo("80.00");

        assertThat(((Number) result.get(1)[0]).intValue()).isEqualTo(15);
        assertThat((BigDecimal) result.get(1)[1]).isEqualByComparingTo("100.00");
    }

    @Test
    void findDailyExpenses_excludesOtherUsersData() {
        // Bob's expense (Day 10, 200.00) must not appear in Alice's daily spending
        List<Object[]> result = transactionRepository.findDailyExpenses(alice.getId(), 2026, 3);

        boolean hasBobsDay = result.stream()
                .anyMatch(row -> ((Number) row[0]).intValue() == 10);
        assertThat(hasBobsDay).isFalse();
    }

    @Test
    void findDailyExpenses_returnsEmptyForMonthWithNoExpenses() {
        List<Object[]> result = transactionRepository.findDailyExpenses(alice.getId(), 2026, 4);
        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // sumByUserIdAndYearAndMonthAndType
    // -------------------------------------------------------------------------

    @Test
    void sumByUserIdAndYearAndMonthAndType_sumsExpensesCorrectly() {
        BigDecimal total = transactionRepository.sumByUserIdAndYearAndMonthAndType(
                alice.getId(), 2026, 3, TransactionType.EXPENSE);

        // 50.00 + 30.00 + 100.00 = 180.00
        assertThat(total).isEqualByComparingTo("180.00");
    }

    @Test
    void sumByUserIdAndYearAndMonthAndType_sumsIncomeCorrectly() {
        BigDecimal total = transactionRepository.sumByUserIdAndYearAndMonthAndType(
                alice.getId(), 2026, 3, TransactionType.INCOME);

        assertThat(total).isEqualByComparingTo("3000.00");
    }

    @Test
    void sumByUserIdAndYearAndMonthAndType_returnsZeroWhenNoTransactions() {
        BigDecimal total = transactionRepository.sumByUserIdAndYearAndMonthAndType(
                alice.getId(), 2026, 4, TransactionType.EXPENSE);

        // COALESCE(..., 0) ensures no null is returned
        assertThat(total).isEqualByComparingTo("0");
    }

    @Test
    void sumByUserIdAndYearAndMonthAndType_excludesOtherUsersAmounts() {
        // Bob also has a FOOD EXPENSE in March; Alice's sum must not include it
        BigDecimal total = transactionRepository.sumByUserIdAndYearAndMonthAndType(
                alice.getId(), 2026, 3, TransactionType.EXPENSE);

        assertThat(total).isEqualByComparingTo("180.00"); // not 380.00
    }

    // -------------------------------------------------------------------------
    // findCategoryTotals
    // -------------------------------------------------------------------------

    @Test
    void findCategoryTotals_groupsByCategoryForExpenses() {
        List<Object[]> result = transactionRepository.findCategoryTotals(
                alice.getId(), 2026, 3, TransactionType.EXPENSE);

        // FOOD: 50 + 100 = 150, TRANSPORT: 30
        assertThat(result).hasSize(2);

        boolean foodFound = result.stream().anyMatch(row ->
                row[0] == Category.FOOD
                        && ((BigDecimal) row[1]).compareTo(new BigDecimal("150.00")) == 0);
        assertThat(foodFound).as("FOOD total should be 150.00").isTrue();

        boolean transportFound = result.stream().anyMatch(row ->
                row[0] == Category.TRANSPORT
                        && ((BigDecimal) row[1]).compareTo(new BigDecimal("30.00")) == 0);
        assertThat(transportFound).as("TRANSPORT total should be 30.00").isTrue();
    }

    @Test
    void findCategoryTotals_groupsByCategoryForIncome() {
        List<Object[]> result = transactionRepository.findCategoryTotals(
                alice.getId(), 2026, 3, TransactionType.INCOME);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)[0]).isEqualTo(Category.OTHER);
        assertThat((BigDecimal) result.get(0)[1]).isEqualByComparingTo("3000.00");
    }

    @Test
    void findCategoryTotals_excludesOtherUserCategories() {
        // Bob's FOOD EXPENSE should not inflate Alice's FOOD total
        List<Object[]> result = transactionRepository.findCategoryTotals(
                alice.getId(), 2026, 3, TransactionType.EXPENSE);

        BigDecimal aliceFoodTotal = result.stream()
                .filter(row -> row[0] == Category.FOOD)
                .map(row -> (BigDecimal) row[1])
                .findFirst()
                .orElse(BigDecimal.ZERO);

        assertThat(aliceFoodTotal).isEqualByComparingTo("150.00"); // not 350.00
    }

    @Test
    void findCategoryTotals_returnsEmptyForMonthWithNoMatchingType() {
        List<Object[]> result = transactionRepository.findCategoryTotals(
                alice.getId(), 2026, 4, TransactionType.EXPENSE);

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // existsByPlaidTransactionId
    // -------------------------------------------------------------------------

    @Test
    void existsByPlaidTransactionId_returnsTrueWhenExists() {
        transactionRepository.save(Transaction.builder()
                .user(alice)
                .amount(new BigDecimal("25.00"))
                .description("Coffee")
                .category(Category.FOOD)
                .type(TransactionType.EXPENSE)
                .date(LocalDate.of(2026, 3, 20))
                .plaidTransactionId("plaid-tx-abc123")
                .build());

        assertThat(transactionRepository.existsByPlaidTransactionId("plaid-tx-abc123")).isTrue();
    }

    @Test
    void existsByPlaidTransactionId_returnsFalseForUnknownId() {
        assertThat(transactionRepository.existsByPlaidTransactionId("plaid-tx-unknown")).isFalse();
    }

    @Test
    void existsByPlaidTransactionId_returnsFalseForNullArgument() {
        // Transactions saved in setUp have null plaidTransactionId; passing null should not match them
        assertThat(transactionRepository.existsByPlaidTransactionId(null)).isFalse();
    }

    @Test
    void existsByPlaidTransactionId_doesNotMatchDifferentId() {
        transactionRepository.save(Transaction.builder()
                .user(alice)
                .amount(new BigDecimal("25.00"))
                .description("Coffee")
                .category(Category.FOOD)
                .type(TransactionType.EXPENSE)
                .date(LocalDate.of(2026, 3, 20))
                .plaidTransactionId("plaid-tx-xyz999")
                .build());

        assertThat(transactionRepository.existsByPlaidTransactionId("plaid-tx-abc123")).isFalse();
        assertThat(transactionRepository.existsByPlaidTransactionId("plaid-tx-xyz999")).isTrue();
    }
}
