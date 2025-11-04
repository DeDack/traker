package com.traker.traker.dto;

import lombok.Data;

@Data
public class TimeEntryDto {
    private Long id;
    private int hour;
    private int minute;
    private int endHour;
    private int endMinute;
    private boolean worked;
    private String comment;
    private StatusDto status;
    private Long userId;
}