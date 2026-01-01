package ua.nulp.elHelper.service.dto.wiki;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class TheoryRequest {
    private Map<String, String> title;
    private Map<String, String> content;
    private Long categoryId;
    private List<String> tags;
}
