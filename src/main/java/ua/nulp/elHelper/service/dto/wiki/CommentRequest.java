package ua.nulp.elHelper.service.dto.wiki;

import lombok.Data;

@Data
public class CommentRequest {
    private Long articleId;
    private String text;
    private Long parentId;
}
