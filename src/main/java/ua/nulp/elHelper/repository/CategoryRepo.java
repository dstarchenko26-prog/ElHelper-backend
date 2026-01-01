package ua.nulp.elHelper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ua.nulp.elHelper.entity.common.Category;

@Repository
public interface CategoryRepo extends JpaRepository<Category, Long> {
    @Query(value = "SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM categories c WHERE EXISTS (SELECT 1 FROM jsonb_each_text(c.names) WHERE value = :name)", nativeQuery = true)
    boolean existsByAnyName(String name);
}