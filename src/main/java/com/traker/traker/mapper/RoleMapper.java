package com.traker.traker.mapper;

import com.traker.traker.api.DefaultMapper;
import com.traker.traker.dto.RoleDto;
import com.traker.traker.entity.Role;
import org.mapstruct.Mapper;

/**
 * Маппер для преобразования между Role и RoleDto.
 */
@Mapper(componentModel = "spring")
public interface RoleMapper extends DefaultMapper<Role, RoleDto> {

    /** Преобразование сущности Role в DTO. */
    @Override
    RoleDto toDto(Role entity);

    /** Преобразование DTO в сущность Role. */
    @Override
    Role toEntity(RoleDto dto);
}