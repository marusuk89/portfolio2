package com.itda.shop.mapper.order;

import com.itda.core.business.services.product.PricingService;
import com.itda.core.model.common.Mapper;
import com.itda.core.model.order.ShoppingCartItem;
import com.itda.core.model.product.ProductVariant;
import com.itda.shop.mapper.content.ReadableContentImageMapper;
import com.itda.shop.mapper.product.ReadableProductSummaryMapper;
import com.itda.shop.mapper.product.ReadableProductVariantMapper;
import com.itda.shop.model.content.ReadableContentImage;
import com.itda.shop.model.order.ReadableShoppingCartItem;
import com.itda.shop.model.product.attribute.ReadableProductVariant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.itda.core.business.constants.Constants.DEFAULT_CURRENCY;
import static com.itda.core.business.constants.Constants.DEFAULT_LOCALE;
import static com.itda.core.business.utils.ProductPriceUtils.estimatedDeliveryDateTime;
import static java.util.stream.Collectors.toUnmodifiableList;

@Component
@RequiredArgsConstructor
public class ReadableShoppingCartItemMapper
        implements Mapper<ShoppingCartItem, ReadableShoppingCartItem>
{
    private final PricingService pricingService;

    private final ReadableProductSummaryMapper readableMinimalProductMapper;
    private final ReadableProductVariantMapper readableProductVariantMapper;
    private final ReadableContentImageMapper readableContentImageMapper;

    @Override
    public ReadableShoppingCartItem createTarget()
    {
        return new ReadableShoppingCartItem();
    }

    @Override
    public ReadableShoppingCartItem merge(ShoppingCartItem source, ReadableShoppingCartItem target)
    {
        target.setCode(source.getCode());
        target.setProduct(readableMinimalProductMapper.convert(source.getProduct()));

        //variation
        if (source.getProductVariant().isPresent()) {
            ProductVariant productVariant = source.getProductVariant().get();
            ReadableProductVariant readableProductVariant = readableProductVariantMapper.convert(productVariant);

            if (productVariant.getImages() != null) {
                Set<String> nameSet = new HashSet<>();
                List<ReadableContentImage> variantImages = productVariant.getImages().stream()
                        .map(readableContentImageMapper::convert)
                        .filter(e -> nameSet.add(e.getImageUrl()))
                        .collect(toUnmodifiableList());
                readableProductVariant.setImages(variantImages);
            }
            target.setVariant(readableProductVariant);
        }

        target.setItemPrice(source.getItemPrice());
        target.setDisplayItemPrice(pricingService.getDisplayAmount(source.getItemPrice(), DEFAULT_LOCALE, DEFAULT_CURRENCY));

        int availableQuantity = source.getProductVariant().map(ProductVariant::getAvailability)
                .orElseGet(() -> source.getProduct().getAvailability())
                .getQuantity();
        target.setQuantity(source.getQuantity());
        target.setAvailableQuantity(availableQuantity);

        BigDecimal subTotal = pricingService.calculatePriceQuantity(source.getItemPrice(), source.getQuantity());

        // calculate sub total (price * quantity)
        target.setSubTotal(subTotal);

        target.setDisplaySubTotal(pricingService.getDisplayAmount(subTotal, DEFAULT_LOCALE, DEFAULT_CURRENCY));
        target.setEstimatedDeliveryDateTime(estimatedDeliveryDateTime(source.getProduct().getShippingClass()));
        target.setSaveForLater(source.isSaveForLater());
        target.setStatus(source.getStatus());

        return target;
    }
}
