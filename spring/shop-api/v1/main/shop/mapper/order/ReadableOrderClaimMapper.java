package com.itda.shop.mapper.order;

import com.itda.core.model.common.Mapper;
import com.itda.core.model.order.OrderClaim;
import com.itda.shop.model.order.ReadableOrderClaim;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReadableOrderClaimMapper
        implements Mapper<OrderClaim, ReadableOrderClaim>
{
    @Override
    public ReadableOrderClaim createTarget()
    {
        return new ReadableOrderClaim();
    }

    @Override
    public ReadableOrderClaim merge(OrderClaim source, ReadableOrderClaim destination)
    {
        return destination;
    }
}
