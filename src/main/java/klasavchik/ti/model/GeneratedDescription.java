package klasavchik.ti.model;

import lombok.Data;

@Data
public class GeneratedDescription {
    private String description;

    public GeneratedDescription(String description) {
        this.description = description;
    }
}
