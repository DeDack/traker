package com.traker.traker.mapper;

import com.traker.traker.dto.StatusDto;
import com.traker.traker.entity.Status;
import com.traker.traker.api.DefaultMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StatusMapper extends DefaultMapper<Status, StatusDto> {
}