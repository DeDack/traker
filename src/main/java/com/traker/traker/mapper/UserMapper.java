package com.traker.traker.mapper;

import com.traker.traker.api.DefaultMapper;
import com.traker.traker.dto.UserDto;
import com.traker.traker.dto.CreateUserDto;
import com.traker.traker.dto.UpdateUserDto;
import com.traker.traker.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Маппер для преобразования сущностей User и соответствующих DTO.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper extends DefaultMapper<User, UserDto> {

    /**
     * Преобразует сущность User в UserDto.
     *
     * @param entity Сущность пользователя
     * @return DTO пользователя
     */
    @Override
    UserDto toDto(User entity);

    /**
     * Преобразует UserDto в сущность User.
     *
     * @param dto DTO пользователя
     * @return Сущность пользователя
     */
    @Override
    User toEntity(UserDto dto);

    /**
     * Преобразует CreateUserDto в сущность User.
     *
     * @param createUserDto DTO для создания пользователя
     * @return Сущность пользователя
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "password", ignore = true)
    User toEntity(CreateUserDto createUserDto);

    /**
     * Обновляет сущность User на основе UpdateUserDto.
     *
     * @param updateUserDto DTO с данными для обновления
     * @param user          Сущность пользователя для обновления
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateEntity(UpdateUserDto updateUserDto, @MappingTarget User user);
}