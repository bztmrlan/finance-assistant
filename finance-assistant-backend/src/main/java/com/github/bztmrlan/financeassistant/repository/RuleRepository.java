package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.enums.TimePeriod;
import com.github.bztmrlan.financeassistant.model.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RuleRepository extends JpaRepository<Rule, UUID> {
    List<Rule> findByUserId(UUID userId);

    @Query("SELECT r FROM Rule r " +
            "WHERE r.user.id = :userId " +
            "AND r.active = true " +
            "AND r.period = :period")
    List<Rule> findActiveRulesByPeriod(
            @Param("userId") UUID userId,
            @Param("period") TimePeriod period
    );

    List<Rule> findByUserIdAndActiveTrue(UUID userId);


}
