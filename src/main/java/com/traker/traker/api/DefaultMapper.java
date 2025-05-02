package com.traker.traker.api;

import org.mapstruct.MapperConfig;

@MapperConfig(componentModel = "spring")
public interface DefaultMapper<ENT, DTO> {
    DTO toDto(ENT entity);

    ENT toEntity(DTO dto);
}
