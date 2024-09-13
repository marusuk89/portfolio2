package com.itda.shop.store.api.v1.order;

import com.itda.core.business.constants.Constants;
import com.itda.core.business.exception.ConstraintException;
import com.itda.core.business.services.order.OrderService;
import com.itda.core.business.services.order.ProductOrderService;
import com.itda.core.business.services.shipping.PackageTrackingService;
import com.itda.core.business.services.shipping.ShippingCarrierService;
import com.itda.core.model.customer.Customer;
import com.itda.core.model.order.CancelReturnReason;
import com.itda.core.model.order.DeliveryStatus;
import com.itda.core.model.order.PlaceOrderStatus;
import com.itda.core.model.order.ProductOrder;
import com.itda.core.model.order.ProductOrderCount;
import com.itda.core.model.order.ProductOrderCountHistory;
import com.itda.core.model.order.ProductOrderCountHistory.ProductOrderRevenueHistory;
import com.itda.core.model.order.ProductOrderCountHistoryType;
import com.itda.core.model.order.ProductOrderCriteria;
import com.itda.core.model.order.ProductOrderDetailStatus;
import com.itda.core.model.order.ProductOrderEvent;
import com.itda.core.model.order.PurchasedOrderCriteria;
import com.itda.core.model.order.message.CancelProductOrder;
import com.itda.core.model.order.message.DelayDispatchProductOrder;
import com.itda.core.model.order.message.DispatchProductOrder;
import com.itda.core.model.order.message.ModifyDeliveryAddress;
import com.itda.core.model.order.message.ProcessProductOrder;
import com.itda.core.model.order.message.ReturnProductOrder;
import com.itda.core.model.shipping.PackageTracking;
import com.itda.core.model.shipping.ShippingCarrier;
import com.itda.core.model.shipping.ShippingClass;
import com.itda.core.model.shipping.ShippingType;
import com.itda.core.model.user.AdminUser;
import com.itda.core.utils.DateUtil;
import com.itda.shop.mapper.shipping.ReadablePackageTrackingMapper;
import com.itda.shop.model.entity.ReadableList;
import com.itda.shop.model.order.PersistableProductOrder;
import com.itda.shop.model.order.ReadableProductOrder;
import com.itda.shop.model.order.ReadablePurchasedOrder;
import com.itda.shop.model.shipping.ReadablePackageTracking;
import com.itda.shop.store.facade.customer.CustomerFacade;
import com.itda.shop.store.facade.order.OrderFacade;
import com.itda.shop.store.facade.user.AdminUserFacade;
import com.itda.shop.utils.MarkdownDescription;
import com.itda.shop.utils.SwaggerMarkdown;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static com.itda.core.common.ErrorCode.ALREADY_DELAYED_DELIVERY;
import static com.itda.core.common.ErrorCode.WRONG_TRACING_NUMBER;
import static java.lang.String.format;
import static java.util.stream.Collectors.toUnmodifiableMap;

@RestController
@RequestMapping("/api/v1")
@MarkdownDescription(markdown = SwaggerMarkdown.ORDER_DESCRIPTION)
@RequiredArgsConstructor
public class OrderApi
{
    private final AdminUserFacade userFacade;
    private final CustomerFacade customerFacade;
    private final ShippingCarrierService shippingCarrierService;
    private final ProductOrderService productOrderService;
    private final PackageTrackingService packageTrackingService;
    private final OrderService orderService;

    private final OrderFacade orderFacade;

    private final ReadablePackageTrackingMapper readablePackageTrackingMapper;

    // Customer Order processing

