package com.traker.traker.mapper;

import com.traker.traker.api.DefaultMapper;
import com.traker.traker.dto.entity.TimeEntryDto;
import com.traker.traker.entity.TimeEntry;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = DefaultMapper.class)
public interface TimeEntryMapper extends DefaultMapper<TimeEntry, TimeEntryDto> {
    void updateEntityFromDto(TimeEntryDto dto, @MappingTarget TimeEntry entity);
}