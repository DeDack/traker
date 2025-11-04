package com.traker.traker.entity;

import com.traker.traker.api.DefaultEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budget", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "period_start"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Budget extends DefaultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(name = "period_start", nullable = false)
    private LocalDate period;

    @Column(name = "planned_income", precision = 19, scale = 2)
    private BigDecimal plannedIncome;

    @Column(name = "planned_expense", precision = 19, scale = 2)
    private BigDecimal plannedExpense;

    @Column(name = "savings_goal", precision = 19, scale = 2)
    private BigDecimal savingsGoal;

    @Column(length = 1000)
    private String notes;
}
