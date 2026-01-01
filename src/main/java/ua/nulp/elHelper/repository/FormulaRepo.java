package ua.nulp.elHelper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import ua.nulp.elHelper.entity.calculation.Formula;
import ua.nulp.elHelper.entity.user.User;

import java.util.List;

@Repository
public interface FormulaRepo extends JpaRepository<Formula, Long> {
    int countByAuthor(User author);

    List<Formula> findAllByAuthorId(Long authorId);

    @Query(value = "SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM formulas f WHERE EXISTS (SELECT 1 FROM jsonb_each_text(f.names) WHERE value = :name)", nativeQuery = true)
    boolean existsByAnyName(String name);
}