package com.itda.shop.mapper.order;

import com.google.common.collect.ImmutableList;
import com.itda.core.business.services.payment.PaymentTransactionService;
import com.itda.core.model.common.DeliveryMapper;
import com.itda.core.model.common.Mapper;
import com.itda.core.model.order.Order;
import com.itda.core.model.order.PurchasedOrder;
import com.itda.shop.mapper.payment.ReadablePaymentTransactionMapper;
import com.itda.shop.model.order.ReadableOrderTotal;
import com.itda.shop.model.order.ReadablePurchasedOrder;
import com.itda.shop.model.order.ReadableStoreOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableMap;

@Component
@RequiredArgsConstructor
public class ReadablePurchasedOrderMapper
        implements Mapper<PurchasedOrder, ReadablePurchasedOrder>
{
    private final ReadableStoreOrderMapper readableStoreOrderMapper;

    private final DeliveryMapper deliveryMapper;

    private final ReadableOrderTotalMapper readableOrderTotalMapper;

    private final PaymentTransactionService paymentTransactionService;

    private final ReadablePaymentTransactionMapper readablePaymentTransactionMapper;

    @Override
    public ReadablePurchasedOrder merge(PurchasedOrder source, ReadablePurchasedOrder target)
    {
        requireNonNull(source, "OrderProduct cannot be null");
        requireNonNull(target, "ReadableOrderProduct cannot be null");

        target.setCode(source.getCode());
        target.setPurchasedDate(source.getPurchasedDateTime());
        target.setTotal(source.getTotal());
        target.setTotals(source.getOrderTotals().stream()
                .map(readableOrderTotalMapper::convert)
                .collect(toUnmodifiableMap(ReadableOrderTotal::getType, Function.identity())));
        target.setCustomerFullName(source.getCustomerFullName());
        target.setCustomerUsername(source.getCustomerUsername());
        target.setCustomerEmail(source.getCustomerEmail());
        target.setCustomerTelephone(source.getCustomerTelephone());
        target.setPaymentType(source.getPaymentType());
        paymentTransactionService.lastTransaction(source)
                .ifPresent(transaction ->
                        target.setPayment(readablePaymentTransactionMapper.convert(transaction)));
        target.setShippingWeight(source.getShippingWeight());

        ImmutableList.Builder<ReadableStoreOrder> builder = ImmutableList.builder();
        for (Order order : source.getOrders()) {
            builder.add(readableStoreOrderMapper.convert(order));
        }
        target.setStoreOrders(builder.build());
        target.setDelivery(deliveryMapper.convert(source.getDelivery()));

        return target;
    }

    @Override
    public ReadablePurchasedOrder createTarget()
    {
        return new ReadablePurchasedOrder();
    }
}
