package com.traker.traker.repository;

import com.traker.traker.api.DefaultRepository;
import com.traker.traker.entity.Role;

import java.util.Optional;

/**
 * Репозиторий для работы с ролями.
 */
public interface RoleRepository extends DefaultRepository<Role, Long> {

    /** Поиск роли по имени. */
    Optional<Role> findByName(String name);
}