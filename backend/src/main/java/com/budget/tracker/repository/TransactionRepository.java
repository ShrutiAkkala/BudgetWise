package com.budget.tracker.repository;

import com.budget.tracker.model.Category;
import com.budget.tracker.model.Transaction;
import com.budget.tracker.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdAndDateBetweenOrderByDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserIdOrderByDateDesc(Long userId);

    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t " +
           "WHERE t.user.id = :userId AND YEAR(t.date) = :year AND MONTH(t.date) = :month " +
           "AND t.type = :type GROUP BY t.category")
    List<Object[]> findCategoryTotals(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("type") TransactionType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND YEAR(t.date) = :year AND MONTH(t.date) = :month " +
           "AND t.type = :type")
    BigDecimal sumByUserIdAndYearAndMonthAndType(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("type") TransactionType type);

    @Query("SELECT DAY(t.date), SUM(t.amount) FROM Transaction t " +
           "WHERE t.user.id = :userId AND YEAR(t.date) = :year AND MONTH(t.date) = :month " +
           "AND t.type = 'EXPENSE' GROUP BY DAY(t.date) ORDER BY DAY(t.date)")
    List<Object[]> findDailyExpenses(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);

    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = 'EXPENSE' " +
           "GROUP BY t.category ORDER BY SUM(t.amount) DESC")
    List<Object[]> findAllTimeCategoryTotals(@Param("userId") Long userId);

    @Query("SELECT MONTH(t.date), SUM(t.amount) FROM Transaction t " +
           "WHERE t.user.id = :userId AND YEAR(t.date) = :year " +
           "AND t.type = 'EXPENSE' GROUP BY MONTH(t.date) ORDER BY MONTH(t.date)")
    List<Object[]> findMonthlyExpenses(@Param("userId") Long userId, @Param("year") int year);

    boolean existsByPlaidTransactionId(String plaidTransactionId);
}
