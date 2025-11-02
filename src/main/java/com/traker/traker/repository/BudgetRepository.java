package com.traker.traker.repository;

import com.traker.traker.api.DefaultRepository;
import com.traker.traker.entity.Budget;
import com.traker.traker.entity.User;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends DefaultRepository<Budget, Long> {

    Optional<Budget> findByUserAndPeriod(User user, LocalDate period);

    List<Budget> findByUser(User user);

    List<Budget> findByUserAndPeriodBetween(User user, LocalDate from, LocalDate to);
}
