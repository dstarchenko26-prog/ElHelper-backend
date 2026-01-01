package ua.nulp.elHelper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.nulp.elHelper.entity.user.AdminRequest;
import ua.nulp.elHelper.entity.Enums.RequestStatus;
import ua.nulp.elHelper.entity.user.User;

import java.util.Optional;

@Repository
public interface AdminRequestRepo extends JpaRepository<AdminRequest, Long> {
    Optional<AdminRequest> findByUserIdAndStatus(Long userId, RequestStatus status);

    Optional<AdminRequest> findTopByUserAndStatusNotOrderByCreatedAtDesc(User user, RequestStatus status);
}