package ua.nulp.elHelper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.nulp.elHelper.entity.user.User;
import ua.nulp.elHelper.entity.wiki.TheoryArticle;

import java.util.List;

@Repository
public interface TheoryRepo extends JpaRepository<TheoryArticle, Long>, JpaSpecificationExecutor<TheoryArticle> {
    int countByAuthor(User author);

    List<TheoryArticle> findAllByAuthorEmailOrderByCreatedAtDesc(String email);

    List<TheoryArticle> findAllByCategoryId(Long categoryId);

    @Query(value = """
        SELECT * FROM theory_articles a 
        WHERE (a.title ->> :lang ILIKE %:keyword%) 
        OR EXISTS (
            SELECT 1 FROM jsonb_array_elements_text(a.tags) AS t 
            WHERE t ILIKE %:keyword%
        )
        ORDER BY a.created_at DESC
        """, nativeQuery = true)
    List<TheoryArticle> searchByKeywordInLang(
            @Param("keyword") String keyword,
            @Param("lang") String lang
    );

    @Query(value = "SELECT * FROM theory_articles WHERE jsonb_exists(tags, :tag)", nativeQuery = true)
    List<TheoryArticle> findAllByTag(@Param("tag") String tag);
}