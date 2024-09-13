package com.itda.shop.mapper.order;

import com.google.common.collect.ImmutableList;
import com.itda.core.model.common.Mapper;
import com.itda.core.model.order.Order;
import com.itda.core.model.order.ProductOrder;
import com.itda.shop.mapper.store.ReadableStoreMapper;
import com.itda.shop.model.order.ReadableProductOrder;
import com.itda.shop.model.order.ReadableStoreOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReadableStoreOrderMapper
        implements Mapper<Order, ReadableStoreOrder>
{
    private final ReadableProductOrderMapper readableProductOrderMapper;

    private final ReadableStoreMapper readableStoreMapper;

    @Override
    public ReadableStoreOrder createTarget()
    {
        return new ReadableStoreOrder();
    }

    @Override
    public ReadableStoreOrder merge(Order source, ReadableStoreOrder destination)
    {
        destination.setStore(readableStoreMapper.convert(source.getStore()));

        ImmutableList.Builder<ReadableProductOrder> builder = ImmutableList.builder();
        for (ProductOrder productOrder : source.getProductOrders()) {
            builder.add(readableProductOrderMapper.convert(productOrder));
        }
        destination.setProductOrders(builder.build());
        return destination;
    }
}
