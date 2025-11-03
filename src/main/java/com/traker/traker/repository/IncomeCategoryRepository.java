package com.traker.traker.repository;

import com.traker.traker.api.DefaultRepository;
import com.traker.traker.entity.IncomeCategory;
import com.traker.traker.entity.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeCategoryRepository extends DefaultRepository<IncomeCategory, Long> {

    List<IncomeCategory> findByUser(User user);

    Optional<IncomeCategory> findByIdAndUser(Long id, User user);

    Optional<IncomeCategory> findByUserAndNameIgnoreCase(User user, String name);
}
