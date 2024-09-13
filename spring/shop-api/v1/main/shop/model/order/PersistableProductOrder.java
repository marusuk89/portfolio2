package com.itda.shop.model.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PersistableProductOrder
{
    @Schema(description = "상품 주문 관련 메모")
    private String comments;
}
