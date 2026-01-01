package ua.nulp.elHelper.entity.calculation;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ua.nulp.elHelper.entity.common.Category;
import ua.nulp.elHelper.entity.user.User;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "formulas")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Formula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, String> names;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> descriptions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<FormulaScript> scripts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<FormulaParam> parameters;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private String schemeUrl;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormulaScript {
        private String target;
        private String expression;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormulaParam {
        private String var;

        private Map<String, String> label;

        private List<UnitDefinition> units;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitDefinition {
        private String name;
        private Double mult;
    }
}