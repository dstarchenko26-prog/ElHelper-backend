package ua.nulp.elHelper.service.dto.user;

import lombok.Data;

import java.time.Instant;

@Data
public class UserProfileDto {
    private String email;
    private String firstName;
    private String lastName;
    private String bio;
    private String avatarUrl;
    private String role;
    private Instant registeredAt;

    private int projectsCount;
    private int formulasCount;
    private int articlesCount;
    private int commentsCount;
}
