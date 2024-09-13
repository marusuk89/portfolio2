package com.itda.shop.model.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Schema(title = "OrderTotal")
public class ReadableOrderTotal
        extends OrderTotalEntity
{
    @Schema(description = "주문 가격 Text")
    private String displayAmount;

    @Schema(description = "주문 가격 종류 Text")
    private String title;

    @Schema(description = "할인 가격 여부")
    private boolean discounted;
}
