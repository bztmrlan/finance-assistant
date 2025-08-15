package com.github.bztmrlan.financeassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleGeminiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String modelName = "gemini-1.5-flash";

    @Value("${gemini.max-tokens:2048}")
    private Integer maxTokens = 2048;

    @Value("${gemini.temperature:0.7}")
    private Double temperature = 0.7;

    @Value("${gemini.top-p:0.9}")
    private Double topP = 0.9;

    @Value("${gemini.top-k:40}")
    private Integer topK = 40;


    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";


    public String generateFinancialInsight(String prompt, Map<String, Object> financialData) {
        try {
            log.info("Generating financial insight using Google Gemini model: {}", modelName);
            String completePrompt = buildCompletePrompt(prompt, financialData);
            Map<String, Object> requestBody = createGeminiRequest(completePrompt);

            String response = callGeminiAPI(requestBody);
            
            if (response != null && !response.trim().isEmpty()) {
                log.info("Successfully generated insight using Gemini, length: {}", response.length());
                return response;
            } else {
                log.warn("Gemini API returned empty response");
                return null;
            }

        } catch (Exception e) {
            log.error("Error generating insight with Google Gemini: {}", e.getMessage(), e);
            return null;
        }
    }


    private Map<String, Object> createGeminiRequest(String prompt) {
        Map<String, Object> request = new HashMap<>();
        

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(Map.of("text", prompt)));
        request.put("contents", List.of(content));
        

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("maxOutputTokens", maxTokens);
        generationConfig.put("temperature", temperature);
        generationConfig.put("topP", topP);
        generationConfig.put("topK", topK);
        request.put("generationConfig", generationConfig);

        List<Map<String, Object>> safetySettings = List.of(
            Map.of(
                "category", "HARM_CATEGORY_HARASSMENT",
                "threshold", "BLOCK_MEDIUM_AND_ABOVE"
            ),
            Map.of(
                "category", "HARM_CATEGORY_HATE_SPEECH",
                "threshold", "BLOCK_MEDIUM_AND_ABOVE"
            ),
            Map.of(
                "category", "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                "threshold", "BLOCK_MEDIUM_AND_ABOVE"
            ),
            Map.of(
                "category", "HARM_CATEGORY_DANGEROUS_CONTENT",
                "threshold", "BLOCK_MEDIUM_AND_ABOVE"
            )
        );
        request.put("safetySettings", safetySettings);
        
        return request;
    }


    private String callGeminiAPI(Map<String, Object> requestBody) {
        try {
            String url = GEMINI_API_URL.replace("{model}", modelName);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            

            url += "?key=" + apiKey;
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            

            try {
                Object contentsObj = requestBody.get("contents");
                if (contentsObj instanceof List && !((List<?>) contentsObj).isEmpty()) {
                    Object firstContent = ((List<?>) contentsObj).get(0);
                    if (firstContent instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> content = (Map<String, Object>) firstContent;
                        Object partsObj = content.get("parts");
                        if (partsObj instanceof List && !((List<?>) partsObj).isEmpty()) {
                            Object firstPart = ((List<?>) partsObj).get(0);
                            if (firstPart instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> part = (Map<String, Object>) firstPart;
                                Object textObj = part.get("text");
                                if (textObj instanceof String) {
                                    log.info("Calling Gemini API with prompt length: {}", ((String) textObj).length());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.info("Calling Gemini API (prompt length logging failed: {})", e.getMessage());
            }
            
            log.info("Making request to Gemini API: {}", url);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Gemini API call successful with status: {}", response.getStatusCode());

                String responseBody = response.getBody().trim();
                if (responseBody.isEmpty()) {
                    log.warn("Gemini API returned empty response body");
                    return null;
                }
                
                return parseGeminiResponse(responseBody);
            } else {
                log.error("Gemini API call failed with status: {} and body: {}", 
                    response.getStatusCode(), 
                    response.getBody() != null ? response.getBody().substring(0, Math.min(200, response.getBody().length())) : "null");
                return null;
            }
            
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);

            if (e.getMessage() != null) {
                if (e.getMessage().contains("401")) {
                    log.error("Authentication failed - check your API key");
                } else if (e.getMessage().contains("403")) {
                    log.error("Access forbidden - check API permissions");
                } else if (e.getMessage().contains("429")) {
                    log.error("Rate limit exceeded - try again later");
                } else if (e.getMessage().contains("500")) {
                    log.error("Gemini service error - try again later");
                } else if (e.getMessage().contains("timeout")) {
                    log.error("Request timeout - check network connection");
                }
            }
            
            return null;
        }
    }

    private String parseGeminiResponse(String responseBody) {
        try {
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.warn("Response body is null or empty");
                return null;
            }
            
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            if (response == null) {
                log.warn("Failed to parse response body to Map");
                return null;
            }
            

            log.debug("Gemini response keys: {}", response.keySet());
            

            if (response.containsKey("error")) {
                Object errorObj = response.get("error");
                if (errorObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> error = (Map<String, Object>) errorObj;
                    String errorMessage = error.containsKey("message") ? String.valueOf(error.get("message")) : "Unknown error";
                    log.error("Gemini API returned error: {}", errorMessage);
                    return null;
                }
            }
            

            String result = parseStandardResponse(response);
            if (result != null) {
                return result;
            }
            

            result = parseAlternativeResponse(response);
            if (result != null) {
                return result;
            }
            

            log.warn("Unexpected Gemini response structure: {}", responseBody.substring(0, Math.min(200, responseBody.length())));
            return null;
            
        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage(), e);
            return null;
        }
    }
    

    private String parseStandardResponse(Map<String, Object> response) {
        try {
            if (response.containsKey("candidates") && response.get("candidates") instanceof List) {
                List<?> candidates = (List<?>) response.get("candidates");
                
                if (!candidates.isEmpty()) {
                    Object firstCandidateObj = candidates.get(0);
                    if (!(firstCandidateObj instanceof Map)) {
                        log.debug("First candidate is not a Map: {}", firstCandidateObj.getClass().getSimpleName());
                        return null;
                    }
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> firstCandidate = (Map<String, Object>) firstCandidateObj;
                    
                    if (firstCandidate.containsKey("content")) {
                        Object contentObj = firstCandidate.get("content");
                        if (!(contentObj instanceof Map)) {
                            log.debug("Content is not a Map: {}", contentObj.getClass().getSimpleName());
                            return null;
                        }
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Object> content = (Map<String, Object>) contentObj;
                        
                        if (content.containsKey("parts") && content.get("parts") instanceof List) {
                            List<?> parts = (List<?>) content.get("parts");
                            
                            if (!parts.isEmpty()) {
                                Object firstPartObj = parts.get(0);
                                if (!(firstPartObj instanceof Map)) {
                                    log.debug("First part is not a Map: {}", firstPartObj.getClass().getSimpleName());
                                    return null;
                                }
                                
                                @SuppressWarnings("unchecked")
                                Map<String, Object> firstPart = (Map<String, Object>) firstPartObj;
                                
                                if (firstPart.containsKey("text")) {
                                    Object textObj = firstPart.get("text");
                                    if (textObj instanceof String) {
                                        return (String) textObj;
                                    } else {
                                        log.debug("Text is not a String: {}", textObj.getClass().getSimpleName());
                                        return null;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.debug("Error parsing standard response: {}", e.getMessage());
            return null;
        }
    }

    private String parseAlternativeResponse(Map<String, Object> response) {
        try {

            if (response.containsKey("text")) {
                Object textObj = response.get("text");
                if (textObj instanceof String) {
                    return (String) textObj;
                }
            }

            if (response.containsKey("responseText")) {
                Object textObj = response.get("responseText");
                if (textObj instanceof String) {
                    return (String) textObj;
                }
            }

            if (response.containsKey("content")) {
                Object contentObj = response.get("content");
                if (contentObj instanceof String) {
                    return (String) contentObj;
                }
            }
            
            return null;
        } catch (Exception e) {
            log.debug("Error parsing alternative response: {}", e.getMessage());
            return null;
        }
    }


    private String buildCompletePrompt(String userQuestion, Map<String, Object> financialData) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert financial advisor AI assistant. Analyze the following financial data and provide comprehensive, actionable insights based on the user's question.\n\n");
        prompt.append("USER QUESTION: ").append(userQuestion).append("\n\n");
        
        prompt.append("FINANCIAL DATA ANALYSIS:\n");
        prompt.append("======================\n\n");
        

        if (financialData.containsKey("totalTransactions")) {
            Integer transactionCount = (Integer) financialData.get("totalTransactions");
            prompt.append("📊 TRANSACTION OVERVIEW:\n");
            prompt.append("Total Transactions: ").append(transactionCount).append("\n");
            prompt.append("Time Period: ").append(financialData.get("timePeriod")).append("\n\n");
        }
        

        if (financialData.containsKey("spending")) {
            Map<String, Object> spending = (Map<String, Object>) financialData.get("spending");
            prompt.append("💰 SPENDING ANALYSIS:\n");
            if (spending.containsKey("totalSpent")) {
                prompt.append("Total Spent: $").append(spending.get("totalSpent")).append("\n");
            }
            if (spending.containsKey("topCategory")) {
                prompt.append("Top Spending Category: ").append(spending.get("topCategory")).append("\n");
            }
            if (spending.containsKey("categoryBreakdown")) {
                prompt.append("Category Breakdown: ").append(spending.get("categoryBreakdown")).append("\n");
            }
            prompt.append("\n");
        }
        

        if (financialData.containsKey("income")) {
            Map<String, Object> income = (Map<String, Object>) financialData.get("income");
            prompt.append("💵 INCOME ANALYSIS:\n");
            if (income.containsKey("totalIncome")) {
                prompt.append("Total Income: $").append(income.get("totalIncome")).append("\n");
            }
            prompt.append("\n");
        }

        if (financialData.containsKey("categories")) {
            Map<String, Object> categories = (Map<String, Object>) financialData.get("categories");
            prompt.append("🏷️ CATEGORY ANALYSIS:\n");
            if (categories.containsKey("categoryCount")) {
                prompt.append("Categories Used: ").append(categories.get("categoryCount")).append("\n");
            }
            prompt.append("\n");
        }

        if (financialData.containsKey("budgets")) {
            List<Map<String, Object>> budgets = (List<Map<String, Object>>) financialData.get("budgets");
            prompt.append("📋 BUDGET INFORMATION:\n");
            prompt.append("Active Budgets: ").append(budgets.size()).append("\n");
            prompt.append("\n");
        }
        

        if (financialData.containsKey("goals")) {
            List<Map<String, Object>> goals = (List<Map<String, Object>>) financialData.get("goals");
            prompt.append("🎯 GOAL INFORMATION:\n");
            prompt.append("Active Goals: ").append(goals.size()).append("\n");
            prompt.append("\n");
        }

        if (financialData.containsKey("transactions")) {
            List<Map<String, Object>> transactions = (List<Map<String, Object>>) financialData.get("transactions");
            prompt.append("📝 SAMPLE TRANSACTIONS (showing first 8):\n");
            transactions.stream().limit(8).forEach(tx -> {
                prompt.append("- ").append(tx.get("date")).append(": $")
                      .append(tx.get("amount")).append(" ").append(tx.get("type"))
                      .append(" - ").append(tx.get("description"));
                if (tx.containsKey("category")) {
                    prompt.append(" (").append(tx.get("category")).append(")");
                }
                prompt.append("\n");
            });
            prompt.append("\n");
        }
        
        prompt.append("ANALYSIS REQUIREMENTS:\n");
        prompt.append("====================\n");
        prompt.append("1. Provide specific, actionable financial insights based on the data\n");
        prompt.append("2. Include relevant numbers, percentages, and trends when available\n");
        prompt.append("3. Identify areas of concern and opportunities for improvement\n");
        prompt.append("4. Suggest concrete next steps and recommendations\n");
        prompt.append("5. Keep the response professional but conversational\n");
        prompt.append("6. Focus on the specific time period and user question\n");
        prompt.append("7. If data is limited, explain why and suggest alternatives\n\n");
        
        prompt.append("RESPONSE FORMAT:\n");
        prompt.append("===============\n");
        prompt.append("Provide a structured financial analysis with:\n");
        prompt.append("- Executive Summary\n");
        prompt.append("- Key Findings\n");
        prompt.append("- Recommendations\n");
        prompt.append("- Action Items\n\n");
        
        prompt.append("Please generate a comprehensive financial insight analysis now:\n\n");
        
        return prompt.toString();
    }

    public boolean testConnection() {
        try {
            log.info("Testing Google Gemini API connection with model: {}", modelName);
            
            String testPrompt = "Generate a brief financial insight: Hello, this is a test message for financial insights.";
            Map<String, Object> testData = Map.of(
                "totalTransactions", 5,
                "timePeriod", "last_7_days",
                "spending", Map.of("totalSpent", 150.00),
                "income", Map.of("totalIncome", 500.00)
            );
            
            String response = generateFinancialInsight(testPrompt, testData);
            
            if (response != null && !response.trim().isEmpty()) {
                log.info("Google Gemini API connection test successful");
                return true;
            } else {
                log.warn("Google Gemini API returned empty response");
                return false;
            }
            
        } catch (Exception e) {
            log.error("Google Gemini connection test failed: {}", e.getMessage(), e);
            return false;
        }
    }


    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            boolean connectionWorking = testConnection();
            status.put("status", connectionWorking ? "OK" : "FAILED");
            status.put("model", modelName);
            status.put("maxTokens", maxTokens);
            status.put("temperature", temperature);
            status.put("apiUrl", GEMINI_API_URL.replace("{model}", modelName));
            status.put("timestamp", java.time.Instant.now().toString());
            
        } catch (Exception e) {
            log.error("Error getting Gemini service status: {}", e.getMessage(), e);
            status.put("status", "ERROR");
            status.put("error", e.getMessage());
        }
        
        return status;
    }
} 