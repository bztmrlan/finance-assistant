package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.model.Budget;
import com.github.bztmrlan.financeassistant.model.BudgetCategory;
import com.github.bztmrlan.financeassistant.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, UUID> {
    @Query("SELECT bc FROM BudgetCategory bc " +
            "WHERE bc.budget.id = :budgetId AND bc.spentAmount > bc.limitAmount")
    List<BudgetCategory> findExceededCategories(@Param("budgetId") UUID budgetId);

    boolean existsByBudgetAndCategory(Budget budget, Category category);
    
    Optional<BudgetCategory> findByBudgetAndCategory(Budget budget, Category category);
    
    List<BudgetCategory> findByBudget(Budget budget);
    
    void deleteByBudget(Budget budget);

    @Modifying
    @Query("DELETE FROM BudgetCategory bc " +
            "WHERE bc.id = :id")
    void deleteById(@NonNull @Param("id") UUID id);
}
