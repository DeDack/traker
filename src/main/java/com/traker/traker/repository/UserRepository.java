package com.traker.traker.repository;

import com.traker.traker.api.DefaultRepository;
import com.traker.traker.entity.User;

import java.util.Optional;

public interface UserRepository extends DefaultRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);
}
