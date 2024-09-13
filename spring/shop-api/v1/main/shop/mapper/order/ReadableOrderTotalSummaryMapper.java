package com.itda.shop.mapper.order;

import com.itda.core.business.services.product.PricingService;
import com.itda.core.model.common.Mapper;
import com.itda.core.model.order.OrderTotalSummary;
import com.itda.shop.model.order.ReadableOrderTotal;
import com.itda.shop.model.order.ReadableOrderTotalSummary;
import com.itda.shop.utils.LabelUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class ReadableOrderTotalSummaryMapper
        implements Mapper<OrderTotalSummary, ReadableOrderTotalSummary>
{
    private final PricingService pricingService;

    private final LabelUtils messages;

    private final ReadableOrderTotalMapper readableOrderTotalMapper;

    @Override
    public ReadableOrderTotalSummary merge(OrderTotalSummary source, ReadableOrderTotalSummary target)
    {
        requireNonNull(pricingService, "PricingService must be set");
        requireNonNull(messages, "LabelUtils must be set");

        if (source.getSubTotal() != null) {
            target.setSubTotal(pricingService.getDisplayAmount(source.getSubTotal()));
        }
        if (source.getTaxTotal() != null) {
            target.setTaxTotal(pricingService.getDisplayAmount(source.getTaxTotal()));
        }
        if (source.getTotal() != null) {
            target.setTotal(pricingService.getDisplayAmount(source.getTotal()));
        }

        if (!CollectionUtils.isEmpty(source.getTotals())) {
            target.setTotals(source.getTotals().stream()
                    .map(readableOrderTotalMapper::convert)
                    .collect(Collectors.toUnmodifiableMap(ReadableOrderTotal::getType, Function.identity())));
        }

        return target;
    }

    @Override
    public ReadableOrderTotalSummary createTarget()
    {
        return new ReadableOrderTotalSummary();
    }
}
