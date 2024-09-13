package com.itda.shop.model.order;

import com.itda.core.model.order.OrderTotalType;
import com.itda.shop.model.entity.CodeEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter @Setter
@Schema(title = "ShoppingCart")
public class ReadableShoppingCart
        extends CodeEntity
{
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

    @Schema(description = "배송 무게")
    private double shippingWeight;

    @Schema(description = "상품별 주문 수량")
    private int productQuantity;

    @Schema(description = "할인 코드")
    private List<String> promoCodes;

    @Schema(description = "고객 고유 code")
    private String customer;

    @Schema(description = "가격 정보")
    private Map<OrderTotalType, ReadableOrderTotal> totals;

    @Schema(description = "상점 별 카트 항목")
    private List<ReadableStoreShoppingCartItem> storeItems = new ArrayList<>();
}
