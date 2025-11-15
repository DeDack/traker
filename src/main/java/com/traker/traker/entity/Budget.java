package com.traker.traker.entity;

import com.traker.traker.api.DefaultEntity;
import com.traker.traker.security.crypto.EncryptedBigDecimalConverter;
import com.traker.traker.security.crypto.EncryptedStringConverter;
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

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "planned_income", columnDefinition = "TEXT")
    private BigDecimal plannedIncome;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "planned_expense", columnDefinition = "TEXT")
    private BigDecimal plannedExpense;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "savings_goal", columnDefinition = "TEXT")
    private BigDecimal savingsGoal;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String notes;
}
