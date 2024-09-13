package com.itda.shop.mapper.order;

import com.itda.core.business.constants.Constants;
import com.itda.core.business.services.product.PricingService;
import com.itda.core.model.common.Mapper;
import com.itda.core.model.order.OrderTotal;
import com.itda.core.model.order.OrderTotalType;
import com.itda.shop.model.order.ReadableOrderTotal;
import com.itda.shop.store.api.exception.ConversionRuntimeException;
import com.itda.shop.utils.LabelUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

@Component
@RequiredArgsConstructor
public class ReadableOrderTotalMapper
        implements Mapper<OrderTotal, ReadableOrderTotal>
{
    private final PricingService pricingService;

    private final LabelUtils messages;

    @Override
    public ReadableOrderTotal merge(OrderTotal source, ReadableOrderTotal target)
    {
        requireNonNull(source, "OrderTotal must not be null");
        requireNonNull(target, "ReadableTotal must not be null");

        try {
            target.setType(source.getType());
            //target.setId(source.getId());
            //target.setModule(source.getModule());
            target.setSortOrder(source.getSortOrder());

            target.setTitle(getMessage(source.getType()));
            //target.setText(source.getText());

            target.setAmount(source.getAmount());
            target.setDisplayAmount(pricingService.getDisplayAmount(source.getAmount()));

            if (source.getType() == OrderTotalType.DISCOUNT) {
                target.setDiscounted(true);
            }
        }
        catch (Exception e) {
            throw new ConversionRuntimeException(e);
        }

        return target;
    }

    @Override
    public ReadableOrderTotal createTarget()
    {
        return new ReadableOrderTotal();
    }

    private String getMessage(OrderTotalType type)
    {
        return messages.getMessage("order.total." + type.name().toLowerCase(Locale.ROOT), Constants.DEFAULT_LOCALE);
    }
}
