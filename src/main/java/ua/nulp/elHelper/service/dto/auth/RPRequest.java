package ua.nulp.elHelper.service.dto.auth;

import lombok.Data;

@Data
public class RPRequest {
    private String token;
    private String newPassword;
}
