package com.itda.shop.model.order;

import com.itda.core.model.common.ReadableDeliveryAddress;
import com.itda.core.model.shipping.ShippingOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter @Setter
@Schema(title = "ShippingSummary")
public class ReadableShippingSummary
{
    @Schema(description = "배송료")
    private BigDecimal shipping;

    @Schema(description = "배송료 TEXT")
    private String displayShipping;

    @Schema(description = "배송 처리비용")
    private BigDecimal handling;

    @Schema(description = "배송 처리비용 TEXT")
    private String displayHandling;

    @Schema(description = "배송 업체 코드")
    private String shippingModule;

    @Schema(description = "배송 옵션")
    private String shippingOption;

    @Schema(description = "무료 배송 여부")
    private boolean freeShipping;

    @Schema(description = "세금 포함 여부")
    private boolean taxOnShipping;

    @Schema(description = "배송 견적 여부")
    private boolean shippingQuote;

    @Schema(description = "배송 주소")
    private ReadableDeliveryAddress delivery;

    @Schema(description = "기본 배송 옵션")
    private ShippingOption selectedShippingOption; //Default selected option

    @Schema(description = "선택 가능 배송 옵션")
    private List<ShippingOption> shippingOptions;

    /** additional information that comes from the quote **/
    @Schema(description = "배송 추가 정보")
    private Map<String, String> quoteInformation = new HashMap<>();
}
