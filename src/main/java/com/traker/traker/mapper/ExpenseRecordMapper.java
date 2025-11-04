package com.traker.traker.mapper;

import com.traker.traker.dto.expense.ExpenseRecordResponseDto;
import com.traker.traker.entity.ExpenseRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ExpenseRecordMapper {

    DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "period", expression = "java(formatPeriod(expenseRecord.getPeriod()))")
    @Mapping(target = "monthOnly", expression = "java(expenseRecord.getExpenseDate() == null)")
    ExpenseRecordResponseDto toDto(ExpenseRecord expenseRecord);

    List<ExpenseRecordResponseDto> toDtoList(List<ExpenseRecord> expenseRecords);

    default String formatPeriod(LocalDate period) {
        return period == null ? null : period.format(PERIOD_FORMATTER);
    }
}
