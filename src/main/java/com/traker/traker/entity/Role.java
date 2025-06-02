package com.traker.traker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

/**
 * Роль пользователя в системе.
 * Реализует GrantedAuthority для интеграции с Spring Security.
 */
@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role implements GrantedAuthority {

    /** Уникальный идентификатор роли. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Название роли (например, USER, MODERATOR, ADMIN). */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Возвращает название роли как authority для Spring Security.
     * @return Название роли.
     */
    @Override
    public String getAuthority() {
        return name;
    }
}