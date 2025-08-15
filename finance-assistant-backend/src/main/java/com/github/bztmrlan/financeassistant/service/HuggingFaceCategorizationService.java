package com.github.bztmrlan.financeassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.bztmrlan.financeassistant.enums.CategoryType;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.repository.CategoryRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class HuggingFaceCategorizationService {

    private final CategoryRepository categoryRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${huggingface.api.key}")
    private String apiKey;

    @Value("${huggingface.model:facebook/bart-large-mnli}")
    private String model;

    @Value("${huggingface.max-length:512}")
    private Integer maxLength;


    public Optional<Category> categorizeTransaction(String description, BigDecimal amount, 
                                                 CategoryType type, User user) {
        try {

            List<Category> userCategories = categoryRepository.findByUserIdAndType(user.getId(), type);
            
            if (userCategories.isEmpty()) {
                log.warn("No categories found for user {} and type {}", user.getId(), type);
                return Optional.empty();
            }


            String prompt = buildCategorizationPrompt(description, amount, type, userCategories);
            

            HuggingFaceRequest request = HuggingFaceRequest.builder()
                    .inputs(prompt)
                    .parameters(HuggingFaceParameters.builder()
                            .candidateLabels(userCategories.stream()
                                    .map(Category::getName)
                                    .toList())
                            .multiLabel(false)
                            .build())
                    .build();

            String response = callHuggingFaceAPI(request);

            return parseCategorizationResponse(response, userCategories);
        } catch (Exception e) {
            log.error("Error in transaction categorization with Hugging Face", e);
            return Optional.empty();
        }
    }

    public List<Category> categorizeTransactionsBatch(List<TransactionCategorizationRequest> requests, User user) {
        try {
            List<Category> results = new ArrayList<>();
            
            for (TransactionCategorizationRequest request : requests) {
                Optional<Category> category = categorizeTransaction(
                    request.getDescription(), 
                    request.getAmount(), 
                    request.getType(), 
                    user
                );
                category.ifPresent(results::add);
            }
            
            return results;
            
        } catch (Exception e) {
            log.error("Error in batch categorization with Hugging Face", e);
            return List.of();
        }
    }

    private String buildCategorizationPrompt(String description, BigDecimal amount, 
                                           CategoryType type, List<Category> categories) {
        return "Categorize this financial transaction: " +
                "Description: " + description +
                ", Amount: " + amount +
                ", Type: " + type;
    }

    private String callHuggingFaceAPI(HuggingFaceRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<HuggingFaceRequest> entity = new HttpEntity<>(request, headers);
            
            String url = "https://api-inference.huggingface.co/models/" + model;
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                log.error("Hugging Face API error: {} - {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Hugging Face API call failed: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error calling Hugging Face API", e);
            throw new RuntimeException("Failed to call Hugging Face API", e);
        }
    }

    private Optional<Category> parseCategorizationResponse(String response, List<Category> userCategories) {
        try {

            HuggingFaceResponse hfResponse = objectMapper.readValue(response, HuggingFaceResponse.class);
            
            if (hfResponse != null && hfResponse.getLabels() != null && !hfResponse.getLabels().isEmpty()) {
                String predictedCategory = hfResponse.getLabels().get(0);

                return userCategories.stream()
                        .filter(cat -> cat.getName().equalsIgnoreCase(predictedCategory))
                        .findFirst();
            }
            
        } catch (Exception e) {
            log.error("Error parsing Hugging Face response: {}", response, e);
        }
        
        return Optional.empty();
    }

    @Data
    @Builder
    public static class HuggingFaceRequest {
        private String inputs;
        private HuggingFaceParameters parameters;
    }

    @Data
    @Builder
    public static class HuggingFaceParameters {
        private List<String> candidateLabels;
        private boolean multiLabel;
    }

    @Data
    public static class HuggingFaceResponse {
        private List<String> labels;
        private List<Double> scores;
    }

    @Data
    public static class TransactionCategorizationRequest {
        private String description;
        private BigDecimal amount;
        private CategoryType type;
    }
} 