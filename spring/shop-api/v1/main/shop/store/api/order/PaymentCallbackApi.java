package com.itda.shop.store.api.v1.order;

import com.itda.core.business.exception.ConstraintException;
import com.itda.core.business.exception.GenericRuntimeException;
import com.itda.core.business.exception.ResourceConflictException;
import com.itda.core.business.exception.ResourceNotFoundException;
import com.itda.core.business.modules.payment.toss.TossPaymentError;
import com.itda.core.business.services.aws.AwsSnsService;
import com.itda.core.business.services.customer.CustomerService;
import com.itda.core.business.services.order.OrderService;
import com.itda.core.business.services.order.PurchasedOrderService;
import com.itda.core.business.services.order.ShoppingCartService;
import com.itda.core.business.services.payment.PaymentService;
import com.itda.core.business.services.payment.PaymentTransactionService;
import com.itda.core.common.ErrorCode;
import com.itda.core.model.common.QMessage;
import com.itda.core.model.customer.Customer;
import com.itda.core.model.order.ProductOrderEvent;
import com.itda.core.model.order.ProductOrderStatus;
import com.itda.core.model.order.PurchasedOrder;
import com.itda.core.model.order.PurchasedOrderStatus;
import com.itda.core.model.order.ShoppingCart;
import com.itda.core.model.order.ShoppingCartItem;
import com.itda.core.model.order.message.PurchasedOrderProcessingMessage;
import com.itda.core.model.payment.Payment;
import com.itda.core.model.payment.PaymentTransaction;
import com.itda.core.model.payment.PaymentTransactionType;
import com.itda.core.utils.DateUtil;
import com.itda.shop.mapper.payment.ReadablePaymentTransactionMapper;
import com.itda.shop.model.payment.ReadablePaymentTransaction;
import io.airlift.json.JsonCodecFactory;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static com.itda.core.common.ErrorCode.PAYMENT_FAILURE;
import static java.lang.String.format;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
@RequiredArgsConstructor
public class PaymentCallbackApi
{
    private final AwsSnsService awsSnsService;
    private final PaymentService paymentService;
    private final PaymentTransactionService paymentTransactionService;

    private final PurchasedOrderService purchasedOrderService;
    private final OrderService orderService;
    private final CustomerService customerService;
    private final ShoppingCartService shoppingCartService;

    private final ReadablePaymentTransactionMapper transactionMapper;

    @Value("${aws.sns.topic.orderPaymentSuccess}")
    private final String orderPaymentSuccess;

    private final JsonCodecFactory codecFactory;
    /*
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/payments/toss/prepare?cart")
    public PaymentPrepareResponse preparePayment()
    {
        String orderId = new
    }
     */

    //https://{ORIGIN}/success?paymentKey={PAYMENT_KEY}&orderId={ORDER_ID}&amount={AMOUNT}
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/payments/toss/success")
    @Operation(hidden = true)
    public ReadablePaymentTransaction paymentSuccess(
            @RequestParam String orderId,
            @RequestParam String paymentType,
            @RequestParam String paymentKey,
            @RequestParam long amount)
    {
        PurchasedOrder purchasedOrder = purchasedOrderService.getByCodeOrThrow(orderId);

        Customer customer = customerService.findByUsername(purchasedOrder.getCustomerUsername())
                .orElseThrow(() -> ResourceNotFoundException.with("Customer", purchasedOrder.getCustomerUsername()));

        ShoppingCart cart = shoppingCartService.findByCode(purchasedOrder.getShoppingCartCode())
                .orElseThrow(() -> ResourceNotFoundException.with("ShoppingCart", purchasedOrder.getShoppingCartCode()));
        // check inventory
        cart.getAllShoppingCartItems().forEach(orderService::checkInventoryOrThrow);

        PaymentTransaction paymentTransaction = paymentTransactionService.lastTransaction(purchasedOrder)
                .orElseThrow(() -> ResourceNotFoundException.with("PaymentTransaction", purchasedOrder.getCode()));

        if (paymentTransaction.getTransactionType() != PaymentTransactionType.INIT) {
            throw new ConstraintException("PaymentTransaction is not INIT");
        }
        if (paymentTransaction.getAmount().longValue() != amount) {
            throw new ResourceConflictException(ErrorCode.CART_AMOUNT_MISMATCH, format("Expected %s, but Payment %,d", paymentTransaction.getAmount(), amount));
        }

        Payment payment = new Payment();
        payment.setPaymentType(purchasedOrder.getPaymentType());
        payment.setModuleName(purchasedOrder.getPaymentModuleCode());
        payment.setTransactionType(paymentTransaction.getNextTransactionType());
        payment.setAmount(paymentTransaction.getAmount());
        payment.setPaymentMetaData(Map.of(
                "orderId", orderId,
                "paymentType", paymentType,
                "paymentKey", paymentKey));

        PaymentTransaction authorizeTransaction = paymentService.processPayment(customer, payment, purchasedOrder);
        // purchasedOrderService.saveOrUpdate(order);

        // send SNS for further processing
        // update cart

        if (!authorizeTransaction.isFailure()) {
            cart.setPurchasedOrderCode(purchasedOrder.getCode());
            shoppingCartService.saveOrUpdate(cart);
            Set<ShoppingCartItem> saveForLaterItems = cart.getSaveForLaterItems();
            if (!saveForLaterItems.isEmpty()) {
                shoppingCartService.createShoppingCart(customer.getUsername(), saveForLaterItems);
            }
            purchasedOrder.setStatus(PurchasedOrderStatus.PAID);
            purchasedOrderService.saveOrUpdate(purchasedOrder);

            try {
                PublishResponse result = awsSnsService.publish(orderPaymentSuccess,
                        QMessage.builder()
                                .messageGroupId(ProductOrderStatus.PAID.name())
                                .messageId(purchasedOrder.getCode())
                                .body(PurchasedOrderProcessingMessage.builder()
                                        .purchasedOrder(purchasedOrder.getCode())
                                        .event(ProductOrderEvent.PAY)
                                        .build())
                                .build());
            }
            catch (Exception e) {
                // TODO: send a notification or do post processing manually
            }
        }
        else {
            purchasedOrder.setStatus(PurchasedOrderStatus.PAYMENT_FAILED);
            purchasedOrderService.saveOrUpdate(purchasedOrder);

            if (authorizeTransaction.getCause() != null) {
                throw new GenericRuntimeException(PAYMENT_FAILURE, authorizeTransaction.getCause());
            }
        }

        return transactionMapper.convert(authorizeTransaction);
    }

    //https://{ORIGIN}/fail?code={ERROR_CODE}&message={ERROR_MESSAGE}&orderId={ORDER_ID}
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/payments/toss/fail")
    @Operation(hidden = true)
    public void paymentFailure(
            @RequestParam String orderId,
            @RequestParam String code,
            @RequestParam String message)
    {
        PurchasedOrder purchasedOrder = purchasedOrderService.findByCodeOrThrow(orderId);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setFailure(true);
        transaction.setPurchasedOrder(purchasedOrder);
        transaction.setAmount(BigDecimal.ZERO);
        transaction.setPaymentType(purchasedOrder.getPaymentType());
        transaction.setTransactionType(PaymentTransactionType.ERROR);
        transaction.setTransactionDate(DateUtil.now());
        transaction.setDetails(
                codecFactory.jsonCodec(TossPaymentError.class)
                        .toJson(new TossPaymentError(code, message)));

        paymentTransactionService.create(transaction);
        purchasedOrder.setStatus(PurchasedOrderStatus.PAYMENT_FAILED);
        purchasedOrderService.saveOrUpdate(purchasedOrder);
    }
}
