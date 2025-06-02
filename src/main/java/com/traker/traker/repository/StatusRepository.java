package com.traker.traker.repository;

import com.traker.traker.entity.Status;
import com.traker.traker.api.DefaultRepository;
import com.traker.traker.entity.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatusRepository extends DefaultRepository<Status, Long> {
    Optional<Status> findByName(String name);
    Optional<Status> findByNameAndUser(String name, User user);
    Optional<Status> findByIdAndUser(Long id, User user);
    List<Status> findByUser(User user);
}