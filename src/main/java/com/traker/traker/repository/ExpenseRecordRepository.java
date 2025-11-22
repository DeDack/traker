package com.traker.traker.repository;

import com.traker.traker.api.DefaultRepository;
import com.traker.traker.entity.ExpenseRecord;
import com.traker.traker.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRecordRepository extends DefaultRepository<ExpenseRecord, Long> {

    @Query("""
            SELECT er FROM ExpenseRecord er
            WHERE er.user = :user
              AND er.period >= COALESCE(:fromPeriod, er.period)
              AND er.period <= COALESCE(:toPeriod, er.period)
              AND (
                    er.expenseDate IS NULL
                    OR er.expenseDate >= COALESCE(:fromDate, er.expenseDate)
                )
              AND (
                    er.expenseDate IS NULL
                    OR er.expenseDate <= COALESCE(:toDate, er.expenseDate)
                )
              AND (:categoryIds IS NULL OR er.category.id IN :categoryIds)
            ORDER BY er.period ASC, er.expenseDate ASC, er.id ASC
            """)
    List<ExpenseRecord> findByUserAndFilter(@Param("user") User user,
                                            @Param("fromDate") LocalDate fromDate,
                                            @Param("toDate") LocalDate toDate,
                                            @Param("fromPeriod") LocalDate fromPeriod,
                                            @Param("toPeriod") LocalDate toPeriod,
                                            @Param("categoryIds") List<Long> categoryIds);

    boolean existsByUserAndCategory_Id(User user, Long categoryId);

    Optional<ExpenseRecord> findByIdAndUser(Long id, User user);

    @Query("""
            SELECT er FROM ExpenseRecord er
            WHERE er.user = :user AND er.id IN :ids
            """)
    List<ExpenseRecord> findByUserAndIdIn(@Param("user") User user,
                                          @Param("ids") List<Long> ids);
}
