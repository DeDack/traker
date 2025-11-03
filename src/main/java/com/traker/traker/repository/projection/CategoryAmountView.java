package com.traker.traker.repository.projection;

import java.math.BigDecimal;

public interface CategoryAmountView {
    Long getCategoryId();
    String getCategoryName();
    BigDecimal getTotalAmount();
}
