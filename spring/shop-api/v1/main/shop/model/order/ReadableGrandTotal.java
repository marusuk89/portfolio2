package com.itda.shop.model.order;

import com.itda.core.model.order.OrderTotalType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Serves as the order total summary calculation
 */
@Getter @Setter
@Schema(title = "GrandTotal")
public class ReadableGrandTotal
{
    @Schema(description = "가격 세부 내용")
    private Map<OrderTotalType, ReadableOrderTotal> totals;

    @Schema(description = "최종 가격")
    private BigDecimal grandTotal;

    @Schema(description = "최종 가격 TEXT")
    private String displayGrandTotal;
}
