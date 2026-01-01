package ua.nulp.elHelper.service.dto.calculation.calculation;

import lombok.Data;
import java.util.Map;

@Data
public class CalculationRequest {
    private Long projectId;
    private Long formulaId;

    private String name;

    private Map<String, Double> inputs;

    private Map<String, String> inputUnits;
}
