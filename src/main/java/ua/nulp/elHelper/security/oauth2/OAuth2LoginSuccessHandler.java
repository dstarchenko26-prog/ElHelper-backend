package ua.nulp.elHelper.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import ua.nulp.elHelper.entity.Enums.AuthProvider;
import ua.nulp.elHelper.entity.Enums.UserRole;
import ua.nulp.elHelper.entity.Enums.UserStatus;
import ua.nulp.elHelper.entity.user.User;
import ua.nulp.elHelper.repository.UserRepo;
import ua.nulp.elHelper.security.JwtService;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepo userRepo;

    @Value("${app.front-url}")
    private String URL;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");
        String avatarUrl = oAuth2User.getAttribute("picture");
        String providerId = oAuth2User.getAttribute("sub");

        Optional<User> userOptional = userRepo.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            user.setProvider(AuthProvider.GOOGLE);
            user.setProviderId(providerId);
            user.setAvatarUrl(avatarUrl);
            userRepo.save(user);
        } else {
            user = new User();
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setAvatarUrl(avatarUrl);
            user.setRole(UserRole.USER);
            user.setStatus(UserStatus.ACTIVE);
            user.setProvider(AuthProvider.GOOGLE);
            user.setProviderId(providerId);
            userRepo.save(user);
        }

        String token = jwtService.generateToken(user);

        String targetUrl = UriComponentsBuilder.fromUriString(URL + "/auth/oauth2")
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
