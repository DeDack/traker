package com.traker.traker.repository;

import com.traker.traker.entity.Status;
import com.traker.traker.api.DefaultRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatusRepository extends DefaultRepository<Status, Long> {
    Optional<Status> findByName(String name);
}