package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findByUserId(UUID userId);

    @Query("SELECT bc.category, SUM(bc.spentAmount) FROM Budget b " +
            "JOIN b.categories bc WHERE b.id = :budgetId GROUP BY bc.category")
    List<Object[]> getCategorySpendingForBudget(@Param("budgetId") UUID budgetId);
}
