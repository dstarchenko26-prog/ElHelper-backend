package ua.nulp.elHelper.service.dto.auth;

import lombok.Data;

@Data
public class ResendCTRequest {
    private String email;
    private String token;
}
