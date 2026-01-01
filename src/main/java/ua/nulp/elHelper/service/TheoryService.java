package ua.nulp.elHelper.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ua.nulp.elHelper.entity.common.Category;
import ua.nulp.elHelper.entity.wiki.TheoryArticle;

import ua.nulp.elHelper.repository.CategoryRepo;
import ua.nulp.elHelper.repository.TheoryRepo;
import ua.nulp.elHelper.repository.UserRepo;

import ua.nulp.elHelper.service.dto.wiki.TheoryRequest;
import ua.nulp.elHelper.service.dto.wiki.TheoryResponse;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TheoryService {

    private final TheoryRepo theoryRepository;
    private final CategoryRepo categoryRepository;
    private final UserRepo userRepository;

    @Transactional
    public TheoryResponse create(String email, TheoryRequest request) {
        var category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        var author = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var article = TheoryArticle.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(category)
                .tags(request.getTags())
                .author(author)
                .build();

        return mapToDto(theoryRepository.save(article));
    }

    @Transactional
    public TheoryResponse update(String email, Long id, TheoryRequest request) {
        var article = theoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        if (!article.getAuthor().getEmail().equals(email)) {
            throw new AccessDeniedException("This is not your article");
        }

        if (request.getTitle() != null) article.setTitle(request.getTitle());
        if (request.getContent() != null) article.setContent(request.getContent());
        if (request.getTags() != null) article.setTags(request.getTags());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            article.setCategory(category);
        }

        return mapToDto(theoryRepository.save(article));
    }

    @Transactional
    public void delete(String email, Long id) {
        var article = theoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        if (!article.getAuthor().getEmail().equals(email)) {
            throw new AccessDeniedException("This is not your article");
        }

        theoryRepository.delete(article);
    }

    @Transactional(readOnly = true)
    public TheoryResponse getById(Long id) {
        return theoryRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Article not found"));
    }

    @Transactional(readOnly = true)
    public List<TheoryResponse> getByAdmin(String email) {
        return theoryRepository.findAllByAuthorEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<TheoryResponse> search(String query, Long categoryId, String sort, String lang) {
        List<TheoryArticle> articles;

        if (categoryId != null) {
            articles = theoryRepository.findAllByCategoryId(categoryId);
        } else {
            articles = theoryRepository.findAll();
        }

        Stream<TheoryArticle> stream = articles.stream()
                .filter(a -> query == null || query.isEmpty() ||
                        (a.getTitle().getOrDefault(lang, "").toLowerCase().contains(query.toLowerCase())) ||
                        (a.getTags() != null && a.getTags().contains(query)));

        stream = switch (sort != null ? sort : "newest") {
            case "oldest" ->
                    stream.sorted(Comparator.comparing(TheoryArticle::getCreatedAt));

            case "title-asc" ->
                    stream.sorted(Comparator.comparing(a -> a.getTitle().getOrDefault(lang, "").toLowerCase()));

            case "title-desc" ->
                    stream.sorted((a, b) -> b.getTitle().getOrDefault(lang, "").toLowerCase()
                            .compareTo(a.getTitle().getOrDefault(lang, "").toLowerCase()));

            default -> // "newest"
                    stream.sorted(Comparator.comparing(TheoryArticle::getCreatedAt).reversed());
        };

        return stream.map(this::mapToDto).collect(Collectors.toList());
    }

    private TheoryResponse mapToDto(TheoryArticle article) {
        return TheoryResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .content(article.getContent())
                .categoryId(article.getCategory().getId())
                .categoryName(article.getCategory().getNames())
                .tags(article.getTags())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .authorId(article.getAuthor().getId())
                .authorName(article.getAuthor().getFirstName() + " " + article.getAuthor().getLastName())
                .build();
    }
}