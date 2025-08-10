package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.enums.CategoryType;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.repository.CategoryRepository;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAICategorizationService {

    private final OpenAiService openAiService;
    private final CategoryRepository categoryRepository;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;

    @Value("${openai.max-tokens:150}")
    private Integer maxTokens;

    /**
     * Categorizes a transaction using OpenAI
     */
    public Optional<Category> categorizeTransaction(String description, BigDecimal amount, 
                                                 CategoryType type, User user) {
        try {
            String prompt = buildCategorizationPrompt(description, amount, type);
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(new ChatMessage("user", prompt)))
                    .maxTokens(maxTokens)
                    .temperature(0.1)
                    .build();

            String response = openAiService.createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();

            return parseCategorizationResponse(response, user);
            
        } catch (Exception e) {
            log.error("Error categorizing transaction with OpenAI: {}", description, e);
            return Optional.empty();
        }
    }

    /**
     * Categorizes multiple transactions in batch for efficiency
     */
    public List<Category> categorizeTransactionsBatch(List<TransactionCategorizationRequest> requests, User user) {
        try {
            String batchPrompt = buildBatchCategorizationPrompt(requests);
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(new ChatMessage("user", batchPrompt)))
                    .maxTokens(maxTokens * 2)
                    .temperature(0.1)
                    .build();

            String response = openAiService.createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();

            return parseBatchCategorizationResponse(response, user);
            
        } catch (Exception e) {
            log.error("Error in batch categorization with OpenAI", e);
            return List.of();
        }
    }

    private String buildCategorizationPrompt(String description, BigDecimal amount, CategoryType type) {
        return String.format("""
            Categorize this financial transaction:
            Description: %s
            Amount: %s
            Type: %s
            
            Return only a JSON response in this exact format:
            {
                "categoryName": "string",
                "confidence": 0.95
            }
            
            Choose the most appropriate category name from these common financial categories:
            - Food & Dining (restaurants, groceries, cafes)
            - Transportation (gas, public transit, rideshare)
            - Shopping (clothing, electronics, home goods)
            - Entertainment (movies, games, events)
            - Healthcare (medical, dental, pharmacy)
            - Utilities (electricity, water, internet)
            - Housing (rent, mortgage, maintenance)
            - Income (salary, freelance, investment)
            - Transfer (bank transfers, ATM withdrawals)
            - Other
            
            Base your decision on the description and amount. Be specific and accurate.
            """, description, amount, type);
    }

    private String buildBatchCategorizationPrompt(List<TransactionCategorizationRequest> requests) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Categorize these financial transactions. Return only a JSON array response:\n\n");
        
        for (int i = 0; i < requests.size(); i++) {
            TransactionCategorizationRequest req = requests.get(i);
            prompt.append(String.format("%d. Description: %s, Amount: %s, Type: %s\n", 
                i + 1, req.description(), req.amount(), req.type()));
        }
        
        prompt.append("\nReturn JSON array in this exact format:\n");
        prompt.append("[\n");
        prompt.append("  {\"index\": 0, \"categoryName\": \"string\", \"confidence\": 0.95},\n");
        prompt.append("  {\"index\": 1, \"categoryName\": \"string\", \"confidence\": 0.95}\n");
        prompt.append("]\n");
        
        return prompt.toString();
    }

    private Optional<Category> parseCategorizationResponse(String response, User user) {
        try {

            if (response.contains("categoryName")) {
                String categoryName = extractValue(response, "categoryName");
                if (categoryName != null && !categoryName.isEmpty()) {
                    return findOrCreateCategory(categoryName, user);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing OpenAI response: {}", response, e);
        }
        return Optional.empty();
    }

    private List<Category> parseBatchCategorizationResponse(String response, User user) {
        return List.of();
    }

    private String extractValue(String json, String key) {
        int start = json.indexOf("\"" + key + "\":");
        if (start == -1) return null;
        
        start = json.indexOf("\"", start + key.length() + 3);
        if (start == -1) return null;
        
        int end = json.indexOf("\"", start + 1);
        if (end == -1) return null;
        
        return json.substring(start + 1, end);
    }

    private Optional<Category> findOrCreateCategory(String categoryName, User user) {

        Optional<Category> existing = categoryRepository.findByUserIdAndNameIgnoreCase(user.getId(), categoryName);
        if (existing.isPresent()) {
            return existing;
        }


        try {
            CategoryType categoryType = determineCategoryType(categoryName);
            Category newCategory = Category.builder()
                    .name(categoryName)
                    .type(categoryType)
                    .user(user)
                    .build();
            
            Category saved = categoryRepository.save(newCategory);
            log.info("Created new category: {} for user: {}", categoryName, user.getId());
            return Optional.of(saved);
            
        } catch (Exception e) {
            log.error("Error creating new category: {}", categoryName, e);
            return Optional.empty();
        }
    }

    private CategoryType determineCategoryType(String categoryName) {
        String lowerName = categoryName.toLowerCase();
        
        if (lowerName.contains("income") || lowerName.contains("salary") || 
            lowerName.contains("freelance") || lowerName.contains("investment")) {
            return CategoryType.INCOME;
        } else if (lowerName.contains("transfer") || lowerName.contains("atm") || 
                   lowerName.contains("withdrawal")) {
            return CategoryType.TRANSFER;
        } else {
            return CategoryType.EXPENSE;
        }
    }

    public record TransactionCategorizationRequest(
        String description, 
        BigDecimal amount, 
        CategoryType type
    ) {}
} 