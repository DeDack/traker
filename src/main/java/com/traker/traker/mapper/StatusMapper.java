package com.traker.traker.mapper;

import com.traker.traker.dto.StatusDto;
import com.traker.traker.entity.Status;
import com.traker.traker.api.DefaultMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StatusMapper extends DefaultMapper<Status, StatusDto> {

    @Override
    @org.mapstruct.Mapping(target = "order", source = "orderIndex")
    @org.mapstruct.Mapping(target = "userId", source = "user.id")
    StatusDto toDto(Status entity);

    @Override
    @org.mapstruct.Mapping(target = "orderIndex", source = "order", defaultValue = "0")
    @org.mapstruct.Mapping(target = "user", ignore = true)
    Status toEntity(StatusDto dto);
}