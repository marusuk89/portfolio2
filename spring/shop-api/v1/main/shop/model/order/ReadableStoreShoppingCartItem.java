package com.itda.shop.model.order;

import com.itda.core.model.order.OrderTotalType;
import com.itda.shop.model.store.ReadableStore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter @Setter
@Schema(title = "StoreShoppingCartItem")
public class ReadableStoreShoppingCartItem
{
    @Schema(description = "상점")
    private ReadableStore store;

    @Schema(description = "상품 금액")
    private BigDecimal subtotal;

    @Schema(description = "상품 금액 text")
    private String displaySubTotal;

    @Schema(description = "배송료 제외 최종 금액")
    private BigDecimal total;

    @Schema(description = "최종 금액 text")
    private String displayTotal;

    @Schema(description = "주문 수량")
    private int quantity;

    @Schema(description = "상품별 주문 수량")
    private int productQuantity;

    @Schema(description = "가격 정보")
    private Map<OrderTotalType, ReadableOrderTotal> totals;

    @Schema(description = "카트 항목")
    private List<ReadableShoppingCartItem> items = new ArrayList<>();
}
