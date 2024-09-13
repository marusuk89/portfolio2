package com.itda.shop.model.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itda.core.model.common.PersistableDeliveryAddress;
import com.itda.core.model.customer.Customer;
import com.itda.core.model.order.PurchasedOrder;
import com.itda.core.model.order.ShoppingCart;
import com.itda.core.model.payment.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;

/**
 * This object is used when processing an order from the API
 * It will be used for processing the payment and as Order metadata
 */
@Getter @Setter
@Schema(title = "(*)Order")
public class PersistableOrder
        extends OrderEntity
{
    /*
    @Schema(description = "결제 정보")
    private PersistablePayment payment;
     */

    @Schema(description = "결제 수단", defaultValue = "CREDITCARD", example = "CREDITCARD", enumAsRef = true)
    private PaymentType paymentType;

    @Schema(hidden = true, description = "배송료 견적 옵션 CODE, 추후 배송옵션을 선택하는 경우 사용")
    private String shippingQuote;

    @Schema(description = "영문 배송 주소")
    @Valid
    private PersistableDeliveryAddress delivery;

    @JsonIgnore
    @Schema(hidden = true)
    private ShoppingCart shoppingCart;

    @JsonIgnore
    @Schema(hidden = true)
    private Customer customer;

    @JsonIgnore
    @Schema(hidden = true)
    private PurchasedOrder purchasedOrder;
}
