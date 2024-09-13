package com.itda.shop.model.order;

import com.itda.core.model.common.Address;
import com.itda.shop.model.entity.CodeEntity;
import com.itda.shop.model.payment.ReadablePaymentTransaction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class OrderConfirmation
        extends CodeEntity
{
    @Schema(description = "배송 주소")
    private Address delivery;

    @Schema(description = "배송 업체")
    private String shipping;

    @Schema(description = "배송 무게")
    private double shippingWeight;

    @Schema(description = "결제 정보")
    private ReadablePaymentTransaction payment;

    @Schema(description = "최종 결제 금액")
    private ReadableGrandTotal total;

    @Schema(description = "주문 상품들")
    private List<ReadableProductOrder> products;
}
