package com.itda.shop.model.order;

import com.itda.core.model.common.Address;
import com.itda.core.model.common.DeliveryAddress;
import com.itda.core.model.order.ClaimStatus;
import com.itda.core.model.order.DeliveryMethod;
import com.itda.core.model.order.InternationalDeliveryMethod;
import com.itda.core.model.order.OrderTotalType;
import com.itda.core.model.order.PlaceOrderStatus;
import com.itda.core.model.order.ProductOrderDetailStatus;
import com.itda.core.model.order.ProductOrderStatus;
import com.itda.core.model.payment.PaymentType;
import com.itda.core.model.shipping.ShippingClass;
import com.itda.shop.model.content.ReadableImage;
import com.itda.shop.model.product.ProductSpecification;
import com.itda.shop.model.shipping.ReadablePackageTracking;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Getter @Setter
@Schema(title = "ProductOrder")
public class ReadableProductOrder
        extends ProductOrderEntity
{
    @Schema(description = "주문 번호")
    private String order;

    @Schema(description = "상품 주문 상태", enumAsRef = true)
    private ProductOrderStatus status;

    @Schema(description = "고객 상품 주문 상태", enumAsRef = true)
    private ProductOrderDetailStatus detailStatus;

    @Schema(description = "Claim 상태", enumAsRef = true)
    private ClaimStatus claimStatus;

    @Schema(description = "발주 상태", enumAsRef = true)
    private PlaceOrderStatus placeStatus;

    @Schema(description = "상품 단가")
    private BigDecimal itemPrice;

    @Schema(description = "상품 소계")
    private BigDecimal subTotal;

    private List<ReadableProductOrderAttribute> attributes;
    //private ReadableProductSummary product;

    @Schema(description = "상품 코드")
    private String product;

    @Schema(description = "판매자 상품 코드")
    private String productReference;

    @Schema(description = "상품명")
    private String productName;

    @Schema(description = "상품 옵션 코드")
    private String variant;

    @Schema(description = "상품 옵션명")
    private String variantName;

    @Schema(description = "상품 주문 관련 메모")
    private String comments;

    @Schema(description = "배송 구분")
    private ShippingClass shippingClass;

    @Schema(description = "발주 기한", deprecated = true)
    @Deprecated
    private OffsetDateTime shippingDueDateTime;

    @Schema(description = "발주 기한")
    private OffsetDateTime placeDueDateTime;

    @Schema(description = "예상 배송일")
    private OffsetDateTime estimatedDeliveryDateTime;

    @Schema(description = "국내 배송 방법", enumAsRef = true)
    private DeliveryMethod nationalDeliveryMethod;

    @Schema(description = "국내 배송 상황")
    private ReadablePackageTracking nationalTracking;

    @Schema(description = "국제 배송 방법", enumAsRef = true)
    private InternationalDeliveryMethod internationalDeliveryMethod;

    @Schema(description = "국제 배송 상황")
    private ReadablePackageTracking internationalTracking;

    @Schema(description = "구매자 명")
    private String customerFullName;

    @Schema(description = "구매자 Email")
    private String customerEmail;

    @Schema(description = "구매자 전화번호")
    private String customerTelephone;

    @Schema(description = "구매자 Cognito Username")
    private String customerUsername;

    @Schema(description = "주문 상품 가격")
    private ReadableProductOrderPrice price;

    @Schema(description = "주문 날짜/시간")
    private OffsetDateTime purchasedDate;

    @Schema(description = "발주 날짜/시간")
    private OffsetDateTime placedDate;

    @Schema(description = "배송완료 날짜/시간")
    private OffsetDateTime deliveredDate;

    @Schema(description = "구매확정 날짜/시간")
    private OffsetDateTime purchaseDecidedDate;

    @Schema(description = "자동 구매확정 날짜/시간")
    private OffsetDateTime automaticPurchaseDecidedDate;

    @Schema(description = "상품 크기 및 무게")
    private ProductSpecification productSpecification;

    @Schema(description = "배송 주소")
    private DeliveryAddress delivery;

    @Schema(description = "결제 수단", enumAsRef = true)
    private PaymentType paymentType;

    @Schema(description = "출고지")
    private Address shippingOrigin;

    @Schema(description = "가격 정보")
    private Map<OrderTotalType, ReadableOrderTotal> totals;

    @Schema(description = "상품 이미지")
    private ReadableImage productImage;
}
