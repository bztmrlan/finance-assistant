package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.model.Rule;
import lombok.Getter;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;

/**
 * Easy Rules implementation for finance rules
 * This class wraps our domain Rule entity and provides Easy Rules integration
 */
@org.jeasy.rules.annotation.Rule(name = "FinanceRule", description = "Evaluates financial spending rules")
public class FinanceRule {

    @Getter
    private final Rule rule;
    private final RuleEngineService ruleEngineService;

    public FinanceRule(Rule rule, RuleEngineService ruleEngineService) {
        this.rule = rule;
        this.ruleEngineService = ruleEngineService;
    }

    @Condition
    public boolean when() {
        return true;
    }

    @Action
    public void then() {
        ruleEngineService.evaluateRule(rule);
    }

    @Override
    public String toString() {
        return "FinanceRule{" +
                "ruleName='" + rule.getName() + '\'' +
                ", conditionType=" + rule.getConditionType() +
                ", threshold=" + rule.getThreshold() +
                '}';
    }
} 