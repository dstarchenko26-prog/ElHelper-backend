package ua.nulp.elHelper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.nulp.elHelper.entity.user.User;
import ua.nulp.elHelper.entity.wiki.Comment;

import java.util.List;

@Repository
public interface CommentRepo extends JpaRepository<Comment, Long> {
    int countByAuthor(User author);

    List<Comment> findAllByArticleIdOrderByCreatedAtAsc(Long articleId);
}