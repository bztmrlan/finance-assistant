package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.enums.CategoryType;
import com.github.bztmrlan.financeassistant.enums.CondititonType;
import com.github.bztmrlan.financeassistant.enums.TimePeriod;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.model.Rule;
import com.github.bztmrlan.financeassistant.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
public class RuleRepositoryTest {

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user1;
    private User user2;
    private Category groceriesCategory;
    private Category diningCategory;
    private Rule rule1;
    private Rule rule2;
    private Rule rule3;

    @BeforeEach
    void setup() {

        user1 = new User();
        user1.setName("John Doe");
        user1.setEmail("john@example.com");
        user1.setPassword("password123");
        user1.setCreatedAt(Instant.now());
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setName("Jane Smith");
        user2.setEmail("jane@example.com");
        user2.setPassword("password456");
        user2.setCreatedAt(Instant.now());
        user2 = userRepository.save(user2);


        groceriesCategory = Category.builder()
                .name("Groceries")
                .type(CategoryType.EXPENSE)
                .user(user1)
                .build();
        groceriesCategory = categoryRepository.save(groceriesCategory);

        diningCategory = Category.builder()
                .name("Dining")
                .type(CategoryType.EXPENSE)
                .user(user1)
                .build();
        diningCategory = categoryRepository.save(diningCategory);


        rule1 = Rule.builder()
                .user(user1)
                .category(groceriesCategory)
                .name("Groceries Limit")
                .conditionType(CondititonType.GREATER_THAN)
                .threshold(new BigDecimal("500.00"))
                .period(TimePeriod.MONTHLY)
                .active(true)
                .build();

        rule2 = Rule.builder()
                .user(user1)
                .category(diningCategory)
                .name("Dining Limit")
                .conditionType(CondititonType.GREATER_THAN)
                .threshold(new BigDecimal("200.00"))
                .period(TimePeriod.WEEKLY)
                .active(true)
                .build();

        rule3 = Rule.builder()
                .user(user2)
                .category(groceriesCategory)
                .name("Jane's Groceries Limit")
                .conditionType(CondititonType.GREATER_THAN)
                .threshold(new BigDecimal("300.00"))
                .period(TimePeriod.MONTHLY)
                .active(false)
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve rule")
    void testSaveAndRetrieveRule() {
        Rule saved = ruleRepository.save(rule1);
        
        Optional<Rule> retrieved = ruleRepository.findById(saved.getId());
        
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Groceries Limit");
        assertThat(retrieved.get().getThreshold()).isEqualTo(new BigDecimal("500.00"));
        assertThat(retrieved.get().getPeriod()).isEqualTo(TimePeriod.MONTHLY);
        assertThat(retrieved.get().isActive()).isTrue();
        assertThat(retrieved.get().getUser().getId()).isEqualTo(user1.getId());
    }

    @Test
    @DisplayName("Should find rules by user")
    void testFindByUserId() {
        ruleRepository.save(rule1);
        ruleRepository.save(rule2);
        ruleRepository.save(rule3);

        List<Rule> user1Rules = ruleRepository.findByUserId(user1.getId());
        List<Rule> user2Rules = ruleRepository.findByUserId(user2.getId());

        assertThat(user1Rules).hasSize(2);
        assertThat(user2Rules).hasSize(1);
        
        assertThat(user1Rules).extracting("name")
                .containsExactlyInAnyOrder("Groceries Limit", "Dining Limit");
    }

    @Test
    @DisplayName("Should find active rules by period")
    void testFindActiveRulesByPeriod() {
        ruleRepository.save(rule1);
        ruleRepository.save(rule2);
        ruleRepository.save(rule3);

        List<Rule> monthlyActiveRules = ruleRepository.findActiveRulesByPeriod(user1.getId(), TimePeriod.MONTHLY);
        List<Rule> weeklyActiveRules = ruleRepository.findActiveRulesByPeriod(user1.getId(), TimePeriod.WEEKLY);

        assertThat(monthlyActiveRules).hasSize(1);
        assertThat(monthlyActiveRules.get(0).getName()).isEqualTo("Groceries Limit");
        
        assertThat(weeklyActiveRules).hasSize(1);
        assertThat(weeklyActiveRules.get(0).getName()).isEqualTo("Dining Limit");
    }

    @Test
    @DisplayName("Should not find inactive rules by period")
    void testFindActiveRulesByPeriodExcludesInactive() {
        ruleRepository.save(rule1);
        ruleRepository.save(rule3);

        List<Rule> monthlyActiveRules = ruleRepository.findActiveRulesByPeriod(user2.getId(), TimePeriod.MONTHLY);

        assertThat(monthlyActiveRules).isEmpty();
    }

    @Test
    @DisplayName("Should update rule")
    void testUpdateRule() {
        Rule saved = ruleRepository.save(rule1);
        
        saved.setThreshold(new BigDecimal("600.00"));
        saved.setActive(false);
        Rule updated = ruleRepository.save(saved);
        
        Optional<Rule> retrieved = ruleRepository.findById(updated.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getThreshold()).isEqualTo(new BigDecimal("600.00"));
        assertThat(retrieved.get().isActive()).isFalse();
    }

    @Test
    @DisplayName("Should delete rule")
    void testDeleteRule() {
        Rule saved = ruleRepository.save(rule1);
        
        ruleRepository.deleteById(saved.getId());
        
        Optional<Rule> retrieved = ruleRepository.findById(saved.getId());
        assertThat(retrieved).isEmpty();
    }

    @Test
    @DisplayName("Should find all rules")
    void testFindAllRules() {
        ruleRepository.save(rule1);
        ruleRepository.save(rule2);
        ruleRepository.save(rule3);

        List<Rule> allRules = ruleRepository.findAll();

        assertThat(allRules).hasSize(3);
        assertThat(allRules).extracting("name")
                .containsExactlyInAnyOrder("Groceries Limit", "Dining Limit", "Jane's Groceries Limit");
    }

    @Test
    @DisplayName("Should handle rules with different condition types")
    void testRulesWithDifferentConditionTypes() {
        Rule greaterThanRule = Rule.builder()
                .user(user1)
                .category(groceriesCategory)
                .name("Greater Than Rule")
                .conditionType(CondititonType.GREATER_THAN)
                .threshold(new BigDecimal("500.00"))
                .period(TimePeriod.MONTHLY)
                .active(true)
                .build();

        Rule lessThanRule = Rule.builder()
                .user(user1)
                .category(diningCategory)
                .name("Less Than Rule")
                .conditionType(CondititonType.LESS_THAN)
                .threshold(new BigDecimal("100.00"))
                .period(TimePeriod.WEEKLY)
                .active(true)
                .build();

        Rule equalToRule = Rule.builder()
                .user(user1)
                .category(groceriesCategory)
                .name("Equal To Rule")
                .conditionType(CondititonType.EQUAL_TO)
                .threshold(new BigDecimal("300.00"))
                .period(TimePeriod.DAILY)
                .active(true)
                .build();

        ruleRepository.save(greaterThanRule);
        ruleRepository.save(lessThanRule);
        ruleRepository.save(equalToRule);

        List<Rule> userRules = ruleRepository.findByUserId(user1.getId());

        assertThat(userRules).hasSize(3);
        assertThat(userRules).extracting("conditionType")
                .containsExactlyInAnyOrder(CondititonType.GREATER_THAN, CondititonType.LESS_THAN, CondititonType.EQUAL_TO);
    }

    @Test
    @DisplayName("Should handle rules with different time periods")
    void testRulesWithDifferentTimePeriods() {
        Rule dailyRule = Rule.builder()
                .user(user1)
                .category(groceriesCategory)
                .name("Daily Rule")
                .conditionType(CondititonType.GREATER_THAN)
                .threshold(new BigDecimal("50.00"))
                .period(TimePeriod.DAILY)
                .active(true)
                .build();

        Rule weeklyRule = Rule.builder()
                .user(user1)
                .category(diningCategory)
                .name("Weekly Rule")
                .conditionType(CondititonType.GREATER_THAN)
                .threshold(new BigDecimal("200.00"))
                .period(TimePeriod.WEEKLY)
                .active(true)
                .build();

        Rule monthlyRule = Rule.builder()
                .user(user1)
                .category(groceriesCategory)
                .name("Monthly Rule")
                .conditionType(CondititonType.GREATER_THAN)
                .threshold(new BigDecimal("500.00"))
                .period(TimePeriod.MONTHLY)
                .active(true)
                .build();

        Rule quarterlyRule = Rule.builder()
                .user(user1)
                .category(diningCategory)
                .name("Quarterly Rule")
                .conditionType(CondititonType.GREATER_THAN)
                .threshold(new BigDecimal("1000.00"))
                .period(TimePeriod.QUARTERLY)
                .active(true)
                .build();

        Rule yearlyRule = Rule.builder()
                .user(user1)
                .category(groceriesCategory)
                .name("Yearly Rule")
                .conditionType(CondititonType.GREATER_THAN)
                .threshold(new BigDecimal("5000.00"))
                .period(TimePeriod.YEARLY)
                .active(true)
                .build();

        ruleRepository.save(dailyRule);
        ruleRepository.save(weeklyRule);
        ruleRepository.save(monthlyRule);
        ruleRepository.save(quarterlyRule);
        ruleRepository.save(yearlyRule);

        List<Rule> userRules = ruleRepository.findByUserId(user1.getId());

        assertThat(userRules).hasSize(5);
        assertThat(userRules).extracting("period")
                .containsExactlyInAnyOrder(TimePeriod.DAILY, TimePeriod.WEEKLY, TimePeriod.MONTHLY, TimePeriod.QUARTERLY, TimePeriod.YEARLY);
    }

    @Test
    @DisplayName("Should handle rule without category")
    void testRuleWithoutCategory() {
        Rule ruleWithoutCategory = Rule.builder()
                .user(user1)
                .name("General Spending Rule")
                .conditionType(CondititonType.GREATER_THAN)
                .threshold(new BigDecimal("1000.00"))
                .period(TimePeriod.MONTHLY)
                .active(true)
                .build();

        Rule saved = ruleRepository.save(ruleWithoutCategory);
        
        Optional<Rule> retrieved = ruleRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCategory()).isNull();
    }

