package ua.nulp.elHelper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.nulp.elHelper.entity.calculation.Project;
import ua.nulp.elHelper.entity.user.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepo extends JpaRepository<Project, Long> {

    int countByUser(User user);

    List<Project> findAllByUserEmail(String email);

    Optional<Project> findByIdAndUserEmail(Long id, String email);

    List<Project> findAllByUserEmailAndActiveTrue(String email);
}
