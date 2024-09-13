package com.itda.shop.model.order;

import com.itda.shop.model.entity.Entity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Getter @Setter
@Schema(title = "(*)ShoppingCartItem")
public class PersistableShoppingCartItem
        extends Entity
{
    @Schema(description = "상품 CODE", requiredMode = REQUIRED)
    private String product;

    @Schema(description = "상품 선택 옵션(Variant) CODE")
    private String variant;

    @Min(1)
    @Schema(description = "상품 수량", requiredMode = REQUIRED)
    private int quantity;

    @Schema(description = "할인 코드, 쿠폰 등")
    private List<String> promoCodes = List.of();
}
