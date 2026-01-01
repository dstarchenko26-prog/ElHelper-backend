package ua.nulp.elHelper.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ua.nulp.elHelper.entity.common.Category;
import ua.nulp.elHelper.repository.CategoryRepo;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepo categoryRepository;

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    public Category create(Category category) {
        Map<String, String> names = category.getNames();

        if (names != null) {
            for (String nameValue : names.values()) {
                if (categoryRepository.existsByAnyName(nameValue)) {
                    throw new RuntimeException("Category with name already exists");
                }
            }
        }

        return categoryRepository.save(category);
    }
}
