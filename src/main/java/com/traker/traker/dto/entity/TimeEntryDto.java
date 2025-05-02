package com.traker.traker.dto.entity;

import lombok.Data;

@Data
public class TimeEntryDto {
    private int hour;
    private boolean worked;
    private String comment;
}