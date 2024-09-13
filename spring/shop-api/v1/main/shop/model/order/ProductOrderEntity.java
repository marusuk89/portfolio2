package com.itda.shop.model.order;

import com.itda.shop.model.entity.CodeEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProductOrderEntity
        extends CodeEntity
{
    @Schema(description = "주문 수량")
    private int quantity;
}
