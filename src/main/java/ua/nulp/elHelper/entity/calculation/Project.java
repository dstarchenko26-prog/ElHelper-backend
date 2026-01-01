package ua.nulp.elHelper.entity.calculation;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import ua.nulp.elHelper.entity.user.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // Додає зручний патерн "Builder" для створення об'єктів
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    @Builder.Default // Щоб Builder ставив true за замовчуванням
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private int versionNumber = 1;

    // Посилання на попередню версію (OneToOne, бо одна версія має лише одну попередню)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prev_version_id")
    private Project previousVersion;

    // Зв'язок з користувачем (Багато проєктів -> Один користувач)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Зв'язок з розрахунками (Один проєкт -> Багато розрахунків)
    // cascade = ALL означає: якщо видалити проєкт, видаляться і всі його розрахунки
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Calculation> calculations = new ArrayList<>();
}
