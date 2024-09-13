package com.itda.shop.mapper.order;

import com.itda.core.model.common.Mapper;
import com.itda.core.model.order.Order;
import com.itda.core.model.order.OrderAttribute;
import com.itda.core.model.store.Store;
import com.itda.shop.model.order.OrderAttributeEntity;
import com.itda.shop.model.order.PersistableOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

@Component
@RequiredArgsConstructor
public class PersistableOrderMapper
        implements Mapper<PersistableOrder, Order>
{
    // store order
    @Override
    public Order merge(PersistableOrder source, Order target, Store store)
    {
        requireNonNull(source.getPaymentType(), "Payment cannot be null");
        requireNonNull(source.getCustomer(), "Customer cannot be null");
        requireNonNull(source.getPurchasedOrder(), "PurchasedOrder cannot be null");

        //Customer
        target.setCode(source.getPurchasedOrder().getCode());
        target.setStore(store);
        target.setCustomerUsername(source.getCustomer().getUsername());
        target.setPurchasedOrder(source.getPurchasedOrder());

        //Billing billing = customer.getBilling();
        //target.setBilling(billing);

        if (!CollectionUtils.isEmpty(source.getAttributes())) {
            Set<OrderAttribute> attrs = new HashSet<>();
            for (OrderAttributeEntity attribute : source.getAttributes()) {
                if (store.getCode().equals(attribute.getStore())) {
                    OrderAttribute attr = new OrderAttribute();
                    attr.setKey(attribute.getKey());
                    attr.setValue(attribute.getValue());
                    attr.setOrder(target);
                    attrs.add(attr);
                }
            }
            target.setOrderAttributes(attrs);
        }

        return target;
    }

    @Override
    public Order createTarget()
    {
        return new Order();
    }

    @Override
    public Order merge(PersistableOrder source, Order destination)
    {
        return merge(source, destination, null);
    }
}
