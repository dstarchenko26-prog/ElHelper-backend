package ua.nulp.elHelper.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ua.nulp.elHelper.entity.calculation.Formula;
import ua.nulp.elHelper.repository.CategoryRepo;
import ua.nulp.elHelper.repository.FormulaRepo;
import ua.nulp.elHelper.repository.UserRepo;
import ua.nulp.elHelper.service.dto.calculation.formula.CreateFormula;
import ua.nulp.elHelper.service.dto.calculation.formula.FormulaResponse;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormulaService {
    private final FormulaRepo formulaRepository;
    private final CategoryRepo categoryRepository;
    private final UserRepo userRepository;
    private final FileService fileService;

    @Transactional(readOnly = true)
    public List<FormulaResponse> getAll() {
        return formulaRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public FormulaResponse getById(Long id) {
        var formula = formulaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formula not found"));
        return mapToDTO(formula);
    }

    public List<FormulaResponse> getByAuthor(Long authorId) {
        return formulaRepository.findAllByAuthorId(authorId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public FormulaResponse create(CreateFormula dto, String email) {
        var author = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        var category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        var formula = Formula.builder()
                .author(author)
                .names(dto.getNames())
                .descriptions(dto.getDescriptions())
                .scripts(dto.getScripts())
                .parameters(dto.getParameters())
                .category(category)
                .schemeUrl(dto.getSchemeUrl())
                .build();

        return mapToDTO(formulaRepository.save(formula));
    }

    @Transactional
    public FormulaResponse update(Long id, String email, CreateFormula dto) {
        var formula = formulaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formula not found"));

        if (!formula.getAuthor().getEmail().equals(email)) {
            throw new AccessDeniedException("This is not your formula");
        }

        if (dto.getNames() != null) {
            formula.setNames(dto.getNames());
        }
        if (dto.getDescriptions() != null) {
            formula.setDescriptions(dto.getDescriptions());
        }
        if (dto.getScripts() != null) {
            formula.setScripts(dto.getScripts());
        }
        if (dto.getParameters() != null) {
            formula.setParameters(dto.getParameters());
        }
        if (dto.getSchemeUrl() != null) {
            formula.setSchemeUrl(dto.getSchemeUrl());
        }
        if (dto.getCategoryId() != null) {
            var category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            formula.setCategory(category);
        }

        return mapToDTO(formulaRepository.save(formula));
    }

    private FormulaResponse mapToDTO(Formula f) {
        return FormulaResponse.builder()
                .id(f.getId())
                .authorId(f.getAuthor().getId())
                .authorName(f.getAuthor().getFirstName() + " " + f.getAuthor().getLastName()) // або інше поле імені
                .names(f.getNames())
                .descriptions(f.getDescriptions())
                .scripts(f.getScripts())
                .parameters(f.getParameters())
                .categoryId(f.getCategory().getId())
                .categoryName(f.getCategory().getNames())
                .schemeUrl(f.getSchemeUrl())
                .build();
    }

    public String uploadScheme(MultipartFile file) {
        String schemeUrl = fileService.saveFile(file);
        return schemeUrl;
    }
}