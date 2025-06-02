package com.traker.traker.repository;

import com.traker.traker.api.DefaultRepository;
import com.traker.traker.entity.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface UserRepository extends DefaultRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
