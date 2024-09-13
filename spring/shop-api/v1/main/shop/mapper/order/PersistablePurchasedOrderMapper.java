package com.itda.shop.mapper.order;

import com.itda.core.business.constants.Constants;
import com.itda.core.business.exception.ConstraintException;
import com.itda.core.business.exception.GenericRuntimeException;
import com.itda.core.business.services.payment.PaymentService;
import com.itda.core.business.services.reference.CurrencyService;
import com.itda.core.business.services.reference.ZoneService;
import com.itda.core.common.CodeGenerator;
import com.itda.core.common.ErrorCode;
import com.itda.core.model.common.Mapper;
import com.itda.core.model.common.PersistableAddressMapper;
import com.itda.core.model.common.PersistableDeliveryAddress;
import com.itda.core.model.customer.Customer;
import com.itda.core.model.order.PurchasedOrder;
import com.itda.core.model.payment.PaymentType;
import com.itda.core.model.reference.Currency;
import com.itda.core.model.reference.Zone;
import com.itda.core.model.system.IntegrationConfiguration;
import com.itda.core.utils.DateUtil;
import com.itda.shop.model.order.PersistableOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.itda.core.common.ErrorCode.CONFIGURATION_INVALID;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableMap;

@Component
@RequiredArgsConstructor
public class PersistablePurchasedOrderMapper
        implements Mapper<PersistableOrder, PurchasedOrder>
{
    private final CurrencyService currencyService;
    private final ZoneService zoneService;
    private final PaymentService paymentService;
    private final PersistableAddressMapper addressMapper;

    private final Map<String, String> paymentTypeModules = new HashMap<>();

    private void loadConfiguration()
    {
        if (!paymentTypeModules.isEmpty()) {
            return;
        }
        paymentTypeModules.putAll(getPaymentTypeModules());
        for (PaymentType paymentType : PaymentType.values()) {
            if (!paymentTypeModules.containsKey(paymentType.name())) {
                throw new GenericRuntimeException(CONFIGURATION_INVALID, "payment module " + paymentType + " not configured");
            }
        }
    }

    // parent order
    @Override
    public PurchasedOrder merge(PersistableOrder source, PurchasedOrder target)
    {
        loadConfiguration();
        requireNonNull(source.getPaymentType(), "Payment cannot be null");
        PersistableDeliveryAddress delivery = source.getDelivery();

        target.setLocale(Constants.DEFAULT_LOCALE.getLanguage());

        Currency currency = currencyService.findByCodeOrThrow(source.getCurrency());

        String zoneCode = requireNonNull(delivery.getZone(), "Zone code is null");

        Zone zone = zoneService.findByCodeOrThrow(zoneCode);
        if (!zone.isShippingEnabled()) {
            throw new ConstraintException(ErrorCode.SHIPPING_ZONE_NOT_SUPPORTED, "Cannot delivery to this Zone: " + zoneCode);
        }
        /*
        if (!ENGLISH_ADDRESS.matcher(delivery.getStreetAddress()).matches()) {
            throw new ConstraintException(ErrorCode.ENGLISH_DELIVERY_STREET_ADDRESS_REQUIRED, "English Delivery Street Address is required");
        }
        if (StringUtils.hasText(delivery.getDetailAddress()) && !ENGLISH_ADDRESS.matcher(delivery.getDetailAddress()).matches()) {
            throw new ConstraintException(ErrorCode.ENGLISH_DELIVERY_STREET_ADDRESS_REQUIRED, "English Delivery Street Address is required");
        }
         */

        //Customer
        Customer customer = source.getCustomer();
        target.setCode(CodeGenerator.createNextId());

        target.setCustomerUsername(customer.getUsername());
        target.setCustomerEmail(customer.getEmail());
        target.setCustomerFullName(customer.getFullName());
        target.setCustomerTelephone(customer.getTelephone());

        //Billing billing = customer.getBilling();
        //target.setBilling(billing);

        /*
        if (!CollectionUtils.isEmpty(source.getAttributes())) {
            Set<OrderAttribute> attrs = new HashSet<>();
            for (OrderAttributeEntity attribute : source.getAttributes()) {
                if (!StringUtils.hasText(attribute.getStore())) {
                    OrderAttribute attr = new OrderAttribute();
                    attr.setKey(attribute.getKey());
                    attr.setValue(attribute.getValue());
                    attr.setOrder(target);
                    attrs.add(attr);
                }
            }
            target.setOrderAttributes(attrs);
        }
         */

        target.setPurchasedDateTime(DateUtil.now());
        target.setCurrency(currency.getCode());
        //need this
        //target.setStatus(OrderStatus.CHECKING_OUT);
        target.setPaymentModuleCode(paymentTypeModules.get(source.getPaymentType().name()));
        target.setPaymentType(source.getPaymentType());
        target.setComments(source.getComments());
        target.setDelivery(addressMapper.convert(source.getDelivery()));

        return target;
    }

    private Map<String, String> getPaymentTypeModules()
    {
        return paymentService.getPaymentModulesConfigured().stream()
                .collect(toUnmodifiableMap(IntegrationConfiguration::getType, IntegrationConfiguration::getModuleCode));
    }

    @Override
    public PurchasedOrder createTarget()
    {
        return new PurchasedOrder();
    }
}
