package com.traker.traker.repository;

import com.traker.traker.api.DefaultRepository;
import com.traker.traker.entity.IncomeRecord;
import com.traker.traker.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeRecordRepository extends DefaultRepository<IncomeRecord, Long> {

    @Query("""
            SELECT ir FROM IncomeRecord ir
            WHERE ir.user = :user
              AND ir.period >= COALESCE(:fromPeriod, ir.period)
              AND ir.period <= COALESCE(:toPeriod, ir.period)
              AND (
                    ir.incomeDate IS NULL
                    OR ir.incomeDate >= COALESCE(:fromDate, ir.incomeDate)
                )
              AND (
                    ir.incomeDate IS NULL
                    OR ir.incomeDate <= COALESCE(:toDate, ir.incomeDate)
                )
              AND (:categoryIds IS NULL OR ir.category.id IN :categoryIds)
            ORDER BY ir.period ASC, ir.incomeDate ASC, ir.id ASC
            """)
    List<IncomeRecord> findByUserAndFilter(@Param("user") User user,
                                           @Param("fromDate") LocalDate fromDate,
                                           @Param("toDate") LocalDate toDate,
                                           @Param("fromPeriod") LocalDate fromPeriod,
                                           @Param("toPeriod") LocalDate toPeriod,
                                           @Param("categoryIds") List<Long> categoryIds);

    boolean existsByUserAndCategory_Id(User user, Long categoryId);

    Optional<IncomeRecord> findByIdAndUser(Long id, User user);

    @Query("""
            SELECT ir FROM IncomeRecord ir
            WHERE ir.user = :user AND ir.id IN :ids
            """)
    List<IncomeRecord> findByUserAndIdIn(@Param("user") User user,
                                         @Param("ids") List<Long> ids);
}
