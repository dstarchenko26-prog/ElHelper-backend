package ua.nulp.elHelper.service.dto.common;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponse {
    private int status;        // 400, 404, 500
    private String error;      // "Not Found", "Bad Request"
    private String message;    // Текст помилки ("Project not found")
    private String path;       // "/api/projects/1"
    private LocalDateTime timestamp;
}
