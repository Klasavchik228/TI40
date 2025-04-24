package klasavchik.ti.controller;

import jakarta.validation.Valid;
import klasavchik.ti.dto.ProductDescriptionRequest;
import klasavchik.ti.model.GeneratedDescription;
import klasavchik.ti.service.AIService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/product-description")
public class DescriptionGeneratorController {
    private static final Logger logger = LoggerFactory.getLogger(DescriptionGeneratorController.class);

    private final AIService aiService;

    public DescriptionGeneratorController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/generate")
    public String generateDescription(
            @Valid @ModelAttribute("request") ProductDescriptionRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.request", bindingResult);
            redirectAttributes.addFlashAttribute("request", request);
            return "redirect:/product-description/generate";
        }

        try {
            String prompt = buildPrompt(request);
            String description = aiService.generateDescription(prompt);
            if (description.startsWith(prompt)) {
                description = description.substring(prompt.length()).trim();
            }
            GeneratedDescription generatedDescription = new GeneratedDescription(description);

            redirectAttributes.addFlashAttribute("generatedDescription", generatedDescription);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка генерации: " + e.getMessage());
        }

        redirectAttributes.addFlashAttribute("request", request);
        return "redirect:/product-description/generate";
    }


    @GetMapping("/generate")
    public String showForm(Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new ProductDescriptionRequest());
        }
        return "generator-form";
    }


    private String buildPrompt(ProductDescriptionRequest request) {
        return String.format(
                "Напиши %s описание товара '%s'. Характеристики: %s. " +
                        "Формат текста: %s. Акцент на: %s. %s пиши всё на русском, ни какие переводы писать не нужно. Основная цель - описание товара для маркетплейса. Поэтому составляй всё основываясь на описании похожих товаров. Не нужно вводное предложение, сразу пиши описание. Фразы типа:'Вот короткое 3-предложенчное описание часов в формальном стиле для корпоративного сайта:' писать не нужно",
                getLengthDescription(request.getLength()),
                request.getProductName(),
                request.getProductFeatures(),
                getStyleDescription(request.getStyle()),
                request.getFocus(),
                request.isSimplifyTerms() ? "Упрости технические термины." : ""
        );
    }

    private String getLengthDescription(String length) {
        return switch (length) {
            case "short" -> "короткое (3-5 предложений)";
            case "medium" -> "развернутое (7-10 предложений)";
            case "long" -> "подробное (12-15 предложений)";
            default -> "";
        };
    }

    private String getStyleDescription(String style) {
        return switch (style) {
            case "formal" -> "формальный, для корпоративного сайта";
            case "friendly" -> "дружелюбный, для блога";
            case "creative" -> "креативный, с метафорами";
            default -> "нейтральный";
        };
    }
}
