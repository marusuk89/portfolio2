package com.itda.shop.mapper.order;

import com.itda.core.business.services.product.PricingService;
import com.itda.core.model.common.Mapper;
import com.itda.core.model.common.ReadableDeliveryMapper;
import com.itda.core.model.shipping.ShippingSummary;
import com.itda.shop.model.order.ReadableShippingSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

@Component
@RequiredArgsConstructor
public class ReadableShippingSummaryMapper
        implements Mapper<ShippingSummary, ReadableShippingSummary>
{
    private final PricingService pricingService;
    private final ReadableDeliveryMapper readableDeliveryMapper;

    @Override
    public ReadableShippingSummary merge(ShippingSummary source, ReadableShippingSummary target)
    {
        requireNonNull(pricingService, "PricingService must be set");
        requireNonNull(source, "ShippingSummary cannot be null");

        target.setShippingQuote(source.isShippingQuote());
        target.setFreeShipping(source.isFreeShipping());
        target.setHandling(source.getHandling());
        target.setShipping(source.getShipping());
        target.setShippingModule(source.getShippingModule());
        target.setShippingOption(source.getShippingOption());
        target.setTaxOnShipping(source.isTaxOnShipping());
        target.setDisplayHandling(pricingService.getDisplayAmount(source.getHandling()));
        target.setDisplayShipping(pricingService.getDisplayAmount(source.getShipping()));

        if (source.getDeliveryAddress().isPresent()) {
            target.setDelivery(readableDeliveryMapper.convert(source.getDeliveryAddress().get()));
        }

        return target;
    }

    @Override
    public ReadableShippingSummary createTarget()
    {
        return new ReadableShippingSummary();
    }
}
