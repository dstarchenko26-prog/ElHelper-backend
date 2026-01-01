package ua.nulp.elHelper.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.nulp.elHelper.entity.wiki.Comment;
import ua.nulp.elHelper.repository.CommentRepo;
import ua.nulp.elHelper.repository.TheoryRepo;
import ua.nulp.elHelper.repository.UserRepo;
import ua.nulp.elHelper.service.dto.wiki.CommentRequest;
import ua.nulp.elHelper.service.dto.wiki.CommentResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepo commentRepository;
    private final TheoryRepo theoryRepository;
    private final UserRepo userRepository;

    @Transactional
    public CommentResponse create(String userEmail, CommentRequest request) {
        var author = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var article = theoryRepository.findById(request.getArticleId())
                .orElseThrow(() -> new RuntimeException("Article not found"));

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
        }

        var comment = Comment.builder()
                .text(request.getText())
                .author(author)
                .article(article)
                .parent(parent)
                .build();

        var saved = commentRepository.save(comment);

        return mapToDtoSimple(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsTree(Long articleId) {
        List<Comment> allComments = commentRepository.findAllByArticleIdOrderByCreatedAtAsc(articleId);

        return allComments.stream()
                .filter(c -> c.getParent() == null)
                .map(this::mapToDtoRecursive)
                .collect(Collectors.toList());
    }

    public void delete(Long id, String userEmail) {
        var comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getAuthor().getEmail().equals(userEmail)) {
            throw new RuntimeException("You can only delete your own comments");
        }

        commentRepository.delete(comment);
    }

    private CommentResponse mapToDtoRecursive(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorName(comment.getAuthor().getFirstName() + " " + comment.getAuthor().getLastName())
                .authorId(comment.getAuthor().getId())
                .createdAt(comment.getCreatedAt())
                .articleId(comment.getArticle().getId())
                .replies(comment.getReplies().stream()
                        .map(this::mapToDtoRecursive)
                        .collect(Collectors.toList()))
                .build();
    }

    private CommentResponse mapToDtoSimple(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorName(comment.getAuthor().getFirstName() + " " + comment.getAuthor().getLastName())
                .authorId(comment.getAuthor().getId())
                .articleId(comment.getArticle().getId())
                .createdAt(comment.getCreatedAt())
                .replies(new ArrayList<>())
                .build();
    }
}
