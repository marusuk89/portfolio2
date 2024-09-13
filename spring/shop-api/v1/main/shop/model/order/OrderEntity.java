package com.itda.shop.model.order;

import com.itda.shop.model.entity.Entity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class OrderEntity
        extends Entity
{
    @Schema(description = "주문 관련 메모")
    private String comments;

    @Schema(description = "결제 통화", defaultValue = "KRW")
    private String currency = "KRW";

    @Schema(description = "주문 추가 정보")
    private List<OrderAttributeEntity> attributes = new ArrayList<>();
}
