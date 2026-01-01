package ua.nulp.elHelper.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ua.nulp.elHelper.entity.user.User;
import ua.nulp.elHelper.entity.Enums.UserStatus;
import ua.nulp.elHelper.entity.Enums.UserRole;

import ua.nulp.elHelper.entity.token.LoginToken;
import ua.nulp.elHelper.entity.token.PasswordResetToken;
import ua.nulp.elHelper.entity.token.ConfirmationToken;

import ua.nulp.elHelper.repository.UserRepo;
import ua.nulp.elHelper.repository.ConfTokenRepo;
import ua.nulp.elHelper.repository.LoginTokenRepo;
import ua.nulp.elHelper.repository.PassResTokenRepo;

import ua.nulp.elHelper.service.dto.auth.*;

import ua.nulp.elHelper.security.JwtService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepo userRepository;

    private final ConfTokenRepo confirmationTokenRepository;
    private final LoginTokenRepo loginTokenRepository;
    private final PassResTokenRepo passwordResetTokenRepository;

    private final JwtService jwtService;
    private final EmailService emailService;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${app.front-url}")
    private String URL;

    @Value("${app.special-code")
    private String SP_CODE;

    //Реєстрація
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already taken");
        }

        var user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.PENDING);

        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        var confirmationToken = new ConfirmationToken(
                token,
                Instant.now(),
                Instant.now().plus(15, ChronoUnit.MINUTES),
                user
        );

        confirmationTokenRepository.save(confirmationToken);

        String link = URL + "/auth/confirm-email?token=" + token;
        emailService.send(
                request.getEmail(),
                "Підтвердіть реєстрацію",
                buildEmail(request.getFirstName(), link)
        );

        var jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken);
    }

    //Вхід
    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        var jwtToken = jwtService.generateToken(user);

        return new AuthResponse(jwtToken);
    }

    //Підтвердження реєстрації
    @Transactional
    public String confirmToken(String token) {
        var confirmationToken = confirmationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (confirmationToken.getConfirmedAt() != null) {
            throw new RuntimeException("Token used");
        }

        Instant expiredAt = confirmationToken.getExpiresAt();
        if (expiredAt.isBefore(Instant.now())) {
            throw new RuntimeException("Token expired");
        }

        confirmationToken.setConfirmedAt(Instant.now());
        confirmationTokenRepository.save(confirmationToken);

        var user = confirmationToken.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        return "Confirmed";
    }

    //Повторна відправка підтвердження
    @Transactional
    public void resendConfirmToken(ResendCTRequest request) {
        var oldToken = confirmationTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token not found"));

        var user = oldToken.getUser();

        if (!user.getEmail().equalsIgnoreCase(request.getEmail())) {
            throw new RuntimeException("The email does not match the token");
        }

        if (user.isEnabled()) {
            throw new RuntimeException("Account activated");
        }

        confirmationTokenRepository.delete(oldToken);

        String token = UUID.randomUUID().toString();
        var confirmationToken = new ConfirmationToken(
                token,
                Instant.now(),
                Instant.now().plus(15, ChronoUnit.MINUTES),
                user
        );

        confirmationTokenRepository.save(confirmationToken);

        String link = URL + "/auth/confirm-email?token=" + token;
        emailService.send(
                request.getEmail(),
                "Повторний лист підтвердження реєстрації",
                buildEmail(user.getFirstName(), link)
        );
    }

    //Відправка листа входу
    public void sendLinkLogin(LTLinkRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        if (!user.isEnabled()) {
            throw new RuntimeException("Account is not active");
        }

        String token = UUID.randomUUID().toString();
        var loginToken = new LoginToken(
                token,
                Instant.now(),
                Instant.now().plus(10, ChronoUnit.MINUTES),
                user
        );
        loginTokenRepository.save(loginToken);

        String link = URL + "/auth/link-login?token=" + token;
        emailService.send(
                request.getEmail(),
                "Вхід без паролю",
                buildLinkLoginEmail(user.getFirstName(), link)
        );
    }

    //Вхід через лист
    @Transactional
    public AuthResponse loginWithLinkToken(String token) {
        var loginToken = loginTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (loginToken.getUsedAt() != null) {
            throw new RuntimeException("Token used");
        }

        if (loginToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Token expired");
        }

        loginToken.setUsedAt(Instant.now());
        loginTokenRepository.save(loginToken);

        var user = loginToken.getUser();
        String jwtToken = jwtService.generateToken(user);

        return new AuthResponse(jwtToken);
    }

    //Лист збросу паролю
    public void forgotPassword(FPRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        String token = UUID.randomUUID().toString();
        var resetToken = new PasswordResetToken(
                token,
                Instant.now(),
                Instant.now().plus(10, ChronoUnit.MINUTES),
                user
        );
        passwordResetTokenRepository.save(resetToken);

        String link = URL + "/auth/reset-password?token=" + token;
        emailService.send(
                user.getEmail(),
                "Відновлення паролю",
                buildResetPasswordEmail(user.getFirstName(), link)
        );
    }

    //зброс паролю
    @Transactional
    public void resetPassword(RPRequest request) {
        var resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Token expired");
        }

        var user = resetToken.getUser();

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
    }

    //Сервісне
    @Transactional
    public void special(SPRequest request) {
        if (!request.getCode().equals(SP_CODE)) {
            throw new RuntimeException("Code invalid");
        }

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException ("User not found"));

        user.setRole(UserRole.SUPER_ADMIN);
        userRepository.save(user);
    }

    // HTML шаблони листів
    private String buildEmail(String name, String link) {
        return "<p>Привіт, " + name + "!</p>" +
                "<p>Ви (або хтось інший) зареєстрували акаунт на цю поштову скриньку.</p>" +
                "<p>Натисніть на посилання нижче, щоб підтвердити реєстрацію</p>" +
                "<a href=\"" + link + "\">Зареєструватися</a>" +
                "<br><p>Якщо ви цього не робили, просто ігноруйте цей лист.</p>";
    }

    private String buildLinkLoginEmail(String name, String link) {
        return "<p>Привіт, " + name + "!</p>" +
                "<p>Натисніть на посилання нижче, щоб увійти в ElHelper без пароля:</p>" +
                "<a href=\"" + link + "\">Увійти в акаунт</a>" +
                "<br><p>Посилання дійсне 10 хвилин.</p>";
    }

    private String buildResetPasswordEmail(String name, String link) {
        return "<p>Привіт, " + name + "!</p>" +
                "<p>Ви (або хтось інший) подали запит на зміну пароля.</p>" +
                "<p>Натисніть на посилання нижче, щоб встановити новий пароль:</p>" +
                "<a href=\"" + link + "\">Змінити пароль</a>" +
                "<br><p>Якщо ви цього не робили, просто ігноруйте цей лист.</p>";
    }
}