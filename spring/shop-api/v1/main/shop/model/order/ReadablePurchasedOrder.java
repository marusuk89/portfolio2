package com.itda.shop.model.order;

import com.itda.core.model.common.DeliveryAddress;
import com.itda.core.model.order.OrderTotalType;
import com.itda.core.model.payment.PaymentType;
import com.itda.shop.model.entity.CodeEntity;
import com.itda.shop.model.payment.ReadablePaymentTransaction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Getter @Setter
@Schema(title = "PurchasedOrder")
public class ReadablePurchasedOrder
        extends CodeEntity
{
    @Schema(description = "주문 번호")
    private String code;

    private List<ReadableProductOrderAttribute> attributes;
    //private ReadableProductSummary product;

    @Schema(description = "최종 결제 금액")
    private BigDecimal total;

    @Schema(description = "구매자 명")
    private String customerFullName;

    @Schema(description = "구매자 Email")
    private String customerEmail;

    @Schema(description = "구매자 전화번호")
    private String customerTelephone;

    @Schema(description = "구매자 Cognito Username")
    private String customerUsername;

    @Schema(description = "주문 날짜/시간")
    private OffsetDateTime purchasedDate;

    @Schema(description = "결제 수단", enumAsRef = true, deprecated = true)
    @Deprecated
    private PaymentType paymentType;

    @Schema(description = "결제 정보")
    private ReadablePaymentTransaction payment;

    @Schema(description = "가격 정보")
    private Map<OrderTotalType, ReadableOrderTotal> totals;

    @Schema(description = "배송 무게")
    private double shippingWeight;

    @Schema(description = "상점 주문 내역")
    private List<ReadableStoreOrder> storeOrders;

    @Schema(description = "배송 주소")
    private DeliveryAddress delivery;
}
