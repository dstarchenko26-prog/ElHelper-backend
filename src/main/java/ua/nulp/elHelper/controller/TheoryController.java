package ua.nulp.elHelper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ua.nulp.elHelper.service.TheoryService;
import ua.nulp.elHelper.service.dto.wiki.TheoryRequest;
import ua.nulp.elHelper.service.dto.wiki.TheoryResponse;

import java.util.List;

@RestController
@RequestMapping("/api/theory")
@RequiredArgsConstructor
public class TheoryController {

    private final TheoryService theoryService;

    @GetMapping("/search")
    public ResponseEntity<List<TheoryResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "uk") String lang) {
        return ResponseEntity.ok(theoryService.search(query, categoryId, sort, lang));
    }

    @GetMapping("/byAdmin")
    public ResponseEntity<List<TheoryResponse>> getByAdmin(Authentication authentication) {
        return ResponseEntity.ok(theoryService.getByAdmin(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TheoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(theoryService.getById(id));
    }

    @PostMapping("/create")
    public ResponseEntity<TheoryResponse> create(
            Authentication authentication,
            @RequestBody TheoryRequest request) {
        return new ResponseEntity<>(theoryService.create(authentication.getName(), request), HttpStatus.CREATED);
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<TheoryResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody TheoryRequest request) {
        return ResponseEntity.ok(theoryService.update(authentication.getName(), id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long id) {
        theoryService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}