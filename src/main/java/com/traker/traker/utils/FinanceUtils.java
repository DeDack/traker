package com.traker.traker.utils;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@UtilityClass
public class FinanceUtils {

    public static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public FinanceFilter buildFilter(String fromDate, String toDate, String month) {
        YearMonth monthFilter = parseOptionalPeriod(month);
        LocalDate from = parseOptionalDate(fromDate);
        LocalDate to = parseOptionalDate(toDate);

        if (monthFilter != null && (from != null || to != null)) {
            throw new IllegalArgumentException("Нельзя одновременно задавать месяц и произвольный диапазон дат");
        }

        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("Дата окончания диапазона не может быть раньше даты начала");
        }

        if (monthFilter != null) {
            LocalDate period = monthFilter.atDay(1);
            return new FinanceFilter(from, to, period, period, monthFilter);
        }

        LocalDate fromPeriod = from != null ? from.withDayOfMonth(1) : null;
        LocalDate toPeriod = to != null ? to.withDayOfMonth(1) : null;
        return new FinanceFilter(from, to, fromPeriod, toPeriod, null);
    }

    public YearMonth parseOptionalPeriod(String period) {
        if (period == null || period.isBlank()) {
            return null;
        }
        return parsePeriod(period);
    }

    public YearMonth parsePeriod(String period) {
        try {
            return YearMonth.parse(period, PERIOD_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Некорректный формат периода: " + period, e);
        }
    }

    public LocalDate parseOptionalDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Некорректный формат даты: " + date, e);
        }
    }

    public BigDecimal normalizeAmount(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public String formatPeriod(LocalDate period) {
        return period == null ? null : period.format(PERIOD_FORMATTER);
    }

    public record FinanceFilter(LocalDate fromDate,
                                LocalDate toDate,
                                LocalDate fromPeriod,
                                LocalDate toPeriod,
                                YearMonth month) {
    }
}
