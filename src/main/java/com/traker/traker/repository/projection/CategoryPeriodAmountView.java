package com.traker.traker.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CategoryPeriodAmountView {
    Long getCategoryId();
    String getCategoryName();
    LocalDate getPeriod();
    BigDecimal getTotalAmount();
}
