package ua.nulp.elHelper.service.dto.user;

import lombok.Data;

@Data
public class ChangeProfileDto {
    private String firstName;
    private String lastName;
    private String bio;
}
