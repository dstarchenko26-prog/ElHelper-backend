package ua.nulp.elHelper.service.dto.calculation.formula;

import lombok.Data;
import ua.nulp.elHelper.entity.calculation.Formula;

import java.util.List;
import java.util.Map;

@Data
public class CreateFormula {
    private Map<String, String> names;
    private Map<String, String> descriptions;
    private List<Formula.FormulaScript> scripts;
    private List<Formula.FormulaParam> parameters;
    private Long categoryId;
    private String schemeUrl;
}