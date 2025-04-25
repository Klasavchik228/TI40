package klasavchik.ti.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openrouter.api.key}")
    private String apiKey;

    public String generateDescription(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "❌ Ошибка: входной промт пустой!";
        }

        String sanitizedPrompt = prompt.replaceAll("[\\p{Cntrl}]", "").trim()
                .replace("\n", " ").replace("\r", " ");

        try {
            String apiUrl = "https://openrouter.ai/api/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("HTTP-Referer", "YOUR_SITE_URL");
            headers.set("X-Title", "YOUR_APP_NAME");

            String requestJson = objectMapper.writeValueAsString(Map.of(
                    "model", "anthropic/claude-2",
                    "messages", Collections.singletonList(Map.of(
                            "role", "user",
                            "content", sanitizedPrompt
                    )),
                    "max_tokens", 700,
                    "temperature", 0.7
            ));

            System.out.println("Отправляем запрос к API OpenRouter...");

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(requestJson, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                System.err.println("Ошибка API: " + response.getStatusCode());
                return "❌ Ошибка API: " + response.getStatusCode();
            }

            return parseGeneratedText(response.getBody());
        } catch (Exception e) {
            System.err.println("Ошибка при генерации описания: " + e.getMessage());
            return "⚠️ Ошибка генерации: " + e.getMessage();
        }
    }

    private String parseGeneratedText(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            // 1. Проверяем наличие ошибки
            if (root.has("error")) {
                String errorMsg = root.path("error").path("message").asText("Неизвестная ошибка API");
                System.err.println("API вернуло ошибку: " + errorMsg);
                return "❌ Ошибка API: " + errorMsg;
            }

            // 2. Пробуем разные варианты извлечения контента
            if (root.has("choices")) {
                JsonNode firstChoice = root.path("choices").get(0);
                if (firstChoice != null && firstChoice.has("message")) {
                    return firstChoice.path("message").path("content").asText("❌ Пустой ответ от API");
                }
            }

            // 3. Альтернативный вариант структуры
            if (root.has("message")) {
                return root.path("message").path("content").asText("❌ Пустой ответ от API");
            }

            System.err.println("Неизвестная структура ответа API: " + root.toPrettyString());
            return "❌ Не удалось обработать ответ API";
        } catch (Exception e) {
            System.err.println("Ошибка парсинга ответа: " + e.getMessage());
            return "❌ Ошибка обработки ответа API";
        }
    }
}