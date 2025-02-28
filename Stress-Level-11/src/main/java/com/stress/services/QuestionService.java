package com.stress.services;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.stress.entity.Question;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class QuestionService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String API_KEY = System.getenv().getOrDefault("OPENROUTER_API_KEY", "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");
    private static final String MODEL = System.getenv().getOrDefault("OPENROUTER_MODEL", "deepseek/deepseek-r1");

    public List<Question> generateQuestions() {
        String prompt = """
            Generate a JSON array containing 15 multiple-choice stress assessment questions:
            - 5 physical stress questions (category: "physical")
            - 5 mental stress questions (category: "mental")
            - 5 job workload stress questions (category: "job")
            - Each question must have exactly 3 options: ["Often", "Sometimes", "Never"]
            
            Example response format:
            [
                {"question": "How often do you experience headaches?", "options": ["Often", "Sometimes", "Never"], "category": "physical"},
                {"question": "How frequently do you feel overwhelmed at work?", "options": ["Often", "Sometimes", "Never"], "category": "mental"}
            ]
            
            Return ONLY the JSON array. Ensure the output is valid JSON and starts with '[' and ends with ']'.
            """;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL);
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 2000); // Increased to handle longer responses
        requestBody.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        System.out.println("API Key: " + API_KEY);
        System.out.println("Model: " + MODEL);
        System.out.println("Request Body: " + requestBody);

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                String jsonResponse = restTemplate.postForObject(
                    "https://openrouter.ai/api/v1/chat/completions",
                    entity,
                    String.class
                );
                System.out.println("Raw Response: " + jsonResponse);
                return parseQuestions(jsonResponse);
            } catch (Exception e) {
                System.err.println("Attempt " + attempt + " failed: " + e.getMessage());
                if (attempt == maxRetries) {
                    throw new RuntimeException("Failed to generate questions after " + maxRetries + " attempts", e);
                }
                try {
                    TimeUnit.SECONDS.sleep(2 * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted during retry delay", ie);
                }
            }
        }
        return Collections.emptyList();
    }

    private List<Question> parseQuestions(String json) {
        try {
            // Parse the entire API response
            Map<String, Object> responseMap = objectMapper.readValue(json, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("No choices found in the response");
            }

            // Extract the content from the first choice's message
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            String generatedText = (String) message.get("content");
            if (generatedText == null || generatedText.isEmpty()) {
                throw new RuntimeException("Empty or null content in the response");
            }

            System.out.println("Extracted Content: " + generatedText);

            // Validate JSON structure
            if (!generatedText.trim().startsWith("[") || !generatedText.trim().endsWith("]")) {
                throw new RuntimeException("Incomplete or malformed JSON response: " + generatedText);
            }

            // Parse the content into a list of questions
            return objectMapper.readValue(
                generatedText,
                new TypeReference<List<Question>>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenRouter response", e);
        }
    }
}