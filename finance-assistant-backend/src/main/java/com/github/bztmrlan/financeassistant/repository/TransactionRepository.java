package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByUserId(UUID userId);
    List<Transaction> findByUserIdAndDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t " +
            "WHERE t.user.id = :userId AND t.date BETWEEN :start AND :end " +
            "GROUP BY t.category")
    List<Object[]> getSpendingByCategory(
            @Param("userId") UUID userId,
            @Param("start") LocalDate startDate,
            @Param("end") LocalDate endDate
    );

    List<Transaction> findByUserIdAndCategoryIdAndDateBetween(
            UUID userId, 
            UUID categoryId, 
            LocalDate startDate, 
            LocalDate endDate
    );

    List<Transaction> findByUserIdAndDateAndAmountAndDescription(
            UUID userId, LocalDate date, BigDecimal amount, String description);
}
