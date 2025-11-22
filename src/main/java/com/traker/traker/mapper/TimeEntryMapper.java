package com.traker.traker.mapper;

import com.traker.traker.api.DefaultMapper;
import com.traker.traker.dto.TimeEntryDto;
import com.traker.traker.entity.TimeEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = DefaultMapper.class, uses = StatusMapper.class)
public interface TimeEntryMapper extends DefaultMapper<TimeEntry, TimeEntryDto> {

    @Override
    @Mapping(target = "userId", source = "user.id")
    TimeEntryDto toDto(TimeEntry entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dayLog", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    TimeEntry toEntity(TimeEntryDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dayLog", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntityFromDto(TimeEntryDto dto, @MappingTarget TimeEntry entity);
}
