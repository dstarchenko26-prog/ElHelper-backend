package ua.nulp.elHelper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import ua.nulp.elHelper.service.CommentService;

import ua.nulp.elHelper.service.dto.wiki.CommentRequest;
import ua.nulp.elHelper.service.dto.wiki.CommentResponse;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/article/{articleId}")
    public ResponseEntity<List<CommentResponse>> getByArticle(@PathVariable Long articleId) {
        return ResponseEntity.ok(commentService.getCommentsTree(articleId));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(Authentication authentication, @RequestBody CommentRequest request) {
        return ResponseEntity.ok(commentService.create(authentication.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        commentService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