    @Test
    @DisplayName("Should handle empty repository")
    void testEmptyRepository() {
        List<Rule> userRules = ruleRepository.findByUserId(user1.getId());
        List<Rule> allRules = ruleRepository.findAll();

        assertThat(userRules).isEmpty();
        assertThat(allRules).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple active rules for same period")
    void testMultipleActiveRulesForSamePeriod() {
        Rule rule1 = Rule.builder()
                .user(user1)
                .category(groceriesCategory)
                .name("Rule 1")
                .conditionType(CondititonType.GREATER_THAN)
                .threshold(new BigDecimal("500.00"))
                .period(TimePeriod.MONTHLY)
                .active(true)
                .build();

        Rule rule2 = Rule.builder()
                .user(user1)
                .category(diningCategory)
                .name("Rule 2")
                .conditionType(CondititonType.GREATER_THAN)
                .threshold(new BigDecimal("200.00"))
                .period(TimePeriod.MONTHLY)
                .active(true)
                .build();

        ruleRepository.save(rule1);
        ruleRepository.save(rule2);

        List<Rule> monthlyActiveRules = ruleRepository.findActiveRulesByPeriod(user1.getId(), TimePeriod.MONTHLY);

        assertThat(monthlyActiveRules).hasSize(2);
        assertThat(monthlyActiveRules).extracting("name")
                .containsExactlyInAnyOrder("Rule 1", "Rule 2");
    }
} 