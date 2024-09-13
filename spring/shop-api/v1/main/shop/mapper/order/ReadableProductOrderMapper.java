package com.itda.shop.mapper.order;

import com.itda.core.model.common.Address;
import com.itda.core.model.common.DeliveryMapper;
import com.itda.core.model.common.Mapper;
import com.itda.core.model.content.FileContentType;
import com.itda.core.model.order.Order;
import com.itda.core.model.order.ProductOrder;
import com.itda.core.model.order.ProductOrderDetailStatus;
import com.itda.core.model.order.PurchasedOrder;
import com.itda.core.model.shipping.PackageTracking;
import com.itda.core.model.shipping.ShippingOrigin;
import com.itda.core.model.store.Store;
import com.itda.shop.mapper.product.ProductSpecificationMapper;
import com.itda.shop.mapper.product.ReadableImageMapper;
import com.itda.shop.mapper.shipping.ReadablePackageTrackingMapper;
import com.itda.shop.mapper.shipping.ReadableShippingCarrierMapper;
import com.itda.shop.model.order.ReadableOrderTotal;
import com.itda.shop.model.order.ReadableProductOrder;
import com.itda.shop.model.shipping.ReadablePackageTracking;
import io.airlift.json.JsonCodecFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableMap;

@Component
@RequiredArgsConstructor
public class ReadableProductOrderMapper
        implements Mapper<ProductOrder, ReadableProductOrder>
{
    private final ReadableShippingCarrierMapper readableShippingCarrierMapper;

    private final ReadablePackageTrackingMapper readablePackageTrackingMapper;

    private final ReadableProductOrderPriceMapper readableOrderProductPriceMapper;

    private final ProductSpecificationMapper productSpecificationMapper;

    private final DeliveryMapper deliveryMapper;

    private final ReadableOrderTotalMapper readableOrderTotalMapper;

    private final ReadableImageMapper readableImageMapper;

    private final JsonCodecFactory codecFactory;

    @Override
    public ReadableProductOrder merge(ProductOrder source, ReadableProductOrder target)
    {
        requireNonNull(source, "OrderProduct cannot be null");
        requireNonNull(target, "ReadableOrderProduct cannot be null");

        Order order = source.getOrder();
        PurchasedOrder purchasedOrder = order.getPurchasedOrder();
        Store store = order.getStore();
        target.setOrder(order.getCode());
        target.setStatus(source.getStatus());
        target.setDetailStatus(ProductOrderDetailStatus.valueOf(source.getStatus(), source.getPlaceStatus(), source.getNationalTracking()));
        target.setPlaceStatus(source.getPlaceStatus());
        target.setClaimStatus(source.getClaimStatus());

        target.setId(source.getId());
        target.setCode(source.getCode());
        target.setQuantity(source.getQuantity());
        target.setPurchasedDate(purchasedOrder.getPurchasedDateTime());
        if (source.getPlacedDateTime() != null) {
            target.setPlacedDate(source.getPlacedDateTime());
        }
        target.setItemPrice(source.getItemPrice());

        // subtotal = price * quantity
        BigDecimal subTotal = source.getItemPrice();
        subTotal = subTotal.multiply(new BigDecimal(source.getQuantity()));
        target.setSubTotal(subTotal);

        target.setProduct(source.getProduct());
        target.setProductReference(source.getProductReference());
        target.setProductName(source.getProductName());
        if (source.getProductImage() != null) {
            target.setProductImage(readableImageMapper.convert(Pair.of(source.getProductImage(), FileContentType.PRODUCT)));
        }
        if (StringUtils.hasText(source.getProductVariant())) {
            target.setVariant(source.getProductVariant());
            target.setVariantName(source.getProductVariantName());
        }

        target.setComments(source.getComments());
        target.setShippingClass(source.getShippingClass());
        target.setShippingDueDateTime(source.getPlaceDueDateTime());
        target.setPlaceDueDateTime(source.getPlaceDueDateTime());
        target.setEstimatedDeliveryDateTime(source.getEstimatedDeliveryDateTime());
        target.setNationalDeliveryMethod(source.getNationalDeliveryMethod());
        target.setInternationalDeliveryMethod(source.getInternationalDeliveryMethod());

        PackageTracking tracking = source.getNationalTracking();
        if (tracking != null) {
            target.setNationalTracking(readablePackageTrackingMapper.convert(tracking));
        }
        else if (source.getProductNationalCarrier() != null) {
            ReadablePackageTracking readableTracking = new ReadablePackageTracking();
            readableTracking.setCarrier(readableShippingCarrierMapper.convert(source.getProductNationalCarrier()));
            target.setNationalTracking(readableTracking);
        }

        tracking = source.getInternationalTracking();
        if (tracking != null) {
            target.setInternationalTracking(readablePackageTrackingMapper.convert(tracking));
        }
        else if (source.getProductInternationalCarrier() != null) {
            ReadablePackageTracking readableTracking = new ReadablePackageTracking();
            readableTracking.setCarrier(readableShippingCarrierMapper.convert(source.getProductInternationalCarrier()));
            target.setInternationalTracking(readableTracking);
        }
        target.setCustomerFullName(purchasedOrder.getCustomerFullName());
        target.setCustomerUsername(purchasedOrder.getCustomerUsername());
        target.setCustomerEmail(purchasedOrder.getCustomerEmail());
        target.setCustomerTelephone(purchasedOrder.getCustomerTelephone());
        target.setPrice(readableOrderProductPriceMapper.convert(source.getPrice()));
        if (source.getDimensions() != null) {
            target.setProductSpecification(productSpecificationMapper.convert(source.getDimensions(), store));
        }
        target.setDelivery(deliveryMapper.convert(purchasedOrder.getDelivery()));
        target.setPaymentType(purchasedOrder.getPaymentType());
        target.setTotals(order.getOrderTotals().stream()
                .map(readableOrderTotalMapper::convert)
                .collect(toUnmodifiableMap(ReadableOrderTotal::getType, Function.identity())));

        if (StringUtils.hasText(source.getShippingOrigin())) {
            ShippingOrigin shippingOrigin = codecFactory.jsonCodec(ShippingOrigin.class).fromJson(source.getShippingOrigin());
            Address address = new Address();
            address.setStreetAddress(shippingOrigin.getStreetAddress());
            address.setCity(shippingOrigin.getCity());
            if (shippingOrigin.getCountry() != null) {
                address.setCountry(shippingOrigin.getCountry());
            }

            if (!StringUtils.isEmpty(shippingOrigin.getStateProvince())) {
                address.setStateProvince(shippingOrigin.getStateProvince());
            }
            address.setPostalCode(shippingOrigin.getPostalCode());
            target.setShippingOrigin(address);
        }

        return target;
    }

    @Override
    public ReadableProductOrder createTarget()
    {
        return new ReadableProductOrder();
    }
}
