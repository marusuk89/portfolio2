package com.itda.shop.model.order;

import com.itda.core.model.order.ShoppingCartItemStatus;
import com.itda.shop.model.entity.CodeEntity;
import com.itda.shop.model.product.ReadableProductSummary;
import com.itda.shop.model.product.attribute.ReadableProductVariant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter @Setter
@Schema(title = "ShoppingCartItem")
public class ReadableShoppingCartItem
        extends CodeEntity
{
    @Schema(description = "단위 가격")
    private BigDecimal itemPrice;

    @Schema(description = "단위 가격 Text")
    private String displayItemPrice;

    @Schema(description = "구매 수량")
    private int quantity;

    @Schema(description = "재고 수량")
    private int availableQuantity;

    @Schema(description = "상품 가격")
    private BigDecimal subTotal;

    @Schema(description = "상품 가격 Text")
    private String displaySubTotal;

    @Schema(description = "예상 배송일")
    private OffsetDateTime estimatedDeliveryDateTime;

    @Schema(description = "추후 결제")
    private boolean saveForLater;

    @Schema(description = "상태 정보", enumAsRef = true)
    private ShoppingCartItemStatus status;

    @Schema(description = "상품 요약 정보")
    private ReadableProductSummary product;

    @Schema(description = "선택한 상품 옵션")
    private ReadableProductVariant variant;
}
