package ua.nulp.elHelper.service.dto.wiki;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private Long authorId;
    private Long articleId;
    private String text;
    private String authorName;
    private Instant createdAt;

    private List<CommentResponse> replies;
}
