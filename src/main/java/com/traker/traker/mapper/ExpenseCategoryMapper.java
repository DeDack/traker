package com.traker.traker.mapper;

import com.traker.traker.api.DefaultMapper;
import com.traker.traker.dto.expense.ExpenseCategoryDto;
import com.traker.traker.entity.ExpenseCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = DefaultMapper.class)
public interface ExpenseCategoryMapper extends DefaultMapper<ExpenseCategory, ExpenseCategoryDto> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    ExpenseCategory toEntity(ExpenseCategoryDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntityFromDto(ExpenseCategoryDto dto, @MappingTarget ExpenseCategory entity);
}
