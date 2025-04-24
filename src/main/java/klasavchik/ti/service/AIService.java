package klasavchik.ti.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIService {
    private final RestTemplate restTemplate;

    @Value("${openrouter.api.key}")
    private String apiKey;

    public String generateDescription(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "❌ Ошибка: входной промт пустой!";
        }

        String sanitizedPrompt = prompt.replaceAll("[\\p{Cntrl}]", "").trim();
        sanitizedPrompt = sanitizedPrompt.replace("\n", " ").replace("\r", " ");

        try {
            // 1. Формируем URL и заголовки
            String apiUrl = "https://openrouter.ai/api/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("HTTP-Referer", "YOUR_SITE_URL"); // Замените на ваш URL
            headers.set("X-Title", "YOUR_APP_NAME"); // Замените на название вашего приложения

            // 2. Создаем JSON-тело запроса
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "anthropic/claude-2"); // Можно выбрать другую модель

            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", sanitizedPrompt);

            requestBody.put("messages", new Map[]{message});
            requestBody.put("max_tokens", 1000);
            requestBody.put("temperature", 0.7);

            ObjectMapper objectMapper = new ObjectMapper();
            String requestJson = objectMapper.writeValueAsString(requestBody);

            System.out.println("Очищенный промт: " + sanitizedPrompt);

            // 3. Отправляем запрос
            HttpEntity<String> request = new HttpEntity<>(requestJson, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl,
                    request,
                    String.class
            );

            // 4. Обрабатываем ответ
            if (response.getStatusCode().is2xxSuccessful()) {
                return parseGeneratedText(response.getBody());
            } else {
                throw new RuntimeException("API error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            return "⚠️ Ошибка генерации: " + e.getMessage();
        }
    }

    private String parseGeneratedText(String jsonResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(jsonResponse);

            if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                JsonNode firstChoice = root.get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    return firstChoice.get("message").get("content").asText();
                }
            }
            return "❌ Некорректный ответ от API";
        } catch (Exception e) {
            return "❌ Ошибка обработки ответа: " + e.getMessage();
        }
    }
}