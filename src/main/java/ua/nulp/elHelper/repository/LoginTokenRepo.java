package ua.nulp.elHelper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.nulp.elHelper.entity.token.LoginToken;

import java.util.Optional;

@Repository
public interface LoginTokenRepo extends JpaRepository<LoginToken, Long> {
    Optional<LoginToken> findByToken(String token);
}