    @GetMapping(value = { "/product-orders" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "고객의 상품주문 검색조건에 맞는 상품 주문 목록, Deprecated, use /orders/product-orders", deprecated = true)
    public ReadableList<ReadableProductOrder> orderListByCustomer(
            @ParameterObject ProductOrderCriteria criteria)
    {
        Customer customer = customerFacade.authenticatedCustomer();
        ReadableList<ReadableProductOrder> orders = orderFacade.searchProductOrderList(customer, criteria);

        return orders;
    }

    @GetMapping(value = { "/orders/product-orders" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "고객의 상품주문 검색조건에 맞는 상품 주문 목록")
    public ReadableList<ReadableProductOrder> productOrderListByCustomer(
            @ParameterObject ProductOrderCriteria criteria)
    {
        Customer customer = customerFacade.authenticatedCustomer();
        ReadableList<ReadableProductOrder> orders = orderFacade.searchProductOrderList(customer, criteria);

        return orders;
    }

    @GetMapping(value = { "/orders/purchased-orders" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "고객의 구매주문 검색조건에 맞는 구매 주문 목록")
    public ReadableList<ReadablePurchasedOrder> purchasedOrderListByCustomer(
            @ParameterObject PurchasedOrderCriteria criteria)
    {
        Customer customer = customerFacade.authenticatedCustomer();
        ReadableList<ReadablePurchasedOrder> orders = orderFacade.searchPurchasedOrderList(customer, criteria);

        return orders;
    }

    @PostMapping(value = { "/orders/product-orders/cancel" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "구매자 상품 취소 요청")
    public void cancelProductOrdersByCustomer(
            @RequestBody @NotEmpty List<@Valid CancelProductOrder> eventDetails)
    {
        customerFacade.authenticatedUsername();

        Map<String, CancelReturnReason> cancelReasons = eventDetails.stream()
                .collect(toUnmodifiableMap(CancelProductOrder::getProductOrder, CancelProductOrder::getReason));
        Predicate<ProductOrder> condition = po -> {
            CancelReturnReason reason = cancelReasons.get(po.getCode());
            if (reason == CancelReturnReason.DELAYED_DELIVERY_BY_PURCHASER) {
                if (po.getPlaceStatus() != PlaceOrderStatus.CANCEL) {
                    throw new ConstraintException(format("ProductOrder(%s) cannot apply cancel event(%s) from place status(%s)",
                            po.getCode(), reason, po.getPlaceStatus()));
                }
            }
            else {
                throw new UnsupportedOperationException("Cancellation with " + reason.name());
            }
            return true;
        };
        processProductOrdersByCustomer(ProductOrderEvent.CANCEL, eventDetails, condition);
    }

    @PostMapping(value = { "/orders/product-orders/return" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "구매자 상품 반품 요청")
    public void returnProductOrdersByCustomer(
            @RequestBody @NotEmpty List<@Valid ReturnProductOrder> eventDetails)
    {
        customerFacade.authenticatedUsername();

        Map<String, CancelReturnReason> returnReason = eventDetails.stream()
                .collect(toUnmodifiableMap(ReturnProductOrder::getProductOrder, ReturnProductOrder::getReason));
        Predicate<ProductOrder> condition = po -> {
            CancelReturnReason reason = returnReason.get(po.getCode());
            throw new UnsupportedOperationException("Cancellation with " + reason.name());
        };
        processProductOrdersByCustomer(ProductOrderEvent.CANCEL, eventDetails, condition);
    }

    @PostMapping(value = { "/orders/product-orders/purchase-decide" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "구매자 상품 구매 확정")
    public void purchaseDecideProductOrdersByCustomer(
            @RequestBody @NotEmpty List<@Valid ProcessProductOrder> eventDetails)
    {
        customerFacade.authenticatedUsername();

        processProductOrdersByCustomer(ProductOrderEvent.PURCHASE_DECIDE, eventDetails);
    }

    // Admin Order processing
    @GetMapping(value = { "/admin/product-orders" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "판매자의 상점에 해당 하는 검색조건에 맞는 상품 주문 목록")
    public ReadableList<ReadableProductOrder> orderListByAdmin(
            @ParameterObject ProductOrderCriteria criteria)
    {
        AdminUser adminUser = userFacade.authenticatedUser();
        // TODO: Authorize
        // authorizationUtils.authorizeUser(user, Stream.of(Constants.GROUP_SUPERADMIN, Constants.GROUP_ADMIN,
        //        Constants.GROUP_ADMIN_ORDER, Constants.GROUP_ADMIN_RETAIL).collect(Collectors.toList()), merchantStore);
        ReadableList<ReadableProductOrder> orders = orderFacade.searchProductOrderList(adminUser.getStore(), criteria);

        return orders;
    }

    @Deprecated
    @GetMapping(value = { "/admin/product-orders/count" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "판매자의 상점에 주문 상태별 수량", description = "{ 'COUNTS' : {ProductOrderDetailStatus : { ShippingClass : Count }}, 'PRODUCT_NAMES' : {ProductOrderDetailStatus : ProductName} }")
    public Map<String, Object> orderCountsByAdmin()
    {
        AdminUser adminUser = userFacade.authenticatedStoreAdmin();

        return buildCountResult(productOrderService.countByStore(adminUser.getStore()), false);
    }

    @Deprecated
    @GetMapping(value = { "/admin/product-orders/delay-count" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "판매자의 상점에 처리 지연 상태별 수량", description = "{ 'COUNTS' : {ProductOrderDetailStatus : { ShippingClass : Count }}, 'PRODUCT_NAMES' : {ProductOrderDetailStatus : ProductName} }")
    public Map<String, Object> shippingDelayCountByAdmin()
    {
        AdminUser adminUser = userFacade.authenticatedStoreAdmin();

        return buildCountResult(productOrderService.countByStore(adminUser.getStore()), true);
    }

    private Map<String, Object> buildCountResult(ProductOrderCount data, boolean delay)
    {
        Map<ProductOrderDetailStatus, Map<ShippingClass, Long>> counts = new HashMap<>();
        Map<ProductOrderDetailStatus, String> names = new HashMap<>();
        for (ProductOrderDetailStatus status : ProductOrderDetailStatus.values()) {
            Map<ShippingClass, Long> count = new HashMap<>();
            for (ShippingClass shippingClass : ShippingClass.values()) {
                count.put(shippingClass, data.countOf(status, shippingClass, delay));
            }
            counts.put(status, count);
            String productName = data.getProductNameOf(status, delay);
            if (productName != null) {
                names.put(status, productName);
            }
        }
        return Map.of("COUNT", Map.copyOf(counts), "PRODUCT_NAMES", Map.copyOf(names));
    }

    @GetMapping(value = { "/admin/product-orders/counts" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "판매자의 상점에 주문 상태별 수량", description = "{ ProductOrderDetailStatus : { ShippingClass : {'ALL' : {count: 10, productName: 'name' }, {'DELAY': {...} }},. }")
    public ProductOrderCount orderCountByAdmin()
    {
        AdminUser adminUser = userFacade.authenticatedUser();

        return productOrderService.countByStore(adminUser.getStore());
    }

    @GetMapping(value = { "/admin/product-orders/counts-history" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "판매자의 상점에 실시간/일간 주문 수량")
    @Parameters(value = {
            @Parameter(name = "types", array = @ArraySchema(schema = @Schema(description = "데이터 타입", enumAsRef = true, implementation = ProductOrderCountHistoryType.class))),
            @Parameter(name = "after", in = ParameterIn.QUERY, description = "해당 시간 이후 데이터만 조회"),
            @Parameter(name = "until", in = ParameterIn.QUERY, description = "해당 시간 까지 데이터만 조회")
    })
    public List<ProductOrderCountHistory> orderCountHistoryByAdmin(
            @RequestParam(required = false) Set<ProductOrderCountHistoryType> types,
            @RequestParam(required = false) OffsetDateTime after,
            @RequestParam(required = false) OffsetDateTime until)
    {
        AdminUser adminUser = userFacade.authenticatedUser();
        if (types == null) {
            types = Set.of(ProductOrderCountHistoryType.values());
        }
        if (after == null) {
            after = DateUtil.now().minus(Duration.ofDays(14));
        }
        if (until == null) {
            until = DateUtil.now();
        }

        return productOrderService.listCountHistory(adminUser.getStore(), types, after, until);
    }

    @GetMapping(value = { "/admin/product-orders/revenue-history" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "판매자의 상점에 실시간/일간 주문 수량")
    @Parameters(value = {
            @Parameter(name = "after", in = ParameterIn.QUERY, description = "해당 시간 이후 데이터만 조회"),
            @Parameter(name = "until", in = ParameterIn.QUERY, description = "해당 시간 까지 데이터만 조회")
    })
    public List<ProductOrderRevenueHistory> orderRevenueHistoryByAdmin(
            @RequestParam(required = false) OffsetDateTime after,
            @RequestParam(required = false) OffsetDateTime until)
    {
        AdminUser adminUser = userFacade.authenticatedUser();
        if (after == null) {
            after = DateUtil.now().minus(Duration.ofDays(14));
        }
        if (until == null) {
            until = DateUtil.now();
        }

        return productOrderService.listCountHistory(adminUser.getStore(), Set.of(ProductOrderCountHistoryType.DAILY), after, until)
                .stream().map(h -> {
                    ProductOrderRevenueHistory revenue = new ProductOrderRevenueHistory();
                    revenue.setQuantity(h.getPaid() - h.getCanceled() - h.getReturned());
                    revenue.setRevenue(h.getRevenue());
                    revenue.setDate(h.getCreatedTimestamp().atOffset(Constants.DEFAULT_ZONE_OFFSET).toLocalDate());
                    return revenue;
                })
                .toList();
    }

    @PostMapping(value = { "/admin/product-orders/{productOrder}/events/{event}" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "주문 처리 Event 발생, 에러 처리용")
    @Parameters(value = {
            @Parameter(name = "productOrder", in = ParameterIn.PATH, description = "상품 주문 CODE"),
            @Parameter(name = "event", in = ParameterIn.PATH, description = "Event name", schema = @Schema(enumAsRef = true))
    })
    public void triggerProductOrderEvent(
            @PathVariable ProductOrder productOrder,
            @PathVariable ProductOrderEvent event,
            @RequestBody @Valid ProcessProductOrder eventDetail)
    {
        if (productOrder.getCode() != eventDetail.getProductOrder()) {
            throw new IllegalArgumentException("Detail productOrder.code mismatch");
        }
        processProductOrders(event, List.of(eventDetail));
    }

    @PostMapping(value = { "/admin/product-orders/place" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "상품 발주 처리")
    public void placeProductOrders(
            @RequestBody @NotEmpty List<@Valid ProcessProductOrder> eventDetails)
    {
        processProductOrders(ProductOrderEvent.PLACE, eventDetails);
    }

    @PostMapping(value = { "/admin/product-orders/cancel" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "판매자/관리자 상품 취소 처리")
    public void cancelProductOrders(
            @RequestBody @NotEmpty List<@Valid CancelProductOrder> eventDetails)
    {
        Map<String, CancelReturnReason> cancelReasons = eventDetails.stream()
                .collect(toUnmodifiableMap(CancelProductOrder::getProductOrder, CancelProductOrder::getReason));
        Predicate<ProductOrder> condition = po -> {
            CancelReturnReason reason = cancelReasons.get(po.getCode());
            throw new UnsupportedOperationException("Cancellation with " + reason.name());
        };
        processProductOrders(ProductOrderEvent.CANCEL, eventDetails, condition);
    }

    @PostMapping(value = { "/admin/product-orders/approve-cancel" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "판매자/관리자 상품 취소 승인")
    public void approveCancelProductOrders(
            @RequestBody @NotEmpty List<@Valid ProcessProductOrder> eventDetails)
    {
        processProductOrders(ProductOrderEvent.APPROVE_CANCEL, eventDetails);
    }

    @PostMapping(value = { "/admin/product-orders/approve-return" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "판매자/관리자 상품 반품 승인")
    public void approveReturnProductOrders(
            @RequestBody @NotEmpty List<@Valid ProcessProductOrder> eventDetails)
    {
        processProductOrders(ProductOrderEvent.APPROVE_RETURN, eventDetails);
    }

    @PostMapping(value = { "/admin/product-orders/dispatch" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "상품 배송 처리")
    public void dispatchProductOrders(
            @RequestBody @NotEmpty List<@Valid DispatchProductOrder> eventDetails)
    {
        Set<String> checkedTrackingNumbers = new HashSet<>();
        eventDetails.forEach(detail -> {
            ShippingCarrier carrier = shippingCarrierService.findByCodeOrThrow(detail.getCarrier());

            if (checkedTrackingNumbers.add(detail.getTrackingNumber())) {
                if (!packageTrackingService.verifyTrackingNumber(carrier, detail.getTrackingNumber())) {
                    throw new ConstraintException(WRONG_TRACING_NUMBER,
                            format("Wrong Tracking Number Carrier:%s, Number:%s", carrier.getName(), detail.getTrackingNumber()));
                }
            }
        });
        processProductOrders(ProductOrderEvent.DISPATCH, eventDetails);
    }

    @PutMapping(value = { "/admin/product-orders/delay-dispatch" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "상품 발송 지연 처리")
    public void delayDispatchProductOrders(
            @RequestBody @NotEmpty List<@Valid DelayDispatchProductOrder> eventDetails)
    {
        processProductOrders(ProductOrderEvent.DELAY_DISPATCH, eventDetails, po -> {
            if (po.getDelayedDispatchReason() != null) {
                throw new ConstraintException(ALREADY_DELAYED_DELIVERY, po.getCode());
            }
            return true;
        });
    }

    @PutMapping(value = { "/admin/product-orders/modify-delivery-address" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "상품 배송지 변경 처리")
    public void modifyDeliveryAddress(
            @RequestBody @NotEmpty List<@Valid ModifyDeliveryAddress> eventDetails)
    {
        Set<Long> checkedOrders = new HashSet<>();
        processProductOrders(ProductOrderEvent.MODIFY_DELIVERY_ADDRESS, eventDetails, po -> checkedOrders.add(po.getOrder().getId()));
    }

    private <T extends ProcessProductOrder> void processProductOrders(ProductOrderEvent event, List<T> eventDetails)
    {
        processProductOrders(event, eventDetails, po -> true);
    }

    private <T extends ProcessProductOrder> void processProductOrdersByCustomer(ProductOrderEvent event, List<T> eventDetails)
    {
        processProductOrdersByCustomer(event, eventDetails, po -> true);
    }

    private <T extends ProcessProductOrder> void processProductOrders(
            ProductOrderEvent event,
            List<T> eventDetails,
            Predicate<ProductOrder> conditions)
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String sender = auth == null ? null : auth.getName();
        conditions = conditions.and(po -> {
            userFacade.authorizeStore(po.getStore());
            return true;
        });
        orderService.sendOrderProcessingMessages(sender, event, eventDetails, conditions);
    }

    private <T extends ProcessProductOrder> void processProductOrdersByCustomer(
            ProductOrderEvent event,
            List<T> eventDetails,
            Predicate<ProductOrder> conditions)
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String sender = auth == null ? null : auth.getName();
        conditions = conditions.and(po -> {
            customerFacade.authorizeUsername(po.getCustomerUsername());
            return true;
        });
        orderService.sendOrderProcessingMessages(sender, event, eventDetails, conditions);
    }

    @GetMapping(value = { "/admin/product-orders/{productOrder}/track-package" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "실시간 배송 정보 조회")
    @Parameters(value = {
            @Parameter(name = "productOrder", in = ParameterIn.PATH, description = "상품 주문 CODE"),
            @Parameter(name = "shippingType", in = ParameterIn.QUERY, description = "국내/국제 배송 여부", schema = @Schema(enumAsRef = true, defaultValue = "NATIONAL", implementation = ShippingType.class))
    })
    public ReadablePackageTracking trackPackageByAdmin(
            @PathVariable ProductOrder productOrder,
            @RequestParam ShippingType shippingType)
    {
        userFacade.authorizeStore(productOrder.getStore());

        return trackPackage(productOrder, shippingType);
    }

    @GetMapping(value = { "/product-orders/{productOrder}/track-package" })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "구매자 실시간 배송 정보 조회")
    @Parameters(value = {
            @Parameter(name = "productOrder", in = ParameterIn.PATH, description = "상품 주문 CODE"),
            @Parameter(name = "shippingType", in = ParameterIn.QUERY, description = "국내/국제 배송 여부", schema = @Schema(enumAsRef = true, defaultValue = "NATIONAL", implementation = ShippingType.class))
    })
    public ReadablePackageTracking trackPackageByCustomer(
            @PathVariable ProductOrder productOrder,
            @RequestParam ShippingType shippingType)
    {
        customerFacade.authorizeUsername(productOrder.getCustomerUsername());

        return trackPackage(productOrder, shippingType);
    }

    private ReadablePackageTracking trackPackage(ProductOrder productOrder, ShippingType shippingType)
    {
        PackageTracking tracking = packageTrackingService.trackPackage(productOrder, shippingType);
        if (tracking == null) {
            tracking = new PackageTracking();
            tracking.setDispatchDateTime(productOrder.getPlacedDateTime());
            tracking.setDeliveryStatus(DeliveryStatus.NOT_TRACKING);
        }
        return readablePackageTrackingMapper.convert(tracking);
    }

    @ResponseStatus(HttpStatus.OK)
    @PatchMapping(value = {"/admin/product-orders/{productOrder}"})
    @Operation(summary = "상품 주문 수정")
    @Parameters(value = {
        @Parameter(name = "productOrder", description = "상품 주문 CODE", in = ParameterIn.PATH)
    })
    public void productOrderUpdate(
            @PathVariable ProductOrder productOrder,
            @Valid @RequestBody PersistableProductOrder persistableProductOrder)
    {
        userFacade.authorizeStore(productOrder.getStore());

        orderFacade.update(productOrder, persistableProductOrder);
    }
}
