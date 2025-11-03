package com.traker.traker.mapper;

import com.traker.traker.api.DefaultMapper;
import com.traker.traker.dto.income.IncomeCategoryDto;
import com.traker.traker.entity.IncomeCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = DefaultMapper.class)
public interface IncomeCategoryMapper extends DefaultMapper<IncomeCategory, IncomeCategoryDto> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    IncomeCategory toEntity(IncomeCategoryDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntityFromDto(IncomeCategoryDto dto, @MappingTarget IncomeCategory entity);
}
