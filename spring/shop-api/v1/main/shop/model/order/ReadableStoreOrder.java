package com.itda.shop.model.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itda.shop.model.store.ReadableStore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@Schema(title = "StoreOrder")
public class ReadableStoreOrder
        extends OrderEntity
{
    @Schema(description = "주문 상점")
    private ReadableStore store;

    @Getter(onMethod_ = @JsonIgnore)
    @Schema(description = "상품 금액", hidden = true)
    private BigDecimal subtotal;

    /*
    @JsonIgnore
    @Schema(description = "상품 금액 text", hidden = true)
    private String displaySubTotal;

    @JsonIgnore
    @Schema(description = "최종 금액", hidden = true)
    private BigDecimal total;

    @JsonIgnore
    @Schema(description = "최종 금액 text", hidden = true)
    private String displayTotal;

    @JsonIgnore
    @Schema(description = "주문 수량", hidden = true)
    private int quantity;

    @JsonIgnore
    @Schema(description = "상품별 주문 수량", hidden = true)
    private int productQuantity;

    @Schema(description = "주문 완료 코드")
    private String order;

    @JsonIgnore
    @Schema(description = "할인 코드", hidden = true)
    private List<String> promoCodes;

    @JsonIgnore
    @Schema(description = "고객 고유 code", hidden = true)
    private String customer;

    @JsonIgnore
    @Schema(description = "가격 정보", hidden = true)
    private Map<OrderTotalType, ReadableOrderTotal> totals;
     */

    @Schema(description = "주문 상품")
    private List<ReadableProductOrder> productOrders;
}
