package ua.nulp.elHelper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.nulp.elHelper.entity.calculation.Calculation;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalculationRepo extends JpaRepository<Calculation, Long> {
    List<Calculation> findAllByProjectId(Long projectId);

    Optional<Calculation> findByIdAndProject_User_Email(Long id, String email);
}