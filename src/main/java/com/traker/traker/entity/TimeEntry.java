package com.traker.traker.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

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
    @ToString.Exclude
    private DayLog dayLog;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(nullable = false)
    private int hour;

    @Column(nullable = false)
    private int minute;

    @Column(nullable = false)
    private boolean worked;

    @Column
    private String comment;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private Status status;
}