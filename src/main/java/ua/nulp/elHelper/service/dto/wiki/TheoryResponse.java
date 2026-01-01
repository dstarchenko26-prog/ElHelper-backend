package ua.nulp.elHelper.service.dto.wiki;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class TheoryResponse {
    private Long id;
    private Long authorId;
    private Long categoryId;

    private Map<String, String> title;
    private Map<String, String> content;
    private List<String> tags;

    private String authorName;
    private Map<String, String> categoryName;

    private Instant createdAt;
    private Instant updatedAt;
}