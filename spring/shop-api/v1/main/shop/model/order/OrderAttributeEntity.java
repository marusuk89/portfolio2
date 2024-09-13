package com.itda.shop.model.order;

import com.itda.core.model.order.OrderAttributeType;
import com.itda.shop.model.entity.Entity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Schema(title = "OrderAttribute")
public class OrderAttributeEntity
        extends Entity
{
    @Schema(description = "주문 추가 정보 상점")
    private String store;

    @Schema(description = "주문 추가 정보 Key")
    private OrderAttributeType key;

    @Schema(description = "주문 추가 값")
    private String value;
}
