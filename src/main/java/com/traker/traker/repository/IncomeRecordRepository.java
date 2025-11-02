package com.traker.traker.repository;

import com.traker.traker.api.DefaultRepository;
import com.traker.traker.entity.IncomeRecord;
import com.traker.traker.entity.User;
import com.traker.traker.repository.projection.CategoryAmountView;
import com.traker.traker.repository.projection.CategoryPeriodAmountView;
import com.traker.traker.repository.projection.PeriodAmountView;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IncomeRecordRepository extends DefaultRepository<IncomeRecord, Long> {

    @Query("""
            SELECT ir FROM IncomeRecord ir
            WHERE ir.user = :user
              AND (:fromPeriod IS NULL OR ir.period >= :fromPeriod)
              AND (:toPeriod IS NULL OR ir.period <= :toPeriod)
              AND (
                    ir.incomeDate IS NULL
                    OR (:fromDate IS NULL OR ir.incomeDate >= :fromDate)
                )
              AND (
                    ir.incomeDate IS NULL
                    OR (:toDate IS NULL OR ir.incomeDate <= :toDate)
                )
            ORDER BY ir.period ASC, ir.incomeDate ASC, ir.id ASC
            """)
    List<IncomeRecord> findByUserAndFilter(@Param("user") User user,
                                           @Param("fromDate") LocalDate fromDate,
                                           @Param("toDate") LocalDate toDate,
                                           @Param("fromPeriod") LocalDate fromPeriod,
                                           @Param("toPeriod") LocalDate toPeriod);

    @Query("""
            SELECT ir.category.id as categoryId,
                   ir.category.name as categoryName,
                   SUM(ir.amount) as totalAmount
            FROM IncomeRecord ir
            WHERE ir.user = :user
              AND (:fromPeriod IS NULL OR ir.period >= :fromPeriod)
              AND (:toPeriod IS NULL OR ir.period <= :toPeriod)
              AND (
                    ir.incomeDate IS NULL
                    OR (:fromDate IS NULL OR ir.incomeDate >= :fromDate)
                )
              AND (
                    ir.incomeDate IS NULL
                    OR (:toDate IS NULL OR ir.incomeDate <= :toDate)
                )
            GROUP BY ir.category.id, ir.category.name
            ORDER BY ir.category.name
            """)
    List<CategoryAmountView> sumByCategory(@Param("user") User user,
                                           @Param("fromDate") LocalDate fromDate,
                                           @Param("toDate") LocalDate toDate,
                                           @Param("fromPeriod") LocalDate fromPeriod,
                                           @Param("toPeriod") LocalDate toPeriod);

    @Query("""
            SELECT ir.period as period,
                   SUM(ir.amount) as totalAmount
            FROM IncomeRecord ir
            WHERE ir.user = :user
              AND (:fromPeriod IS NULL OR ir.period >= :fromPeriod)
              AND (:toPeriod IS NULL OR ir.period <= :toPeriod)
              AND (
                    ir.incomeDate IS NULL
                    OR (:fromDate IS NULL OR ir.incomeDate >= :fromDate)
                )
              AND (
                    ir.incomeDate IS NULL
                    OR (:toDate IS NULL OR ir.incomeDate <= :toDate)
                )
            GROUP BY ir.period
            ORDER BY ir.period
            """)
    List<PeriodAmountView> sumByPeriod(@Param("user") User user,
                                       @Param("fromDate") LocalDate fromDate,
                                       @Param("toDate") LocalDate toDate,
                                       @Param("fromPeriod") LocalDate fromPeriod,
                                       @Param("toPeriod") LocalDate toPeriod);

    @Query("""
            SELECT ir.category.id as categoryId,
                   ir.category.name as categoryName,
                   ir.period as period,
                   SUM(ir.amount) as totalAmount
            FROM IncomeRecord ir
            WHERE ir.user = :user
              AND (:fromPeriod IS NULL OR ir.period >= :fromPeriod)
              AND (:toPeriod IS NULL OR ir.period <= :toPeriod)
              AND (
                    ir.incomeDate IS NULL
                    OR (:fromDate IS NULL OR ir.incomeDate >= :fromDate)
                )
              AND (
                    ir.incomeDate IS NULL
                    OR (:toDate IS NULL OR ir.incomeDate <= :toDate)
                )
            GROUP BY ir.category.id, ir.category.name, ir.period
            ORDER BY ir.category.name, ir.period
            """)
    List<CategoryPeriodAmountView> sumByCategoryAndPeriod(@Param("user") User user,
                                                          @Param("fromDate") LocalDate fromDate,
                                                          @Param("toDate") LocalDate toDate,
                                                          @Param("fromPeriod") LocalDate fromPeriod,
                                                          @Param("toPeriod") LocalDate toPeriod);
}
