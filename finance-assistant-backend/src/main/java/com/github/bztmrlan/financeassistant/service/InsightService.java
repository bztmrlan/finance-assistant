package com.github.bztmrlan.financeassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.bztmrlan.financeassistant.dto.InsightRequest;
import com.github.bztmrlan.financeassistant.dto.InsightResponse;
import com.github.bztmrlan.financeassistant.enums.InsightType;
import com.github.bztmrlan.financeassistant.model.*;
import com.github.bztmrlan.financeassistant.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsightService {

    private final InsightRepository insightRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final CategoryRepository categoryRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GoogleGeminiService googleGeminiService;


    @Value("${huggingface.api.key}")
    private String apiKey;


    @Value("${huggingface.model:gpt2}")
    private String model;

    @Value("${huggingface.max-length:256}")
    private Integer maxLength;

    @Value("${huggingface.base.url:https://api-inference.huggingface.co}")
    private String baseUrl;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
        value = {org.springframework.dao.OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public InsightResponse generateInsight(InsightRequest request, UUID userId) {
        try {
            log.info("Generating insight for user {} with question: {}", userId, request.getQuestion());


            Map<String, Object> financialData = gatherFinancialData(userId, request.getTimePeriod());
            

            String prompt = buildInsightPrompt(request.getQuestion(), financialData, request.getAnalysisDepth());
            

            String aiResponse = null;
            String aiProvider = "Unknown";
            
            try {
                log.info("Attempting to generate insight using Google Gemini");
                aiResponse = googleGeminiService.generateFinancialInsight(request.getQuestion(), financialData);
                aiProvider = "Google Gemini";
            } catch (Exception e) {
                log.warn("Google Gemini failed, falling back to Hugging Face: {}", e.getMessage());
                try {
                    aiResponse = callHuggingFaceAPI(prompt);
                    aiProvider = "Hugging Face";
                } catch (Exception hfException) {
                    log.error("Both Google Gemini and Hugging Face failed: {}", hfException.getMessage());
                }
            }
            
            String insightMessage;
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                log.warn("All AI models returned empty response, generating fallback insight");
                insightMessage = generateFallbackInsight(request.getQuestion(), financialData);
                aiProvider = "Fallback";
            } else {

                insightMessage = parseAIResponse(aiResponse);
            }
            

            financialData.put("aiProvider", aiProvider);
            

            InsightType insightType = determineInsightType(request.getQuestion());
            

            Insight insight = Insight.builder()
                    .user(User.builder().id(userId).build())
                    .type(insightType)
                    .message(insightMessage)
                    .userQuestion(request.getQuestion())
                    .generatedAt(Instant.now())
                    .viewed(false)
                    .confidenceScore(calculateConfidenceScore(financialData))
                    .categoryTags(extractCategoryTags(financialData))
                    .timePeriod(request.getTimePeriod())
                    .insightData(createCompactInsightData(financialData))
                    .build();

            Insight savedInsight = insightRepository.save(insight);

            return convertToResponse(savedInsight, financialData);

        } catch (Exception e) {
            log.error("Error generating insight for user {} with question: {}", userId, request.getQuestion(), e);
            throw new RuntimeException("Failed to generate insight: " + e.getMessage(), e);
        }
    }


    private Map<String, Object> gatherFinancialData(UUID userId, String timePeriod) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            LocalDateTime startDate = parseTimePeriod(timePeriod);
            LocalDateTime endDate;

            if (timePeriod != null && timePeriod.contains("_2025")) {
                endDate = startDate.plusMonths(1).minusSeconds(1);
            } else {
                endDate = LocalDateTime.now();
            }
            
            log.info("Gathering data for time period: {}, startDate: {}, endDate: {}", 
                    timePeriod, startDate, endDate);


            List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(
                    userId, startDate.toLocalDate(), endDate.toLocalDate());
            
            log.info("Found {} transactions for user {} in date range {} to {}", 
                    transactions.size(), userId, startDate.toLocalDate(), endDate.toLocalDate());

            data.put("totalTransactions", transactions.size());
            data.put("startDate", startDate.toString());
            data.put("endDate", endDate.toString());
            data.put("timePeriod", timePeriod);


            List<Map<String, Object>> anonymizedTransactions = transactions.stream()
                    .map(this::anonymizeTransaction)
                    .collect(Collectors.toList());
            data.put("transactions", anonymizedTransactions);


            Map<String, Object> spendingData = analyzeSpending(transactions);
            data.put("spending", spendingData);


            Map<String, Object> incomeData = analyzeIncome(transactions);
            data.put("income", incomeData);


            Map<String, Object> categoryData = analyzeCategories(transactions);
            data.put("categories", categoryData);


            List<Budget> budgets = budgetRepository.findByUserId(userId);
            if (!budgets.isEmpty()) {
                List<Map<String, Object>> budgetData = budgets.stream()
                        .map(this::anonymizeBudget)
                        .collect(Collectors.toList());
                data.put("budgets", budgetData);
            }


            List<Goal> goals = goalRepository.findByUserId(userId);
            if (!goals.isEmpty()) {
                List<Map<String, Object>> goalData = goals.stream()
                        .map(this::anonymizeGoal)
                        .collect(Collectors.toList());
                data.put("goals", goalData);
            }

            log.info("Final data structure keys: {}", data.keySet());

        } catch (Exception e) {
            log.error("Error gathering financial data for user {} with time period {}", userId, timePeriod, e);
            data.put("totalTransactions", 0);
            data.put("spending", new HashMap<>());
            data.put("income", new HashMap<>());
            data.put("categories", new HashMap<>());
        }

        return data;
    }


    private Map<String, Object> anonymizeTransaction(Transaction transaction) {
        Map<String, Object> anonymized = new HashMap<>();
        anonymized.put("id", "txn_" + transaction.getId().toString().substring(0, 8));
        anonymized.put("description", transaction.getDescription());
        anonymized.put("amount", transaction.getAmount());
        anonymized.put("type", transaction.getType());
        anonymized.put("date", transaction.getDate().toString());
        
        if (transaction.getCategory() != null) {
            anonymized.put("category", transaction.getCategory().getName());
        }
        
        return anonymized;
    }


    private Map<String, Object> anonymizeBudget(Budget budget) {
        Map<String, Object> anonymized = new HashMap<>();
        anonymized.put("id", "budget_" + budget.getId().toString().substring(0, 8));
        anonymized.put("name", budget.getName());
        anonymized.put("startDate", budget.getStartDate().toString());
        anonymized.put("endDate", budget.getEndDate().toString());
        anonymized.put("status", budget.getStatus());
        
        if (budget.getCategories() != null) {
            anonymized.put("categoryCount", budget.getCategories().size());
        }
        
        return anonymized;
    }


    private Map<String, Object> anonymizeGoal(Goal goal) {
        Map<String, Object> anonymized = new HashMap<>();
        anonymized.put("id", "goal_" + goal.getId().toString().substring(0, 8));
        anonymized.put("name", goal.getName());
        anonymized.put("targetAmount", goal.getTargetAmount());
        anonymized.put("currentAmount", goal.getCurrentAmount());
        anonymized.put("targetDate", goal.getTargetDate().toString());
        anonymized.put("completed", goal.isCompleted());
        return anonymized;
    }


    private String buildInsightPrompt(String question, Map<String, Object> financialData, String analysisDepth) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a financial advisor AI. Analyze the following financial data and provide insights based on the user's question.\n\n");
        prompt.append("USER QUESTION: ").append(question).append("\n\n");
        prompt.append("ANALYSIS DEPTH: ").append(analysisDepth != null ? analysisDepth : "detailed").append("\n\n");
        
        prompt.append("FINANCIAL DATA:\n");
        prompt.append("==============\n");
        

        if (financialData.containsKey("totalTransactions")) {
            prompt.append("Total Transactions: ").append(financialData.get("totalTransactions")).append("\n");
        }
        

        if (financialData.containsKey("timePeriod")) {
            prompt.append("Time Period: ").append(financialData.get("timePeriod")).append("\n");
        }
        

        if (financialData.containsKey("spending")) {
            Map<String, Object> spending = (Map<String, Object>) financialData.get("spending");
            prompt.append("\nSPENDING ANALYSIS:\n");
            if (spending.containsKey("totalSpent")) {
                prompt.append("Total Spent: $").append(spending.get("totalSpent")).append("\n");
            }
            if (spending.containsKey("topCategory")) {
                prompt.append("Top Spending Category: ").append(spending.get("topCategory")).append("\n");
            }
            if (spending.containsKey("categoryBreakdown")) {
                prompt.append("Category Breakdown: ").append(spending.get("categoryBreakdown")).append("\n");
            }
        }
        

        if (financialData.containsKey("income")) {
            Map<String, Object> income = (Map<String, Object>) financialData.get("income");
            prompt.append("\nINCOME ANALYSIS:\n");
            if (income.containsKey("totalIncome")) {
                prompt.append("Total Income: $").append(income.get("totalIncome")).append("\n");
            }
        }
        

        if (financialData.containsKey("categories")) {
            Map<String, Object> categories = (Map<String, Object>) financialData.get("categories");
            prompt.append("\nCATEGORY ANALYSIS:\n");
            if (categories.containsKey("categoryCount")) {
                prompt.append("Categories Used: ").append(categories.get("categoryCount")).append("\n");
            }
        }
        

        if (financialData.containsKey("budgets")) {
            List<Map<String, Object>> budgets = (List<Map<String, Object>>) financialData.get("budgets");
            prompt.append("\nBUDGET INFORMATION:\n");
            prompt.append("Active Budgets: ").append(budgets.size()).append("\n");
        }
        

        if (financialData.containsKey("goals")) {
            List<Map<String, Object>> goals = (List<Map<String, Object>>) financialData.get("goals");
            prompt.append("\nGOAL INFORMATION:\n");
            prompt.append("Active Goals: ").append(goals.size()).append("\n");
        }
        

        if (financialData.containsKey("transactions")) {
            List<Map<String, Object>> transactions = (List<Map<String, Object>>) financialData.get("transactions");
            prompt.append("\nSAMPLE TRANSACTIONS (showing first 10):\n");
            transactions.stream().limit(10).forEach(tx -> {
                prompt.append("- ").append(tx.get("date")).append(": $")
                      .append(tx.get("amount")).append(" ").append(tx.get("type"))
                      .append(" - ").append(tx.get("description"));
                if (tx.containsKey("category")) {
                    prompt.append(" (").append(tx.get("category")).append(")");
                }
                prompt.append("\n");
            });
        }
        
        prompt.append("\nTASK: Generate a comprehensive financial insight analysis.\n\n");
        prompt.append("REQUIREMENTS:\n");
        prompt.append("- Analyze the financial data thoroughly\n");
        prompt.append("- Provide specific, actionable insights based on the user's question\n");
        prompt.append("- Include relevant numbers and percentages when available\n");
        prompt.append("- Suggest improvements or areas of concern\n");
        prompt.append("- Keep the response professional but conversational\n");
        prompt.append("- Focus on the specific time period mentioned\n");
        prompt.append("- If no data is available for the time period, explain why and suggest alternatives\n\n");
        
        prompt.append("RESPONSE FORMAT: Provide a detailed financial analysis with clear sections for insights, recommendations, and action items.\n\n");
        prompt.append("ANALYSIS:\n");
        
        return prompt.toString();
    }


    private String callHuggingFaceAPI(String prompt) {
        try {
            String url = baseUrl + "/models/" + model;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);


            Map<String, Object> requestBody = Map.of(
                "inputs", prompt,
                "parameters", Map.of(
                    "max_new_tokens", maxLength,
                    "temperature", 0.7,
                    "do_sample", true,
                    "top_p", 0.9,
                    "repetition_penalty", 1.1
                )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            log.info("Calling Hugging Face text generation API with prompt length: {}", prompt.length());
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Hugging Face text generation API call successful");
                return response.getBody();
            } else {
                log.error("Hugging Face text generation API call failed with status: {}", response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.error("Error calling Hugging Face text generation API: {}", e.getMessage(), e);
            return null;
        }
    }

    private String parseAIResponse(String aiResponse) {
        try {

            String cleanedResponse = aiResponse.trim();
            
            log.info("Raw AI response: {}", aiResponse.substring(0, Math.min(200, aiResponse.length())));
            

            if (cleanedResponse.startsWith("[") && cleanedResponse.endsWith("]")) {
                try {
                    List<Map<String, Object>> responseArray = objectMapper.readValue(cleanedResponse, List.class);
                    if (!responseArray.isEmpty()) {
                        Map<String, Object> firstElement = responseArray.get(0);
                        if (firstElement.containsKey("generated_text")) {
                            cleanedResponse = (String) firstElement.get("generated_text");
                            log.info("Extracted generated_text from JSON response");
                        } else {
                            log.warn("No generated_text found in JSON response, using raw response");
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not parse AI response as JSON, using raw response: {}", e.getMessage());
                }
            }
            

            if (cleanedResponse.length() > 2000) {
                cleanedResponse = cleanedResponse.substring(0, 2000) + "...";
                log.warn("AI response was too long, truncated to 2000 characters");
            }
            
            log.info("Final parsed AI response, length: {}", cleanedResponse.length());
            return cleanedResponse;
            
        } catch (Exception e) {
            log.error("Error parsing AI response: {}", e.getMessage(), e);
            return "Based on your financial data, I can see some interesting patterns. Consider reviewing your spending habits and setting clear financial goals.";
        }
    }


    private LocalDateTime parseTimePeriod(String timePeriod) {
        if (timePeriod == null || timePeriod.trim().isEmpty()) {
            return LocalDateTime.now().minusDays(30);
        }

        String lowerPeriod = timePeriod.toLowerCase();

        if (lowerPeriod.contains("august")) {
            return LocalDateTime.of(2025, 8, 1, 0, 0, 0);
        } else if (lowerPeriod.contains("july")) {
            return LocalDateTime.of(2025, 7, 1, 0, 0, 0);
        } else if (lowerPeriod.contains("september")) {
            return LocalDateTime.of(2025, 9, 1, 0, 0, 0);
        } else if (lowerPeriod.contains("june")) {
            return LocalDateTime.of(2025, 6, 1, 0, 0, 0);
        } else if (lowerPeriod.contains("may")) {
            return LocalDateTime.of(2025, 5, 1, 0, 0, 0);
        } else if (lowerPeriod.contains("april")) {
            return LocalDateTime.of(2025, 4, 1, 0, 0, 0);
        } else if (lowerPeriod.contains("march")) {
            return LocalDateTime.of(2025, 3, 1, 0, 0, 0);
        } else if (lowerPeriod.contains("february")) {
            return LocalDateTime.of(2025, 2, 1, 0, 0, 0);
        } else if (lowerPeriod.contains("january")) {
            return LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        } else if (lowerPeriod.contains("december")) {
            return LocalDateTime.of(2025, 12, 1, 0, 0, 0);
        } else if (lowerPeriod.contains("november")) {
            return LocalDateTime.of(2025, 11, 1, 0, 0, 0);
        } else if (lowerPeriod.contains("october")) {
            return LocalDateTime.of(2025, 10, 1, 0, 0, 0);
        } else if (lowerPeriod.contains("last") && lowerPeriod.contains("days")) {

            String[] parts = lowerPeriod.split("\\s+");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("last") && i + 2 < parts.length && parts[i + 2].equals("days")) {
                    try {
                        int days = Integer.parseInt(parts[i + 1]);
                        return LocalDateTime.now().minusDays(days);
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse days from time period: {}", timePeriod);
                    }
                }
            }
        } else if (lowerPeriod.contains("this month")) {
            return LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        } else if (lowerPeriod.contains("this year")) {
            return LocalDateTime.now().withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        }


        return LocalDateTime.now().minusDays(30);
    }


    private Map<String, Object> analyzeSpending(List<Transaction> transactions) {
        Map<String, Object> spendingData = new HashMap<>();
        
        log.info("Analyzing spending for {} transactions", transactions.size());

        List<Transaction> expenses = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .collect(Collectors.toList());
        
        log.info("Found {} expense transactions", expenses.size());

        if (!expenses.isEmpty()) {
            BigDecimal totalSpent = expenses.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            spendingData.put("totalSpent", totalSpent);
            spendingData.put("transactionCount", expenses.size());
            
            log.info("Total spent: {}, transaction count: {}", totalSpent, expenses.size());


            Map<String, BigDecimal> categorySpending = expenses.stream()
                    .filter(t -> t.getCategory() != null)
                    .collect(Collectors.groupingBy(
                            t -> t.getCategory().getName(),
                            Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                    ));

            if (!categorySpending.isEmpty()) {

                String topCategory = categorySpending.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("Unknown");

                spendingData.put("topCategory", topCategory);
                spendingData.put("categoryBreakdown", categorySpending);
                
                log.info("Top spending category: {}, total categories: {}", topCategory, categorySpending.size());
            } else {
                log.info("No categorized expenses found");
            }
        } else {
            log.info("No expense transactions found");
            spendingData.put("totalSpent", BigDecimal.ZERO);
            spendingData.put("transactionCount", 0);
        }

        return spendingData;
    }


    private Map<String, Object> analyzeIncome(List<Transaction> transactions) {
        Map<String, Object> incomeData = new HashMap<>();
        
        log.info("Analyzing income for {} transactions", transactions.size());

        List<Transaction> income = transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .collect(Collectors.toList());
        
        log.info("Found {} income transactions", income.size());

        if (!income.isEmpty()) {
            BigDecimal totalIncome = income.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            incomeData.put("totalIncome", totalIncome);
            incomeData.put("transactionCount", income.size());
            
            log.info("Total income: {}, transaction count: {}", totalIncome, income.size());
        } else {
            log.info("No income transactions found");
            incomeData.put("totalIncome", BigDecimal.ZERO);
            incomeData.put("transactionCount", 0);
        }

        return incomeData;
    }


    private Map<String, Object> analyzeCategories(List<Transaction> transactions) {
        Map<String, Object> categoryData = new HashMap<>();
        
        log.info("Analyzing categories for {} transactions", transactions.size());

        Map<String, List<Transaction>> categoryGroups = transactions.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(t -> t.getCategory().getName()));
        
        log.info("Found {} categories with transactions", categoryGroups.size());

        if (!categoryGroups.isEmpty()) {
            categoryData.put("categoryCount", categoryGroups.size());
            categoryData.put("categoryBreakdown", categoryGroups.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().size()
                    )));
            
            log.info("Category breakdown: {}", categoryGroups.keySet());
        } else {
            log.info("No categories found in transactions");
        }

        return categoryData;
    }


    private InsightType determineInsightType(String question) {
        String lowerQuestion = question.toLowerCase();
        
        if (lowerQuestion.contains("spend") || lowerQuestion.contains("spent") || lowerQuestion.contains("expense")) {
            return InsightType.SPENDING_ANALYSIS;
        } else if (lowerQuestion.contains("earn") || lowerQuestion.contains("income") || lowerQuestion.contains("salary")) {
            return InsightType.INCOME_ANALYSIS;
        } else if (lowerQuestion.contains("save") || lowerQuestion.contains("saving")) {
            return InsightType.SAVINGS_OPPORTUNITY;
        } else if (lowerQuestion.contains("budget")) {
            return InsightType.BUDGET_PERFORMANCE;
        } else if (lowerQuestion.contains("goal")) {
            return InsightType.GOAL_PROGRESS;
        } else {
            return InsightType.CUSTOM_QUERY;
        }
    }


    private Double calculateConfidenceScore(Map<String, Object> data) {
        double score = 0.0;
        
        if (data.containsKey("totalTransactions")) {
            Integer transactionCount = (Integer) data.get("totalTransactions");
            if (transactionCount > 0) {
                score += 0.4;
                if (transactionCount > 10) {
                    score += 0.2;
                }
            }
        }
        
        if (data.containsKey("spending")) {
            score += 0.2;
        }
        
        if (data.containsKey("income")) {
            score += 0.2;
        }
        
        if (data.containsKey("budgets") || data.containsKey("goals")) {
            score += 0.2;
        }
        
        return Math.min(score, 1.0);
    }


    private String extractCategoryTags(Map<String, Object> data) {
        Set<String> tags = new HashSet<>();
        
        if (data.containsKey("spending")) {
            Map<String, Object> spending = (Map<String, Object>) data.get("spending");
            if (spending.containsKey("categoryBreakdown")) {
                Map<String, BigDecimal> categories = (Map<String, BigDecimal>) spending.get("categoryBreakdown");
                tags.addAll(categories.keySet());
            }
        }
        
        if (data.containsKey("categories")) {
            Map<String, Object> categories = (Map<String, Object>) data.get("categories");
            if (categories.containsKey("categoryBreakdown")) {
                Map<String, Integer> categoryCounts = (Map<String, Integer>) categories.get("categoryBreakdown");
                tags.addAll(categoryCounts.keySet());
            }
        }
        
        return tags.stream().limit(10).collect(Collectors.joining(", "));
    }


    private String createCompactInsightData(Map<String, Object> data) {
        try {
            log.info("Creating compact insight data with {} keys", data.size());

            Map<String, Object> compactData = new HashMap<>();


            if (data.containsKey("totalTransactions")) {
                compactData.put("totalTransactions", data.get("totalTransactions"));
            }

            if (data.containsKey("spending")) {
                Map<String, Object> spending = (Map<String, Object>) data.get("spending");
                if (spending != null) {
                    Map<String, Object> compactSpending = new HashMap<>();
                    if (spending.containsKey("totalSpent")) {
                        compactSpending.put("totalSpent", spending.get("totalSpent"));
                    }
                    if (spending.containsKey("topCategory")) {
                        compactSpending.put("topCategory", spending.get("topCategory"));
                    }
                    if (spending.containsKey("categoryBreakdown")) {

                        Object categoryBreakdown = spending.get("categoryBreakdown");
                        if (categoryBreakdown instanceof Map) {
                            Map<String, Object> categories = (Map<String, Object>) categoryBreakdown;
                            Map<String, Object> limitedCategories = categories.entrySet().stream()
                                    .limit(5)
                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                            compactSpending.put("categoryBreakdown", limitedCategories);
                        }
                    }
                    compactData.put("spending", compactSpending);
                }
            }

            if (data.containsKey("income")) {
                Map<String, Object> income = (Map<String, Object>) data.get("income");
                if (income != null) {
                    Map<String, Object> compactIncome = new HashMap<>();
                    if (income.containsKey("totalIncome")) {
                        compactIncome.put("totalIncome", income.get("totalIncome"));
                    }
                    compactData.put("income", compactIncome);
                }
            }

            if (data.containsKey("budgets")) {
                List<Map<String, Object>> budgets = (List<Map<String, Object>>) data.get("budgets");
                if (budgets != null && !budgets.isEmpty()) {
                    compactData.put("budgetCount", budgets.size());
                    double totalLimit = budgets.stream()
                            .mapToDouble(b -> {
                                Object limit = b.get("totalLimit");
                                return limit instanceof Number ? ((Number) limit).doubleValue() : 0.0;
                            })
                            .sum();
                    compactData.put("totalBudgetLimit", totalLimit);
                }
            }

            if (data.containsKey("goals")) {
                List<Map<String, Object>> goals = (List<Map<String, Object>>) data.get("goals");
                if (goals != null && !goals.isEmpty()) {
                    compactData.put("goalCount", goals.size());

                    double totalAmount = goals.stream()
                            .mapToDouble(g -> {
                                Object amount = g.get("targetAmount");
                                return amount instanceof Number ? ((Number) amount).doubleValue() : 0.0;
                            })
                            .sum();
                    compactData.put("totalGoalAmount", totalAmount);
                }
            }

            String jsonData = objectMapper.writeValueAsString(compactData);
            log.info("Created compact insight data with {} characters", jsonData.length());

            if (jsonData.length() > 200) {
                log.warn("Insight data too long ({} chars), creating minimal version", jsonData.length());
                Map<String, Object> minimalData = new HashMap<>();
                minimalData.put("totalTransactions", data.getOrDefault("totalTransactions", 0));
                if (data.containsKey("spending")) {
                    Map<String, Object> spending = (Map<String, Object>) data.get("spending");
                    if (spending != null && spending.containsKey("totalSpent")) {
                        minimalData.put("totalSpent", spending.get("totalSpent"));
                    }
                }
                jsonData = objectMapper.writeValueAsString(minimalData);
                log.info("Created minimal insight data with {} characters", jsonData.length());
            }

            return jsonData;
        } catch (Exception e) {
            log.error("Error creating compact insight data: {}", e.getMessage(), e);
            return "{}";
        }
    }


    private InsightResponse convertToResponse(Insight insight, Map<String, Object> data) {
        try {
            log.info("Converting insight to response: id={}, type={}", insight.getId(), insight.getType());
            Map<String, Object> insightDataMap = objectMapper.readValue(insight.getInsightData(), Map.class);

            log.info("Successfully converted insight to response with {} data points", data.get("totalTransactions"));

            String dataSource = "AI Analysis";
            if (data.containsKey("aiProvider")) {
                dataSource = data.get("aiProvider") + " AI Analysis";
            }
            
            return InsightResponse.builder()
                    .id(insight.getId())
                    .userId(insight.getUser().getId())
                    .type(insight.getType())
                    .message(insight.getMessage())
                    .userQuestion(insight.getUserQuestion())
                    .generatedAt(insight.getGeneratedAt())
                    .viewed(insight.isViewed())
                    .confidenceScore(insight.getConfidenceScore())
                    .categoryTags(insight.getCategoryTags())
                    .timePeriod(insight.getTimePeriod())
                    .insightData(insightDataMap)
                    .analysisType(insight.getType().toString())
                    .dataSource(dataSource)
                    .dataPointsAnalyzed((Integer) data.get("totalTransactions"))
                    .build();
            

            
        } catch (Exception e) {
            log.error("Error converting insight to response for insight {}", insight.getId(), e);
            return InsightResponse.builder()
                    .id(insight.getId())
                    .userId(insight.getUser().getId())
                    .type(insight.getType())
                    .message(insight.getMessage())
                    .userQuestion(insight.getUserQuestion())
                    .generatedAt(insight.getGeneratedAt())
                    .viewed(insight.isViewed())
                    .build();
        }
    }


    public List<InsightResponse> getUserInsights(UUID userId) {
        log.info("Getting insights for user: {}", userId);
        List<Insight> insights = insightRepository.findByUserIdOrderByGeneratedAtDesc(userId);
        log.info("Found {} insights for user {}", insights.size(), userId);
        
        return insights.stream()
                .map(insight -> {
                    try {
                        Map<String, Object> data = objectMapper.readValue(insight.getInsightData(), Map.class);
                        return convertToResponse(insight, data);
                    } catch (Exception e) {
                        log.error("Error converting insight to response for insight {}", insight.getId(), e);
                        return convertToResponse(insight, new HashMap<>());
                    }
                })
                .collect(Collectors.toList());
    }


    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {

            Map<String, Object> geminiStatus = googleGeminiService.getServiceStatus();
            status.put("geminiStatus", geminiStatus);
            

            boolean huggingFaceWorking = testConnection();
            status.put("huggingFaceStatus", huggingFaceWorking ? "OK" : "FAILED");
            status.put("currentModel", model);
            

            boolean databaseWorking = testDatabaseConnection();
            status.put("databaseStatus", databaseWorking ? "OK" : "FAILED");
            

            Map<String, Object> schemaInfo = checkDatabaseSchema();
            status.put("databaseSchema", schemaInfo);
            

            boolean geminiWorking = "OK".equals(geminiStatus.get("status"));
            boolean overallWorking = geminiWorking || huggingFaceWorking;
            status.put("overallStatus", overallWorking ? "OK" : "DEGRADED");
            status.put("primaryAIProvider", geminiWorking ? "Google Gemini" : "Hugging Face (Fallback)");
            status.put("timestamp", Instant.now().toString());
            
        } catch (Exception e) {
            log.error("Error getting service status: {}", e.getMessage(), e);
            status.put("overallStatus", "ERROR");
            status.put("error", e.getMessage());
        }
        
        return status;
    }
    

    private boolean testDatabaseConnection() {
        try {

            Insight testInsight = Insight.builder()
                    .id(UUID.randomUUID())
                    .user(new User())
                    .type(InsightType.SPENDING_ANALYSIS)
                    .message("Test insight")
                    .generatedAt(Instant.now())
                    .viewed(false)
                    .insightData("{\"test\":true}")
                    .build();
            

            insightRepository.save(testInsight);
            return true;
        } catch (Exception e) {

            if (e.getMessage() != null && e.getMessage().contains("constraint")) {
                log.info("Database connection test successful (expected constraint error)");
                return true;
            }
            log.error("Database connection test failed: {}", e.getMessage());
            return false;
        }
    }
    

    public Map<String, Object> checkDatabaseSchema() {
        Map<String, Object> schemaInfo = new HashMap<>();
        
        try {
            Insight testInsight = Insight.builder()
                    .id(UUID.randomUUID())
                    .user(new User())
                    .type(InsightType.SPENDING_ANALYSIS)
                    .message("Test")
                    .generatedAt(Instant.now())
                    .viewed(false)
                    .insightData("{\"test\":true,\"data\":\"This is a test string that is longer than 255 characters to check if the database schema has been updated to use TEXT instead of VARCHAR(255) for the insight_data field. This should help us determine if we need to update the database schema.\"}")
                    .build();
            
            insightRepository.save(testInsight);
            schemaInfo.put("status", "SUCCESS");
            schemaInfo.put("message", "Database schema appears to be up to date");
            
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("value too long")) {
                schemaInfo.put("status", "NEEDS_UPDATE");
                schemaInfo.put("message", "Database schema needs updating - insight_data field is still VARCHAR(255)");
                schemaInfo.put("recommendation", "Run ALTER TABLE insights ALTER COLUMN insight_data TYPE TEXT;");
            } else if (e.getMessage() != null && e.getMessage().contains("constraint")) {
                schemaInfo.put("status", "PARTIAL_SUCCESS");
                schemaInfo.put("message", "Database connection works but schema test incomplete due to foreign key constraints");
            } else {
                schemaInfo.put("status", "ERROR");
                schemaInfo.put("message", "Database test failed: " + e.getMessage());
            }
        }
        
        return schemaInfo;
    }


    public void markInsightAsViewed(UUID insightId, UUID userId) {
        log.info("Marking insight {} as viewed for user {}", insightId, userId);
        Optional<Insight> insightOpt = insightRepository.findByIdAndUserId(insightId, userId);
        log.info("Found insight: {}", insightOpt.isPresent());
        insightOpt.ifPresent(insight -> {
            insight.setViewed(true);
            insightRepository.save(insight);
            log.info("Successfully marked insight {} as viewed", insightId);
        });
    }


    public void deleteInsight(UUID insightId, UUID userId) {
        log.info("Deleting insight {} for user {}", insightId, userId);
        Optional<Insight> insightOpt = insightRepository.findByIdAndUserId(insightId, userId);
        insightOpt.ifPresent(insight -> {
            insightRepository.delete(insight);
            log.info("Successfully deleted insight {}", insightId);
        });
    }


    private String generateFallbackInsight(String question, Map<String, Object> financialData) {
        StringBuilder insight = new StringBuilder();
        
        insight.append("Based on your financial data analysis, here are the key insights:\n\n");
        

        if (financialData.containsKey("totalTransactions")) {
            Integer transactionCount = (Integer) financialData.get("totalTransactions");
            insight.append("📊 Transaction Overview: You have ").append(transactionCount).append(" transactions in the selected period.\n\n");
        }

        if (financialData.containsKey("spending")) {
            Map<String, Object> spending = (Map<String, Object>) financialData.get("spending");
            if (spending.containsKey("totalSpent")) {
                BigDecimal totalSpent = (BigDecimal) spending.get("totalSpent");
                if (totalSpent.compareTo(BigDecimal.ZERO) > 0) {
                    insight.append("💰 Spending Analysis: Total spending: $").append(totalSpent).append("\n");
                    
                    if (spending.containsKey("topCategory")) {
                        String topCategory = (String) spending.get("topCategory");
                        insight.append("Top spending category: ").append(topCategory).append("\n\n");
                    }
                } else {
                    insight.append("💰 Spending Analysis: No spending recorded in this period.\n\n");
                }
            }
        }
        

        if (financialData.containsKey("income")) {
            Map<String, Object> income = (Map<String, Object>) financialData.get("income");
            if (income.containsKey("totalIncome")) {
                BigDecimal totalIncome = (BigDecimal) income.get("totalIncome");
                if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
                    insight.append("💵 Income Analysis: Total income: $").append(totalIncome).append("\n\n");
                }
            }
        }
        

        insight.append("💡 Recommendations:\n");
        insight.append("- Review your spending patterns regularly\n");
        insight.append("- Set clear financial goals\n");
        insight.append("- Consider creating a budget if you haven't already\n");
        insight.append("- Monitor your progress towards financial objectives\n\n");
        
        insight.append("Note: This is a fallback analysis. The AI-powered insight generation is temporarily unavailable.");
        
        return insight.toString();
    }


    public boolean testConnection() {
        try {
            log.info("Testing Hugging Face API connection with model: {}", model);
            String testPrompt = "Generate a brief financial insight: Hello, this is a test message for financial insights.";
            String response = callHuggingFaceAPI(testPrompt);
            
            if (response != null && !response.isEmpty()) {
                log.info("Hugging Face API connection test successful");
                return true;
            } else {
                log.warn("Hugging Face API returned empty response");
                return false;
            }
        } catch (Exception e) {
            log.error("Hugging Face connection test failed: {}", e.getMessage());

            if (e.getMessage() != null && e.getMessage().contains("404")) {
                log.warn("Model {} not found, trying alternative models", model);
                return tryAlternativeModels();
            }
            
            return false;
        }
    }
    

    private boolean tryAlternativeModels() {
        String[] alternativeModels = {
            "microsoft/DialoGPT-medium",
            "facebook/opt-350m",
            "EleutherAI/gpt-neo-125M"
        };
        
        for (String altModel : alternativeModels) {
            String originalModel = this.model;
            try {
                log.info("Trying alternative model: {}", altModel);

                this.model = altModel;
                
                String testPrompt = "Generate a brief financial insight: Hello, this is a test message for financial insights.";
                String response = callHuggingFaceAPI(testPrompt);
                
                if (response != null && !response.isEmpty()) {
                    log.info("Alternative model {} works, updating configuration", altModel);
                    return true;
                }

                this.model = originalModel;
                
            } catch (Exception e) {
                log.warn("Alternative model {} also failed: {}", altModel, e.getMessage());
                this.model = originalModel;
            }
        }
        
        log.error("All alternative models failed");
        return false;
    }
} 