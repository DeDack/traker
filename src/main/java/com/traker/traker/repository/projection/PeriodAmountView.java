package com.traker.traker.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PeriodAmountView {
    LocalDate getPeriod();
    BigDecimal getTotalAmount();
}
