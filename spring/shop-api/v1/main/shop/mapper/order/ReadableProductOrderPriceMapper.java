package com.itda.shop.mapper.order;

import com.itda.core.model.common.Mapper;
import com.itda.core.model.order.ProductOrderPrice;
import com.itda.shop.model.order.ReadableProductOrderPrice;
import org.springframework.stereotype.Component;

@Component
public class ReadableProductOrderPriceMapper
        implements Mapper<ProductOrderPrice, ReadableProductOrderPrice>
{
    @Override
    public ReadableProductOrderPrice createTarget()
    {
        return new ReadableProductOrderPrice();
    }

    @Override
    public ReadableProductOrderPrice merge(ProductOrderPrice source, ReadableProductOrderPrice destination)
    {
        if (source == null) {
            // FIXME: Remove this
            source = new ProductOrderPrice();
        }
        destination.setProductPrice(source.getProductPrice());
        destination.setProductDiscountPrice(source.getProductDiscountPrice());
        destination.setQuantity(source.getQuantity());
        destination.setProductItemPrice(source.getProductItemPrice());
        destination.setVariantPriceDiff(source.getVariantPriceDiff());
        return destination;
    }
}
