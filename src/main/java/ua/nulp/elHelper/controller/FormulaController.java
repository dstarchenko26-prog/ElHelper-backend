package ua.nulp.elHelper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ua.nulp.elHelper.service.FormulaService;
import ua.nulp.elHelper.service.dto.calculation.formula.CreateFormula;
import ua.nulp.elHelper.service.dto.calculation.formula.FormulaResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/formulas")
@RequiredArgsConstructor
public class FormulaController {

    private final FormulaService formulaService;

    @GetMapping
    public ResponseEntity<List<FormulaResponse>> getAll() {
        return ResponseEntity.ok(formulaService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormulaResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(formulaService.getById(id));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<FormulaResponse>> getByAuthor(@PathVariable Long authorId) {
        return ResponseEntity.ok(formulaService.getByAuthor(authorId));
    }

    @PostMapping
    public ResponseEntity<FormulaResponse> create(
            Authentication authentication,
            @RequestBody CreateFormula dto
    ) {
        return ResponseEntity.ok(formulaService.create(dto, authentication.getName()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FormulaResponse> update(
            @PathVariable Long id,
            Authentication authentication,
            @RequestBody CreateFormula dto
    ) {
        return ResponseEntity.ok(formulaService.update(id, authentication.getName(), dto));
    }

    @PostMapping(value = "/scheme", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadScheme(@RequestParam("file") MultipartFile file) {
        String url = formulaService.uploadScheme(file);
        return ResponseEntity.ok(Map.of("schemeUrl", url));
    }
}