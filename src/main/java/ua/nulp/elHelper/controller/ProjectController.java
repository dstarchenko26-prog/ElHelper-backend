package ua.nulp.elHelper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ua.nulp.elHelper.service.BomService;
import ua.nulp.elHelper.service.ProjectService;
import ua.nulp.elHelper.service.dto.calculation.project.ProjectRequest;
import ua.nulp.elHelper.service.dto.calculation.project.ProjectResponse;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final BomService bomService;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(Authentication authentication, @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(projectService.create(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllActive(Authentication authentication) {
        return ResponseEntity.ok(projectService.getMyActiveProjects(authentication.getName()));
    }

    @GetMapping("/archive")
    public ResponseEntity<List<ProjectResponse>> getAllHistory(Authentication authentication) {
        return ResponseEntity.ok(projectService.getMyProjects(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getOne(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(projectService.getById(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(Authentication authentication, @PathVariable Long id, @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(projectService.update(id, authentication.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        projectService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/version")
    public ResponseEntity<ProjectResponse> createNewVersion(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(projectService.createNextVersion(id, authentication.getName()));
    }

    @GetMapping("/{id}/bom")
    public ResponseEntity<byte[]> downloadBom(Authentication authentication, @PathVariable Long id) {
        byte[] csvContent = bomService.generateCsvBom(id, authentication.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"project_" + id + "_bom.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent);
    }
}