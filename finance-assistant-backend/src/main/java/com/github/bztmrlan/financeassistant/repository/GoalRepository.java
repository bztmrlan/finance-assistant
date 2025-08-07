package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.model.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface GoalRepository extends JpaRepository<Goal, UUID> {
    List<Goal> findByUserId(UUID userId);

    @Query("SELECT g FROM Goal g " +
            "WHERE g.user.id = :userId " +
            "AND g.targetDate BETWEEN :start AND :end")
    List<Goal> findGoalsByTargetDateRange(
            @Param("userId") UUID userId,
            @Param("start") LocalDate startDate,
            @Param("end") LocalDate endDate
    );
}
