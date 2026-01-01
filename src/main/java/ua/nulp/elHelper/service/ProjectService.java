package ua.nulp.elHelper.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.nulp.elHelper.entity.calculation.Calculation;
import ua.nulp.elHelper.entity.calculation.Project;
import ua.nulp.elHelper.repository.ProjectRepo;
import ua.nulp.elHelper.repository.UserRepo;
import ua.nulp.elHelper.service.dto.calculation.project.ProjectRequest;
import ua.nulp.elHelper.service.dto.calculation.project.ProjectResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepo projectRepository;
    private final UserRepo userRepository;

    public ProjectResponse create(String userEmail, ProjectRequest request) {
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .user(user)
                .active(true)
                .versionNumber(1)
                .build();

        var saved = projectRepository.save(project);
        return mapToDto(saved);
    }

    public List<ProjectResponse> getMyProjects(String userEmail) {
        return projectRepository.findAllByUserEmail(userEmail).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public ProjectResponse getById(Long id, String userEmail) {
        var project = projectRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));
        return mapToDto(project);
    }

    public ProjectResponse update(Long id, String userEmail, ProjectRequest request) {
        var project = projectRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        return mapToDto(projectRepository.save(project));
    }

    public void delete(Long id, String userEmail) {
        var project = projectRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));

        projectRepository.delete(project);
    }

    public List<ProjectResponse> getMyActiveProjects(String userEmail) {
        return projectRepository.findAllByUserEmailAndActiveTrue(userEmail).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // --- НОВИЙ МЕТОД: СТВОРЕННЯ НОВОЇ ВЕРСІЇ ---
    @Transactional
    public ProjectResponse createNextVersion(Long projectId, String userEmail) {
        // 1. Знаходимо поточний проєкт
        Project currentProject = projectRepository.findByIdAndUserEmail(projectId, userEmail)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!currentProject.isActive()) {
            throw new RuntimeException("Не можна створити версію від архівованого проєкту. Виберіть останню версію.");
        }

        // 2. "Архівуємо" поточний
        currentProject.setActive(false);
        projectRepository.save(currentProject);

        // 3. Створюємо нову версію
        Project newVersion = new Project();
        newVersion.setName(currentProject.getName());
        newVersion.setDescription(currentProject.getDescription());
        newVersion.setUser(currentProject.getUser());
        newVersion.setActive(true);
        newVersion.setPreviousVersion(currentProject);

        // --- ВАЖЛИВО: Піднімаємо номер версії ---
        newVersion.setVersionNumber(currentProject.getVersionNumber() + 1);

        // 4. КЛОНУЄМО РОЗРАХУНКИ (Deep Copy)
        List<Calculation> clonedCalculations = new ArrayList<>();
        if (currentProject.getCalculations() != null) {
            for (Calculation oldCalc : currentProject.getCalculations()) {
                Calculation newCalc = new Calculation();
                newCalc.setName(oldCalc.getName());
                newCalc.setFormula(oldCalc.getFormula());

                // Копіюємо Map-и (щоб не було посилань на той самий об'єкт)
                if (oldCalc.getInputs() != null) newCalc.setInputs(new java.util.HashMap<>(oldCalc.getInputs()));
                if (oldCalc.getInputUnits() != null) newCalc.setInputUnits(new java.util.HashMap<>(oldCalc.getInputUnits())); // <--- Додано
                if (oldCalc.getResults() != null) newCalc.setResults(new java.util.HashMap<>(oldCalc.getResults()));     // <--- Виправлено на results

                newCalc.setCreatedAt(Instant.now()); // Дата створення розрахунку в новій версії
                newCalc.setProject(newVersion);

                clonedCalculations.add(newCalc);
            }
        }
        newVersion.setCalculations(clonedCalculations);

        // 5. Зберігаємо нову версію
        Project savedNewVersion = projectRepository.save(newVersion);

        return mapToDto(savedNewVersion);
    }

    private ProjectResponse mapToDto(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .createdAt(project.getCreatedAt())
                .calculationsCount(project.getCalculations() != null ? project.getCalculations().size() : 0)
                .active(project.isActive())
                .versionNumber(project.getVersionNumber()) // <--- Додано
                .previousVersionId(project.getPreviousVersion() != null ? project.getPreviousVersion().getId() : null)
                .build();
    }
}