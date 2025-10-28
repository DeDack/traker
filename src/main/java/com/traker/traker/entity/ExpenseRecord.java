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
@Table(name = "expense_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseRecord extends DefaultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    private ExpenseCategory category;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "period_start", nullable = false)
    private LocalDate period;

    @Column(name = "expense_date")
    private LocalDate expenseDate;
}
