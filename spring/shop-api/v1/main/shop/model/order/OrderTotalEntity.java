package com.itda.shop.model.order;

import com.itda.core.model.order.OrderTotalType;
import com.itda.shop.model.entity.Entity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class OrderTotalEntity
        extends Entity
{
    @Schema(description = "주문 가격 종류", enumAsRef = true)
    private OrderTotalType type;

    @Schema(description = "주문 가격")
    private BigDecimal amount;

    @Schema(description = "주문 가격 표시 순서")
    private int sortOrder;
    //private String module;
    //private String title;
    //private String text;
}
