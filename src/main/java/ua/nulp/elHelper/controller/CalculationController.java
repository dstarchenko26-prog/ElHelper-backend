package ua.nulp.elHelper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ua.nulp.elHelper.service.CalculatorService;
import ua.nulp.elHelper.service.dto.calculation.calculation.CalculationRequest;
import ua.nulp.elHelper.service.dto.calculation.calculation.CalculationResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calculations")
@RequiredArgsConstructor
public class CalculationController {

    private final CalculatorService calculatorService;

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testCalculation(@RequestBody CalculationRequest request) {
        return ResponseEntity.ok(calculatorService.calculateTest(request));
    }

    @PostMapping
    public ResponseEntity<CalculationResponse> create(Authentication authentication, @RequestBody CalculationRequest request) {
        return ResponseEntity.ok(calculatorService.calculateAndSave(request, authentication.getName()));
    }

    @GetMapping("{projectId}")
    public ResponseEntity<List<CalculationResponse>> getByProject(Authentication authentication, @PathVariable Long projectId) {
        return ResponseEntity.ok(calculatorService.getProjectCalculations(projectId, authentication.getName()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CalculationResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody CalculationRequest request
    ) {
        return ResponseEntity.ok(calculatorService.updateCalculation(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        calculatorService.deleteCalculation(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}