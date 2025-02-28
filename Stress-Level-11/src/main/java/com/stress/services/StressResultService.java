
package com.stress.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stress.entity.StressResult;
import com.stress.entity.User;
import com.stress.repository.StressResultRepository;
import com.stress.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class StressResultService {

	 @Autowired
	    private UserRepository userRepository;
	 @Autowired
	    private StressResultRepository stressResultRepository;
	
	private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String API_KEY = System.getenv().getOrDefault("OPENROUTER_API_KEY", "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");
    private static final String MODEL = System.getenv().getOrDefault("OPENROUTER_MODEL", "deepseek/deepseek-r1");
    
    public StressResult calculateStress(StressResult stressResult, User user) {
        // Link the result to the user
        stressResult.setUser(user);

        // Calculate stress percentages and levels
        Map<String, List<Integer>> categoryScores = new HashMap<>();
        stressResult.getResponses().forEach((questionId, score) -> {
            String category = getCategoryFromQuestionId(questionId); // Extract category from question ID
            categoryScores.putIfAbsent(category, new ArrayList<>());
            categoryScores.get(category).add(score);
        });

        // Physical Stress
        calculateCategoryStress(categoryScores, "physical",
                percentage -> stressResult.setPhysicalStressPercentage(percentage),
                level -> stressResult.setPhysicalStressLevel(level));

        // Mental Stress
        calculateCategoryStress(categoryScores, "mental",
                percentage -> stressResult.setMentalStressPercentage(percentage),
                level -> stressResult.setMentalStressLevel(level));

        // Job Stress
        calculateCategoryStress(categoryScores, "job",
                percentage -> stressResult.setJobStressPercentage(percentage),
                level -> stressResult.setJobStressLevel(level));

        // Generate AI-based suggestions
        generateAISuggestion(stressResult);

        // Save the result to the database
        return stressResultRepository.save(stressResult);
    }

    private String getCategoryFromQuestionId(String questionId) {
        return questionId.split("_")[0]; // Example: "physical_1" -> "physical"
    }

    private void calculateCategoryStress(Map<String, List<Integer>> categoryScores, String category,
                                          Consumer<Double> setPercentage, Consumer<String> setLevel) {
        List<Integer> scores = categoryScores.getOrDefault(category, Collections.emptyList());
        if (!scores.isEmpty()) {
            int totalScore = scores.stream().mapToInt(Integer::intValue).sum();
            int numQuestions = scores.size();
            double percentage = (totalScore * 100.0) / (numQuestions * 5); // Assuming max score per question is 5
            String level = getStressLevel(percentage);
            setPercentage.accept(percentage);
            setLevel.accept(level);
        }
    }

    private String getStressLevel(double percentage) {
        if (percentage >= 70) return "High";
        else if (percentage >= 40) return "Medium";
        else return "Low";
    }
    
    
    public List<StressResult> getUserHistory(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return stressResultRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<StressResult> getUserHistoryByDateRange(String email, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Convert startDate and endDate to LocalDateTime
        if (startDate != null && endDate != null) {
            // Convert startDate to LocalDateTime (00:00:00)
            LocalDateTime startDateTime = startDate.atStartOfDay();

            // Convert endDate to LocalDateTime (23:59:59)
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            // Filter results by the date range
            return stressResultRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, startDateTime, endDateTime);
        }

        // If no date range is specified, return all history
        return stressResultRepository.findByUserOrderByCreatedAtDesc(user);
    }

    
    public List<StressResult> userStressResults(Long userId) {
        
        return stressResultRepository.findByUserId(userId);
    }
     
    
   /* ..................................................*/

    public String generateAISuggestion(StressResult stressResult) {
        String prompt = """
            Generate personalized stress management suggestions based on the following stress levels:
            - Physical Stress Level: %s (%.2f%%)
            - Mental Stress Level: %s (%.2f%%)
            - Job Stress Level: %s (%.2f%%)
            
            Provide concise and actionable advice in English for each category.
            Example format:
            [
                {"category": "physical", "suggestion": "Your physical stress is very high. Please take rest and exercise regularly."},
                {"category": "mental", "suggestion": "Your mental stress is moderate. Ensure adequate sleep and practice mindfulness."},
                {"category": "job", "suggestion": "Your job stress is low. Continue working efficiently!"}
            ]
            
            Return ONLY the JSON array. Ensure the output is valid JSON and starts with '[' and ends with ']'.
            """.formatted(
                stressResult.getPhysicalStressLevel(), stressResult.getPhysicalStressPercentage(),
                stressResult.getMentalStressLevel(), stressResult.getMentalStressPercentage(),
                stressResult.getJobStressLevel(), stressResult.getJobStressPercentage()
            );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL);
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 500); // Adjust token limit as needed
        requestBody.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                String jsonResponse = restTemplate.postForObject(
                    "https://openrouter.ai/api/v1/chat/completions",
                    entity,
                    String.class
                );

                // Log the raw API response for debugging
                System.out.println("API Response (Attempt " + attempt + "): " + jsonResponse);

                if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                    throw new RuntimeException("Empty API response");
                }

                parseSuggestions(jsonResponse, stressResult); // Pass the StressResult object
                return "Suggestions generated successfully.";
            } catch (Exception e) {
                System.err.println("Attempt " + attempt + " failed: " + e.getMessage());
                if (attempt == maxRetries) {
                    // Provide default suggestions if all attempts fail
                    stressResult.setPhysicalSuggestion("Default physical suggestion: Take regular breaks and exercise daily.");
                    stressResult.setMentalSuggestion("Default mental suggestion: Practice mindfulness and ensure adequate sleep.");
                    stressResult.setJobSuggestion("Default job suggestion: Prioritize tasks and maintain a healthy work-life balance.");
                    return "Default suggestions provided due to API failure.";
                }
                try {
                    TimeUnit.SECONDS.sleep(2 * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted during retry delay", ie);
                }
            }
        }
        return "No suggestions available.";
    }

    private void parseSuggestions(String json, StressResult stressResult) {
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

            if (generatedText == null || generatedText.trim().isEmpty()) {
                throw new RuntimeException("Empty or null content in the response");
            }

            System.out.println("Extracted Content: " + generatedText);

            // Validate JSON structure
            if (!generatedText.trim().startsWith("[") || !generatedText.trim().endsWith("]")) {
                throw new RuntimeException("Incomplete or malformed JSON response: " + generatedText);
            }

            // Parse the content into a list of suggestions
            List<Map<String, String>> suggestions = objectMapper.readValue(
                generatedText,
                new TypeReference<List<Map<String, String>>>() {}
            );

            // Assign suggestions to the StressResult object
            for (Map<String, String> suggestion : suggestions) {
                String category = suggestion.get("category");
                String text = suggestion.get("suggestion");
                switch (category) {
                    case "physical":
                        stressResult.setPhysicalSuggestion(text);
                        break;
                    case "mental":
                        stressResult.setMentalSuggestion(text);
                        break;
                    case "job":
                        stressResult.setJobSuggestion(text);
                        break;
                    default:
                        throw new RuntimeException("Unknown category: " + category);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenRouter response", e);
        }
    }
    
    
    }