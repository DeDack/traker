package com.traker.traker.dto.entity;

import com.traker.traker.entity.Status;
import lombok.Data;

@Data
public class TimeEntryDto {
    private int hour;
    private boolean worked;
    private String comment;
    private Status status;
}