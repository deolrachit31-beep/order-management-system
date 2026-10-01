package com.oms.order.domain;

import com.oms.order.domain.DomainExceptions.BadRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class OrderTotals {

    private OrderTotals() {
    }

    public static BigDecimal lineTotal(BigDecimal unitPrice, int quantity) {
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Unit price must be positive");
        }
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal sum(List<BigDecimal> lineTotals) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal line : lineTotals) {
            total = total.add(line);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
