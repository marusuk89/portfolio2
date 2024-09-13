package com.itda.shop.model.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class ReadableProductOrderPrice
{
    /*
    @Schema(description = "기본 가격 여부")
    private boolean defaultPrice;
     */

    @Schema(description = "개별 상품 가격 (Inc. priceDiff)")
    private BigDecimal productItemPrice;

    @Schema(description = "주문 수량")
    private int quantity;

    @Schema(description = "상품 옴션 차액")
    private BigDecimal variantPriceDiff;

    @Schema(description = "상품 가격")
    private BigDecimal productPrice;

    @Schema(description = "할인된 상품 가격")
    private BigDecimal productDiscountPrice;
}
