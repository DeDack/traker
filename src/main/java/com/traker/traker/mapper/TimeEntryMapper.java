package com.traker.traker.mapper;

import com.traker.traker.api.DefaultMapper;
import com.traker.traker.dto.TimeEntryDto;
import com.traker.traker.entity.TimeEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = DefaultMapper.class)
public interface TimeEntryMapper extends DefaultMapper<TimeEntry, TimeEntryDto> {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dayLog", ignore = true)
    TimeEntry toEntity(TimeEntryDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dayLog", ignore = true)
    void updateEntityFromDto(TimeEntryDto dto, @MappingTarget TimeEntry entity);
}
