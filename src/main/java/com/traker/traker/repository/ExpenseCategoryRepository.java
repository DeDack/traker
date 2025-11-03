package com.traker.traker.repository;

import com.traker.traker.api.DefaultRepository;
import com.traker.traker.entity.ExpenseCategory;
import com.traker.traker.entity.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseCategoryRepository extends DefaultRepository<ExpenseCategory, Long> {

    List<ExpenseCategory> findByUser(User user);

    Optional<ExpenseCategory> findByIdAndUser(Long id, User user);

    boolean existsByUserAndNameIgnoreCase(User user, String name);

    Optional<ExpenseCategory> findByUserAndNameIgnoreCase(User user, String name);
}
