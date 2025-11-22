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

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String description;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "amount", nullable = false, columnDefinition = "TEXT")
    private BigDecimal amount;

    @Column(name = "period_start", nullable = false)
    private LocalDate period;

    @Column(name = "expense_date")
    private LocalDate expenseDate;
}
