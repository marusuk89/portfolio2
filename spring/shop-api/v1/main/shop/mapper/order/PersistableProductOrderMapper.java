package com.itda.shop.mapper.order;

import com.itda.core.business.exception.ServiceRuntimeException;
import com.itda.core.business.services.product.CategoryService;
import com.itda.core.business.services.shipping.ShippingQuoteService;
import com.itda.core.common.CodeGenerator;
import com.itda.core.model.common.Mapper;
import com.itda.core.model.order.Order;
import com.itda.core.model.order.ProductOrder;
import com.itda.core.model.order.ProductOrderPrice;
import com.itda.core.model.order.ProductOrderStatus;
import com.itda.core.model.order.ShoppingCartItem;
import com.itda.core.model.product.Category;
import com.itda.core.model.product.FinalPrice;
import com.itda.core.model.product.Product;
import com.itda.core.model.product.ProductPrice;
import com.itda.core.model.product.ProductVariant;
import com.itda.core.model.shipping.ShippingOrigin;
import com.itda.core.utils.DateUtil;
import io.airlift.json.JsonCodecFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

import static com.google.common.collect.Iterables.getFirst;
import static com.itda.core.business.utils.ProductPriceUtils.estimatedDeliveryDateTime;
import static java.util.Objects.requireNonNull;

@Component
@RequiredArgsConstructor
public class PersistableProductOrderMapper
        implements Mapper<ShoppingCartItem, ProductOrder>
{
    private final JsonCodecFactory codecFactory;
    private final CategoryService categoryService;
    private final ShippingQuoteService shippingQuoteService;

    @Override
    public ProductOrder merge(ShoppingCartItem source, ProductOrder target)
    {
        Product product = requireNonNull(source.getProduct(), "product must be set");
        Order order = requireNonNull(source.getOrder(), "order must be set");
        Category category = categoryService.findByCodeOrThrow(requireNonNull(getFirst(product.getCategories(), null), "Expect at least one product category"));

        target.setStatus(ProductOrderStatus.PAYMENT_WAITING);
        target.setOrder(order);
        target.setStore(order.getStore());
        target.setCustomerUsername(order.getCustomerUsername());
        if (target.isNew()) {
            target.setCode(CodeGenerator.createNextId());
        }
        target.setItemPrice(source.getItemPrice());
        target.setQuantity(source.getQuantity());
        target.setProduct(source.getProductCode());
        target.setProductName(product.getName());
        target.setEnglishName(categoryService.findByCodeOrThrow(product.getRootCategory()).getEnglishName());
        target.setProductReference(product.getReference());
        if (product.getDefaultImage() != null) {
            target.setProductImage(product.getDefaultImage().getCode());
        }

        target.setShippingClass(product.getShippingClass());
        target.setProductNationalCarrier(product.getNationalCarrier());
        target.setProductInternationalCarrier(product.getInternationalCarrier());
        target.setEstimatedDeliveryDateTime(estimatedDeliveryDateTime(product.getShippingClass()));

        if (StringUtils.hasText(source.getProductVariantCode())) {
            target.setProductVariant(source.getProductVariantCode());
            target.setProductVariantName(source.getProductVariant().map(ProductVariant::getName).orElse("N/A"));
        }

        FinalPrice finalPrice = source.getFinalPrice();
        if (finalPrice == null) {
            throw new ServiceRuntimeException("Object final price not populated in shoppingCartItem (source)");
        }
        //Default price
        ProductOrderPrice orderProductPrice = productOrderPrice(finalPrice, source.getQuantity());
        orderProductPrice.setProductOrder(target);

        target.setPrice(orderProductPrice);
        target.setDimensions(product.getDimensions());
        target.setPlaceDueDateTime(DateUtil.addWorkingDaysFromNow(3));

        if (product.getShippingOrigin() != null) {
            target.setShippingOrigin(codecFactory.jsonCodec(ShippingOrigin.class).toJson(product.getShippingOrigin()));
        }

        return target;
    }

    @Override
    public ProductOrder createTarget()
    {
        return new ProductOrder();
    }

    private ProductOrderPrice productOrderPrice(FinalPrice price, int quantity)
    {
        ProductOrderPrice orderProductPrice = new ProductOrderPrice();

        ProductPrice productPrice = price.getProductPrice();

        //orderProductPrice.setDefaultPrice(productPrice.isDefaultPrice());

        orderProductPrice.setProductPriceCode(productPrice.getCode());
        orderProductPrice.setProductPriceName(productPrice.getName());
        orderProductPrice.setProductItemPrice(price.getOriginalPrice());
        orderProductPrice.setVariantPriceDiff(price.getPriceDiff());
        orderProductPrice.setQuantity(quantity);
        orderProductPrice.setProductPrice(price.getOriginalPrice().multiply(BigDecimal.valueOf(quantity)));
        orderProductPrice.setProductDiscountPrice(price.getFinalPrice().multiply(BigDecimal.valueOf(quantity)));
        orderProductPrice.setProductDiscountStartDate(productPrice.getDiscountStartDate());
        orderProductPrice.setProductDiscountEndDate(productPrice.getDiscountEndDate());

        return orderProductPrice;
    }
}
