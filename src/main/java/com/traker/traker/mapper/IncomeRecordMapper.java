package com.traker.traker.mapper;

import com.traker.traker.dto.income.IncomeRecordResponseDto;
import com.traker.traker.entity.IncomeRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring")
public interface IncomeRecordMapper {

    DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "period", expression = "java(formatPeriod(incomeRecord.getPeriod()))")
    @Mapping(target = "monthOnly", expression = "java(incomeRecord.getIncomeDate() == null)")
    IncomeRecordResponseDto toDto(IncomeRecord incomeRecord);

    List<IncomeRecordResponseDto> toDtoList(List<IncomeRecord> incomeRecords);

    default String formatPeriod(LocalDate period) {
        return period == null ? null : period.format(PERIOD_FORMATTER);
    }
}
