package ua.nulp.elHelper.service.dto.auth;

import lombok.Data;

@Data
public class SPRequest {
    private String email;
    private String code;
}
