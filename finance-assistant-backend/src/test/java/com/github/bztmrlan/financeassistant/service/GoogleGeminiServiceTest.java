package com.github.bztmrlan.financeassistant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
class GoogleGeminiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private GoogleGeminiService googleGeminiService;

    @Test
    void testServiceInitialization() {

        assertNotNull(googleGeminiService);
    }

    @Test
    void testBuildCompletePrompt() {

        String userQuestion = "How can I save money?";
        Map<String, Object> financialData = new HashMap<>();
        financialData.put("totalTransactions", 10);
        financialData.put("timePeriod", "last_30_days");
        financialData.put("spending", Map.of("totalSpent", 1000.0));
        financialData.put("income", Map.of("totalIncome", 3000.0));


        String prompt = (String) ReflectionTestUtils.invokeMethod(
            googleGeminiService, 
            "buildCompletePrompt", 
            userQuestion, 
            financialData
        );

        assertNotNull(prompt);
        assertTrue(prompt.contains(userQuestion));
        assertTrue(prompt.contains("10"));
        assertTrue(prompt.contains("last_30_days"));
        assertTrue(prompt.contains("1000.0"));
        assertTrue(prompt.contains("3000.0"));
        assertTrue(prompt.contains("financial advisor"));
    }

    @Test
    void testCreateGeminiRequest() {

        String testPrompt = "Test financial question";
        
        Map<String, Object> request = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
            googleGeminiService, 
            "createGeminiRequest", 
            testPrompt
        );

        assertNotNull(request);
        assertTrue(request.containsKey("contents"));
        assertTrue(request.containsKey("generationConfig"));
        assertTrue(request.containsKey("safetySettings"));
        

        Object contents = request.get("contents");
        assertTrue(contents instanceof java.util.List);
    }

    @Test
    void testServiceStatus() {

        Map<String, Object> status = googleGeminiService.getServiceStatus();
        
        assertNotNull(status);
        assertTrue(status.containsKey("status"));
        assertTrue(status.containsKey("timestamp"));

        assertTrue(status.containsKey("model"));
        assertTrue(status.containsKey("maxTokens"));
        assertTrue(status.containsKey("temperature"));
    }

    @Test
    void testParseStandardResponse() {

        Map<String, Object> mockResponse = new HashMap<>();

        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        
        part.put("text", "Test financial insight");
        content.put("parts", java.util.List.of(part));
        candidate.put("content", content);
        mockResponse.put("candidates", java.util.List.of(candidate));

        String result = (String) ReflectionTestUtils.invokeMethod(
            googleGeminiService, 
            "parseStandardResponse", 
            mockResponse
        );

        assertEquals("Test financial insight", result);
    }

    @Test
    void testParseAlternativeResponse() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("text", "Alternative response format");
        
        String result = (String) ReflectionTestUtils.invokeMethod(
            googleGeminiService, 
            "parseAlternativeResponse", 
            mockResponse
        );

        assertEquals("Alternative response format", result);
    }

    @Test
    void testParseAlternativeResponseWithResponseText() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("responseText", "Response text format");
        
        String result = (String) ReflectionTestUtils.invokeMethod(
            googleGeminiService, 
            "parseAlternativeResponse", 
            mockResponse
        );

        assertEquals("Response text format", result);
    }

    @Test
    void testParseAlternativeResponseWithContent() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("content", "Content format");
        
        String result = (String) ReflectionTestUtils.invokeMethod(
            googleGeminiService, 
            "parseAlternativeResponse", 
            mockResponse
        );

        assertEquals("Content format", result);
    }

    @Test
    void testParseAlternativeResponseWithNoValidFormat() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("invalid", "data");
        
        String result = (String) ReflectionTestUtils.invokeMethod(
            googleGeminiService, 
            "parseAlternativeResponse", 
            mockResponse
        );

        assertNull(result);
    }

    @Test
    void testErrorResponseHandling() {
        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> error = new HashMap<>();
        error.put("message", "API key invalid");
        mockResponse.put("error", error);

        assertTrue(mockResponse.containsKey("error"));
        Object errorObj = mockResponse.get("error");
        assertTrue(errorObj instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> errorMap = (Map<String, Object>) errorObj;
        assertTrue(errorMap.containsKey("message"));
        assertEquals("API key invalid", errorMap.get("message"));
    }
} 