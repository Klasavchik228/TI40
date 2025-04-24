package klasavchik.ti.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductDescriptionRequest {
    @NotBlank(message = "Название товара обязательно")
    private String productName;

    @NotBlank(message = "Характеристики товара обязательны")
    private String productFeatures;

    private String style = "friendly";
    private String length = "medium";
    private String focus ;
    private boolean simplifyTerms = true;

    public boolean isSimplifyTerms() {
        return simplifyTerms;
    }

    public void setSimplifyTerms(boolean simplifyTerms) {
        this.simplifyTerms = simplifyTerms;
    }
}
