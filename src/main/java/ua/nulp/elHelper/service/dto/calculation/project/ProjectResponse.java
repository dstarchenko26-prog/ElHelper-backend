package ua.nulp.elHelper.service.dto.calculation.project;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private Instant createdAt;
    private int calculationsCount;

    private boolean active;
    private int versionNumber;
    private Long previousVersionId;
}
