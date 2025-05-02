package com.traker.traker.repository;

import com.traker.traker.entity.DayLog;
import com.traker.traker.api.DefaultRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DayLogRepository extends DefaultRepository<DayLog, Long> {
    Optional<DayLog> findByDate(LocalDate date);
}