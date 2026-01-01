package ua.nulp.elHelper.service.dto.calculation.formula;

import lombok.Builder;
import lombok.Data;
import ua.nulp.elHelper.entity.calculation.Formula;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class FormulaResponse {
    private Long id;
    private Long authorId;
    private String authorName;
    private Map<String, String> names;
    private Map<String, String> descriptions;
    private List<Formula.FormulaScript> scripts;
    private List<Formula.FormulaParam> parameters;
    private Long categoryId;
    private Map<String, String> categoryName;
    private String schemeUrl;
}
