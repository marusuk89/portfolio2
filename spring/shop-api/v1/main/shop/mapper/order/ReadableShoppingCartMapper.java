package com.itda.shop.mapper.order;

import com.itda.core.business.services.order.ShoppingCartCalculationService;
import com.itda.core.business.services.product.PricingService;
import com.itda.core.model.common.Mapper;
import com.itda.core.model.order.OrderTotal;
import com.itda.core.model.order.OrderTotalSummary;
import com.itda.core.model.order.OrderTotalType;
import com.itda.core.model.order.ShoppingCart;
import com.itda.core.model.order.ShoppingCartItem;
import com.itda.core.model.order.StoreShoppingCart;
import com.itda.core.model.shipping.ShippingSummary;
import com.itda.shop.mapper.store.ReadableStoreMapper;
import com.itda.shop.model.order.ReadableOrderTotal;
import com.itda.shop.model.order.ReadableShoppingCart;
import com.itda.shop.model.order.ReadableShoppingCartItem;
import com.itda.shop.model.order.ReadableStoreShoppingCartItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.itda.core.business.constants.Constants.DEFAULT_CURRENCY;
import static com.itda.core.business.constants.Constants.DEFAULT_LOCALE;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableMap;

@Component
@RequiredArgsConstructor
public class ReadableShoppingCartMapper
        implements Mapper<ShoppingCart, ReadableShoppingCart>
{
    private final ShoppingCartCalculationService shoppingCartCalculationService;

    private final PricingService pricingService;

    private final ReadableStoreMapper readableStoreMapper;

    private final ReadableShoppingCartItemMapper readableShoppingCartItemMapper;

    private final ReadableOrderTotalMapper readableOrderTotalMapper;

    @Override
    public ReadableShoppingCart createTarget()
    {
        return new ReadableShoppingCart();
    }

    @Override
    public ReadableShoppingCart merge(ShoppingCart source, ReadableShoppingCart destination)
    {
        requireNonNull(source, "ShoppingCart cannot be null");
        requireNonNull(destination, "ReadableShoppingCart cannot be null");

        destination.setCode(source.getCode());
        int cartQuantity = 0;
        int cartProductQuantity = 0;

        destination.setCustomer(source.getCustomerUsername());

        // TODO: Promotion
        /*
        if (!StringUtils.isEmpty(source.getPromoCode())) {
            Date promoDateAdded = source.getPromoAdded();// promo valid 1 day
            if (promoDateAdded == null) {
                promoDateAdded = new Date();
            }
            Instant instant = promoDateAdded.toInstant();
            ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
            LocalDate date = zdt.toLocalDate();
            // date added < date + 1 day
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            if (date.isBefore(tomorrow)) {
                destination.setPromoCode(source.getPromoCode());
            }
        }
         */

        // OrderTotalSummary contains all calculations

        OrderTotalSummary orderSummary = shoppingCartCalculationService.calculate(source);

        for (StoreShoppingCart storeCart : source.getStoreShoppingCarts()) {
            List<ReadableShoppingCartItem> items = new ArrayList<>();
            int storeCartQuantity = 0;
            int storeCartProductQuantity = 0;
            for (ShoppingCartItem item : storeCart.getCartItems()) {
                ReadableShoppingCartItem shoppingCartItem = readableShoppingCartItemMapper.convert(item);

                cartQuantity = cartQuantity + item.getQuantity();
                storeCartQuantity = storeCartQuantity + item.getQuantity();
                cartProductQuantity = cartProductQuantity + 1;
                storeCartProductQuantity = storeCartProductQuantity + 1;

                items.add(shoppingCartItem);
            }

            if (!items.isEmpty()) {
                Collection<OrderTotal> totals = orderSummary.getStoreTotals().get(storeCart.getStore());
                OrderTotal storeTotal = totals.stream().filter(ot -> ot.getType() == OrderTotalType.STORE_TOTAL).findFirst().get();
                OrderTotal storeSubTotal = totals.stream().filter(ot -> ot.getType() == OrderTotalType.SUBTOTAL).findFirst().get();
                ReadableStoreShoppingCartItem storeItem = new ReadableStoreShoppingCartItem();
                storeItem.setStore(readableStoreMapper.convert(storeCart.getStore()));
                storeItem.setItems(items);
                storeItem.setQuantity(storeCartQuantity);
                storeItem.setProductQuantity(storeCartProductQuantity);

                storeItem.setSubtotal(storeSubTotal.getAmount());
                storeItem.setDisplaySubTotal(pricingService.getDisplayAmount(storeSubTotal.getAmount(), DEFAULT_LOCALE, DEFAULT_CURRENCY));

                storeItem.setTotal(storeTotal.getAmount());
                storeItem.setDisplayTotal(pricingService.getDisplayAmount(storeTotal.getAmount(), DEFAULT_LOCALE, DEFAULT_CURRENCY));

                storeItem.setTotals(totals.stream().map(readableOrderTotalMapper::convert).collect(toUnmodifiableMap(ReadableOrderTotal::getType, Function.identity())));
                destination.getStoreItems().add(storeItem);
            }
        }

        if (!CollectionUtils.isEmpty(orderSummary.getTotals())) {
            if (!orderSummary.getTotals().stream().anyMatch(t -> t.getType() == OrderTotalType.DISCOUNT)) {
                // no promo coupon applied
                destination.setPromoCodes(null);
            }

            Map<OrderTotalType, ReadableOrderTotal> totals = orderSummary.getTotals().stream()
                    .map(readableOrderTotalMapper::convert)
                    .collect(toUnmodifiableMap(ReadableOrderTotal::getType, Function.identity()));
            destination.setTotals(totals);
        }

        destination.setSubtotal(orderSummary.getSubTotal());
        destination.setDisplaySubTotal(pricingService.getDisplayAmount(orderSummary.getSubTotal(), DEFAULT_LOCALE, DEFAULT_CURRENCY));

        destination.setTotal(orderSummary.getTotal());
        destination.setDisplayTotal(pricingService.getDisplayAmount(orderSummary.getTotal(), DEFAULT_LOCALE, DEFAULT_CURRENCY));

        destination.setQuantity(cartQuantity);
        destination.setProductQuantity(cartProductQuantity);
        destination.setShippingWeight(orderSummary.getShippingSummary().map(ShippingSummary::getTotalWeight).orElse(0d));

        /*
        if (source.getOrder() != null) {
            destination.setOrder(source.getOrder().getCode()); // TODO: Order code
        }
         */

        return destination;
    }
}
