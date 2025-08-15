package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.enums.CondititonType;
import com.github.bztmrlan.financeassistant.enums.TimePeriod;
import com.github.bztmrlan.financeassistant.model.Alert;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.model.Rule;
import com.github.bztmrlan.financeassistant.model.Transaction;
import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.repository.AlertRepository;
import com.github.bztmrlan.financeassistant.repository.RuleRepository;
import com.github.bztmrlan.financeassistant.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private RuleEngineService ruleEngineService;

    private User testUser;
    private Category testCategory;
    private Rule testRule;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("testuser")
                .email("test@example.com")
                .build();

        testCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Groceries")
                .user(testUser)
                .build();

        testRule = Rule.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .category(testCategory)
                .name("Monthly Grocery Budget")
                .conditionType(CondititonType.GREATER_THAN)
                .threshold(new BigDecimal("500.00"))
                .active(true)
                .period(TimePeriod.MONTHLY)
                .build();

        testTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .category(testCategory)
                .date(LocalDate.now())
                .amount(new BigDecimal("600.00"))
                .currency("USD")
                .description("Grocery shopping")
                .build();
    }

    @Test
    void testEvaluateRulesForUser_NoViolation() {

        List<Rule> rules = Arrays.asList(testRule);
        List<Transaction> transactions = Arrays.asList(
                Transaction.builder()
                        .amount(new BigDecimal("300.00"))
                        .build()
        );

        when(ruleRepository.findByUserIdAndActiveTrue(testUser.getId())).thenReturn(rules);
        when(transactionRepository.findByUserIdAndCategoryIdAndDateBetween(
                any(), any(), any(), any()
        )).thenReturn(transactions);


        ruleEngineService.evaluateRulesForUser(testUser.getId());


        verify(alertRepository, never()).save(any());
    }

    @Test
    void testEvaluateRulesForUser_WithViolation() {

        List<Rule> rules = Arrays.asList(testRule);
        List<Transaction> transactions = Arrays.asList(
                Transaction.builder()
                        .amount(new BigDecimal("600.00"))
                        .build()
        );

        when(ruleRepository.findByUserIdAndActiveTrue(testUser.getId())).thenReturn(rules);
        when(transactionRepository.findByUserIdAndCategoryIdAndDateBetween(
                any(), any(), any(), any()
        )).thenReturn(transactions);
        when(alertRepository.save(any(Alert.class))).thenReturn(new Alert());


        ruleEngineService.evaluateRulesForUser(testUser.getId());


        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void testEvaluateRulesForTransaction() {

        List<Rule> rules = Arrays.asList(testRule);
        
        when(ruleRepository.findByUserIdAndActiveTrue(testUser.getId())).thenReturn(rules);
        when(alertRepository.save(any(Alert.class))).thenReturn(new Alert());

        ruleEngineService.evaluateRulesForTransaction(testTransaction);

        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void testEvaluateRulesWithEasyRules() {
        List<Rule> rules = Arrays.asList(testRule);
        List<Transaction> transactions = Arrays.asList(
                Transaction.builder()
                        .amount(new BigDecimal("600.00"))
                        .build()
        );

        when(ruleRepository.findByUserIdAndActiveTrue(testUser.getId())).thenReturn(rules);
        when(transactionRepository.findByUserIdAndCategoryIdAndDateBetween(
                any(), any(), any(), any()
        )).thenReturn(transactions);
        when(alertRepository.save(any(Alert.class))).thenReturn(new Alert());
        ruleEngineService.evaluateRulesWithEasyRules(testUser.getId());
        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void testRuleWithLessThanCondition() {

        Rule lessThanRule = Rule.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .category(testCategory)
                .name("Minimum Spending Rule")
                .conditionType(CondititonType.LESS_THAN)
                .threshold(new BigDecimal("50.00"))
                .active(true)
                .period(TimePeriod.WEEKLY)
                .build();

        List<Rule> rules = Arrays.asList(lessThanRule);
        List<Transaction> transactions = Arrays.asList(
                Transaction.builder()
                        .amount(new BigDecimal("30.00"))
                        .build()
        );

        when(ruleRepository.findByUserIdAndActiveTrue(testUser.getId())).thenReturn(rules);
        when(transactionRepository.findByUserIdAndCategoryIdAndDateBetween(
                any(), any(), any(), any()
        )).thenReturn(transactions);
        when(alertRepository.save(any(Alert.class))).thenReturn(new Alert());

        ruleEngineService.evaluateRulesForUser(testUser.getId());

        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void testRuleWithEqualCondition() {
        Rule equalRule = Rule.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .category(testCategory)
                .name("Exact Amount Rule")
                .conditionType(CondititonType.EQUAL_TO)
                .threshold(new BigDecimal("100.00"))
                .active(true)
                .period(TimePeriod.DAILY)
                .build();

        List<Rule> rules = Arrays.asList(equalRule);
        List<Transaction> transactions = Arrays.asList(
                Transaction.builder()
                        .amount(new BigDecimal("100.00"))
                        .build()
        );

        when(ruleRepository.findByUserIdAndActiveTrue(testUser.getId())).thenReturn(rules);
        when(transactionRepository.findByUserIdAndCategoryIdAndDateBetween(
                any(), any(), any(), any()
        )).thenReturn(transactions);
        when(alertRepository.save(any(Alert.class))).thenReturn(new Alert());

        ruleEngineService.evaluateRulesForUser(testUser.getId());

        verify(alertRepository, times(1)).save(any(Alert.class));
    }
} 