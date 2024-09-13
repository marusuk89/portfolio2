package com.itda.shop.store.api.v1.order;

import com.itda.core.business.constants.Constants;
import com.itda.core.business.exception.ResourceNotFoundException;
import com.itda.core.business.services.customer.CustomerService;
import com.itda.core.business.services.order.OrderService;
import com.itda.core.business.services.order.ShoppingCartService;
import com.itda.core.business.services.product.PricingService;
import com.itda.core.model.common.Delivery;
import com.itda.core.model.common.PersistableAddressMapper;
import com.itda.core.model.common.PersistableDeliveryAddress;
import com.itda.core.model.order.ShoppingCart;
import com.itda.core.model.order.StoreShoppingCart;
import com.itda.core.model.shipping.ShippingOption;
import com.itda.core.model.shipping.ShippingQuoteResult;
import com.itda.core.model.shipping.ShippingSummary;
import com.itda.shop.mapper.order.ReadableShippingSummaryMapper;
import com.itda.shop.model.order.ReadableShippingSummary;
import com.itda.shop.store.facade.customer.CustomerFacade;
import com.itda.shop.store.facade.order.OrderFacade;
import com.itda.shop.store.facade.order.ShoppingCartFacade;
import com.itda.shop.utils.LabelUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.itda.core.business.constants.Constants.DEFAULT_SHOPPING_CART;
import static java.util.Collections.emptyList;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Shopping Cart")
public class OrderShippingApi
{
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderShippingApi.class);

    private final CustomerFacade customerFacade;

    private final CustomerService customerService;

    private final OrderFacade orderFacade;

    private final ShoppingCartFacade shoppingCartFacade;

    private final LabelUtils messages;

    private final PricingService pricingService;

    private final OrderService orderService;

    private final ShoppingCartService shoppingCartService;

    private final PersistableAddressMapper persistableAddressMapper;

    private final ReadableShippingSummaryMapper readableShippingSummaryMapper;

    /**
     * Get shipping quote for a given shopping cart
     */
    @GetMapping(value = {"/carts/{cart}/shipping"})
    @Operation(summary = "고객 기본 배송지에 대한 쇼핑 카트의 배송료")
    @Parameters(value = {
            @Parameter(name = "cart", description = "장바구니 code", in = ParameterIn.PATH),
    })
    public ReadableShippingSummary shipping(@PathVariable String cart, HttpServletRequest request)
    {
        return shippingSummary(cart, Optional.empty());
    }

    /**
     * Get shipping quote based on postal code
     */
    @PostMapping(value = {"/carts/{cart}/shipping"})
    @Operation(summary = "배송지에 대한 쇼핑 카트의 배송료")
    @Parameters(value = {
            @Parameter(name = "cart", description = "장바구니 code", in = ParameterIn.PATH),
    })
    public ReadableShippingSummary shipping(
            @PathVariable String cart,
            @RequestBody @Valid PersistableDeliveryAddress address,
            HttpServletRequest request)
    {
        return shippingSummary(cart, Optional.of(persistableAddressMapper.convert(address)));
    }

    private ReadableShippingSummary shippingSummary(String cartCode, Optional<Delivery> delivery)
    {
        Locale locale = Constants.DEFAULT_LOCALE;

        // get customer id
        String username = customerFacade.authenticatedUsername();

        ShoppingCart cart;
        if (DEFAULT_SHOPPING_CART.equals(cartCode)) {
            cart = shoppingCartService.getOrCreateShoppingCart(username);
        }
        else {
            cart = shoppingCartService.findByCode(cartCode)
                    .orElseThrow(() -> ResourceNotFoundException.with("Cart", cartCode));
        }
        delivery = delivery.or(() -> customerFacade.authenticatedCustomer().getDefaultDelivery());

        for (StoreShoppingCart storeCart : cart.getStoreShoppingCarts()) {
            // FIXME
            Optional<ShippingQuoteResult> quote = orderService.getShippingQuote(delivery, storeCart);

            ShippingSummary summary = quote.map(ShippingQuoteResult::toShippingSummary).orElseGet(() -> new ShippingSummary());

            ReadableShippingSummary shippingSummary = readableShippingSummaryMapper.convert(summary);

            List<ShippingOption> options = quote.map(ShippingQuoteResult::getShippingOptions).orElse(emptyList());

            if (!CollectionUtils.isEmpty(options)) {
                for (ShippingOption shipOption : options) {
                    StringBuilder moduleName = new StringBuilder();
                    moduleName.append("module.shipping.").append(shipOption.getShippingModuleCode());

                    String carrier = messages.getMessage(moduleName.toString(), locale);

                    String note = messages.getMessage(moduleName.append(".note").toString(), locale, "");

                    shipOption.setDescription(carrier);
                    shipOption.setNote(note);

                    // option name
                    if (!StringUtils.isEmpty(shipOption.getOptionCode())) {
                        // try to get the translation
                        StringBuilder optionCodeBuilder = new StringBuilder()
                                .append("module.shipping.")
                                .append(shipOption.getShippingModuleCode());
                        try {
                            String optionName = messages.getMessage(optionCodeBuilder.toString(), locale);
                            shipOption.setOptionName(optionName);
                        }
                        catch (Exception e) { // label not found
                            LOGGER.warn("No shipping code found for " + optionCodeBuilder);
                        }
                    }
                }

                shippingSummary.setShippingOptions(options);
            }
        }

        // FIXME: Revisit here
        throw new UnsupportedOperationException();
    }
}
