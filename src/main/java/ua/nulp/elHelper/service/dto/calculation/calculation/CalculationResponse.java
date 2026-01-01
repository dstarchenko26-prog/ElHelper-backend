package ua.nulp.elHelper.service.dto.calculation.calculation;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class CalculationResponse {
    private Long id;
    private String name;

    private Long formulaId;
    private Long categoryId;

    private Map<String, String> formulaName;
    private Map<String, String> categoryName;

    private Map<String, Double> inputs;
    private Map<String, String> inputUnits;

    private Map<String, Double> results;

    private Instant createdAt;

    private Map<String, Double> standardizedResults;
}
