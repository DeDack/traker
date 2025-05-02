package com.traker.traker.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "day_log_id", nullable = false)
    private DayLog dayLog;

    @Column(nullable = false)
    private int hour;

    @Column(nullable = false)
    private boolean worked;

    @Column
    private String comment;
}
