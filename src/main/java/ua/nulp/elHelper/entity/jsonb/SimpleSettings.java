package ua.nulp.elHelper.entity.jsonb;

import lombok.Data;
import java.io.Serializable;

@Data
public class SimpleSettings implements Serializable {

    private String theme = "light";
    private String language = "uk";
}
