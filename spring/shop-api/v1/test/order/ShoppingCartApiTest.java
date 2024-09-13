package com.itda.shop.order;

import com.itda.core.business.services.customer.CustomerService;
import com.itda.core.business.services.order.ShoppingCartService;
import com.itda.core.business.services.product.ProductVariantService;
import com.itda.core.common.CodeGenerator;
import com.itda.core.common.ErrorCode;
import com.itda.core.model.common.DeliveryMapper;
import com.itda.core.model.order.OrderTotalType;
import com.itda.core.model.order.ShoppingCartItemStatus;
import com.itda.core.model.payment.PaymentTransactionType;
import com.itda.core.model.payment.PaymentType;
import com.itda.core.model.product.ProductVariant;
import com.itda.shop.ServiceTestSupport;
import com.itda.shop.application.ShopApplication;
import com.itda.shop.model.order.OrderConfirmation;
import com.itda.shop.model.order.PersistableOrder;
import com.itda.shop.model.order.PersistableShoppingCartItem;
import com.itda.shop.model.order.ReadableShoppingCart;
import com.itda.shop.model.order.ReadableShoppingCartItem;
import com.itda.shop.model.product.PersistableBoolean;
import com.itda.shop.model.product.PersistableQuantity;
import com.itda.shop.model.product.attribute.ReadableProductVariant;
import com.itda.shop.store.api.exception.ErrorEntity;
import com.itda.shop.store.facade.product.ProductFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@SpringBootTest(classes = ShopApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("local")
public class ShoppingCartApiTest
        extends ServiceTestSupport
{
    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private ShoppingCartService cartService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private DeliveryMapper deliveryMapper;

    @Autowired
    private ProductVariantService productVariantService;

    @BeforeEach
    public void setUp()
    {
        productFacade.evictAllCache();

        ResponseEntity<ReadableShoppingCart> cartResponse = httpAuthGet("bbbb", ReadableShoppingCart.class, "/carts/default");
        assertEquals(OK, cartResponse.getStatusCode());
        deleteCart(cartResponse.getBody().getCode());
    }

    @Test
    public void createNewShoppingCart()
    {
        // create again
        ResponseEntity<ReadableShoppingCart> cartResponse = httpAuthGet("bbbb", ReadableShoppingCart.class, "/carts/default");
        assertEquals(OK, cartResponse.getStatusCode());
    }

    @Test
    public void addItemToShoppingCart()
            throws Exception
    {
        List<String> productCodes = new ArrayList<>();
        BigDecimal totalAmount = addRandomItem("default", 3, productCodes).getSecond();
        String lastProductCode = productCodes.get(productCodes.size() - 1);
        ResponseEntity<ReadableShoppingCart> cartResponse = httpAuthGet("bbbb", ReadableShoppingCart.class, "/carts/default");

        assertEquals(6, cartResponse.getBody().getQuantity());
        assertEquals(totalAmount, cartResponse.getBody().getSubtotal());
        String lastCartItemCode = cartResponse.getBody().getStoreItems().stream()
                .flatMap(storeCart -> storeCart.getItems().stream())
                .filter(item -> item.getProduct().getCode().equals(lastProductCode))
                .findFirst().get()
                .getCode();

        PersistableQuantity quantity = new PersistableQuantity();
        quantity.setQuantity(1); // 3 -> 1
        cartResponse = httpAuthPatch("bbbb", quantity, ReadableShoppingCart.class, "/carts/default", "items", lastCartItemCode, "quantity");

        assertEquals(OK, cartResponse.getStatusCode());
        assertEquals(4, cartResponse.getBody().getQuantity());

        // try adding quantity more than inventory
        quantity.setQuantity(100000000); // 3 -> 1
        ResponseEntity<ErrorEntity> errorResponse = httpAuthPatch("bbbb", quantity, ErrorEntity.class, "/carts/default", "items", lastCartItemCode, "quantity");

        assertEquals(BAD_REQUEST, errorResponse.getStatusCode());
        assertEquals(ErrorCode.OUT_OF_STOCK.getName(), errorResponse.getBody().getName());

        // add same item
        for (ReadableShoppingCartItem existing : cartResponse.getBody().getStoreItems().stream().flatMap(s -> s.getItems().stream()).toList()) {
            if (existing.getCode().equals(lastCartItemCode)) {
                continue;
            }
            PersistableShoppingCartItem item = new PersistableShoppingCartItem();
            item.setProduct(existing.getProduct().getCode());
            item.setVariant(existing.getVariant() != null ? existing.getVariant().getCode() : null);
            item.setQuantity(1);

            cartResponse = httpAuthPost("bbbb", List.of(item), ReadableShoppingCart.class, "/carts", "default", "items");
            assertEquals(CREATED, cartResponse.getStatusCode());
            assertEquals(5, cartResponse.getBody().getQuantity());
            break;
        }

        cartResponse = httpAuthDelete("bbbb", ReadableShoppingCart.class, "/carts/default", "items", lastCartItemCode);
        assertEquals(OK, cartResponse.getStatusCode());
        assertEquals(2, cartResponse.getBody().getStoreItems().stream().flatMap(v -> v.getItems().stream()).count());
        assertEquals(4, cartResponse.getBody().getQuantity());
        assertThat(cartResponse.getBody().getShippingWeight()).isGreaterThan(0d);
    }

    @Test
    public void saveForLaterByCartTest()
            throws Exception
    {
        List<String> productCodes = new ArrayList<>();
        addRandomItem("default", 3, productCodes);

        ResponseEntity<ReadableShoppingCart> cartResponse = httpAuthGet("bbbb", ReadableShoppingCart.class, "/carts/default");
        assertEquals(OK, cartResponse.getStatusCode());

        String cartCode = cartResponse.getBody().getCode();

        PersistableBoolean saveForLater = new PersistableBoolean();
        saveForLater.setValue(true);
        ResponseEntity<ReadableShoppingCart> saveForLaterResponse = httpAuthPatch("bbbb", saveForLater, ReadableShoppingCart.class, "/carts/" + cartCode + "/save-for-later");

        assertEquals(OK, saveForLaterResponse.getStatusCode());

        ReadableShoppingCart updatedCart = saveForLaterResponse.getBody();
        boolean allItemsSavedForLater = updatedCart.getStoreItems().stream()
                .flatMap(storeCart -> storeCart.getItems().stream())
                .allMatch(ReadableShoppingCartItem::isSaveForLater);
        assertTrue(allItemsSavedForLater);
    }

    @Test
    public void deletedItemTest()
            throws Exception
    {
        Optional<ReadableProductVariant> variantOpt = Optional.empty();
        while (variantOpt.isEmpty()) {
            List<String> productCodes = new ArrayList<>();
            ReadableShoppingCart shoppingCart = addRandomItem("default", 3, productCodes).getFirst();
            variantOpt = shoppingCart.getStoreItems().stream().flatMap(s -> s.getItems().stream())
                    .map(ReadableShoppingCartItem::getVariant)
                    .filter(Objects::nonNull)
                    .findFirst();
        }
        ReadableProductVariant variant = variantOpt.get();
        ProductVariant variantInst = productVariantService.findByCodeOrThrow(variant.getCode());
        variantInst.setDeleted(true);
        productVariantService.saveOrUpdate(variantInst);

        ResponseEntity<ReadableShoppingCart> cartResponse = httpAuthGet("bbbb", ReadableShoppingCart.class, "/carts/default");
        assertEquals(OK, cartResponse.getStatusCode());

        ReadableShoppingCart cart = cartResponse.getBody();

        ReadableShoppingCartItem deletedItem = cart.getStoreItems().stream()
                .flatMap(storeCart -> storeCart.getItems().stream())
                .filter(item -> item.getVariant() != null && item.getVariant().getCode().equals(variant.getCode()))
                .findFirst()
                .get();
        assertEquals(ShoppingCartItemStatus.UNAVAILABLE, deletedItem.getStatus());

        // rollback for repeated tests
        variantInst = productVariantService.findByCodeOrThrow(variant.getCode());
        variantInst.setAvailable(true);
        variantInst.setDeleted(false);
        productVariantService.saveOrUpdate(variantInst);
    }

    @Test
    @Disabled("Manual Test")
    public void checkoutTest()
            throws Exception
    {
        List<String> productCodes = new ArrayList<>();
        Pair<ReadableShoppingCart, BigDecimal> pair = addRandomItem("default", 3, productCodes);
        ReadableShoppingCart shoppingCart = pair.getFirst();
        BigDecimal productFee = pair.getSecond();

        PersistableOrder order = new PersistableOrder();
        order.setPaymentType(PaymentType.CREDITCARD);
        order.setDelivery(getDelivery("bbbb"));

        ReadableShoppingCartItem saveForLaterItem = shoppingCart.getStoreItems().get(0).getItems().get(0);
        // save for later
        PersistableBoolean saveForLater = new PersistableBoolean();
        saveForLater.setValue(true);

        ResponseEntity<ReadableShoppingCart> saveForLaterResponse = httpAuthPatch("bbbb", saveForLater, ReadableShoppingCart.class, "/carts/default/items", saveForLaterItem.getCode(), "save-for-later");
        assertEquals(OK, saveForLaterResponse.getStatusCode());
        productFee = productFee.subtract(saveForLaterItem.getSubTotal());

        ResponseEntity<OrderConfirmation> orderConfirmResponse = httpAuthPost("bbbb", order, OrderConfirmation.class, "/carts/default/checkout");
        assertEquals(OK, orderConfirmResponse.getStatusCode());
        OrderConfirmation orderConfirmation = orderConfirmResponse.getBody();

        BigDecimal shippingFee = orderConfirmation.getTotal().getTotals().values().stream()
                .filter(o -> o.getType() == OrderTotalType.SHIPPING)
                .map(o -> o.getAmount())
                .findFirst()
                .get();

        BigDecimal totalFee = productFee.add(shippingFee);
        assertTrue(shippingFee.compareTo(BigDecimal.ZERO) > 0, "expect positive shipping fee");
        assertEquals(totalFee, orderConfirmation.getTotal().getGrandTotal());
        assertEquals(totalFee, orderConfirmation.getPayment().getAmount());
        assertEquals(PaymentTransactionType.INIT, orderConfirmation.getPayment().getTransactionType());

        Map<String, Object> paymentDetails = orderConfirmation.getPayment().getDetails();
        openTossPayment(orderConfirmation.getCode(), totalFee.longValue(), paymentDetails);
        // call success url
        Thread.sleep(120_000);

        ResponseEntity<ReadableShoppingCart> cartResponse = httpAuthGet("bbbb", ReadableShoppingCart.class, "/carts/default");
        assertEquals(OK, cartResponse.getStatusCode());

        List<ReadableShoppingCartItem> leftItem = cartResponse.getBody().getStoreItems().stream()
                .flatMap(c -> c.getItems().stream())
                .toList();
        assertEquals(1, leftItem.size());
        assertEquals(saveForLaterItem.getCode(), leftItem.get(0).getCode());
    }

    @Test
    public void instantAddItemToShoppingCart()
            throws Exception
    {
        List<String> productCodes = new ArrayList<>();
        Pair<ReadableShoppingCart, BigDecimal> pair = addRandomItem("instant", 1, productCodes);
        ReadableShoppingCart shoppingCart = pair.getFirst();
        String actualCartCode = shoppingCart.getCode();
        BigDecimal totalAmount = pair.getSecond();

        ResponseEntity<ReadableShoppingCart> cartResponse = httpAuthGet("bbbb", ReadableShoppingCart.class, "/carts", actualCartCode);

        assertEquals(1, cartResponse.getBody().getQuantity());
        assertEquals(totalAmount, cartResponse.getBody().getSubtotal());

        PersistableOrder order = new PersistableOrder();
        order.setPaymentType(PaymentType.CREDITCARD);
        order.setDelivery(getDelivery("bbbb"));

        ResponseEntity<OrderConfirmation> orderConfirmResponse = httpAuthPost("bbbb", order, OrderConfirmation.class, "/carts", actualCartCode, "checkout");
        assertEquals(OK, orderConfirmResponse.getStatusCode());
        OrderConfirmation orderConfirmation = orderConfirmResponse.getBody();

        assertTrue(orderConfirmation.getTotal().getGrandTotal().subtract(totalAmount).compareTo(BigDecimal.ZERO) > 0);
        assertTrue(orderConfirmation.getPayment().getAmount().subtract(totalAmount).compareTo(BigDecimal.ZERO) > 0);
    }

    //@Test
    public void testTossPayment()
            throws Exception
    {
        int port = URI.create(testRestTemplate.getRootUri()).getPort();
        openTossPayment(CodeGenerator.create(), 15000, Map.of(
                "customerKey", CodeGenerator.create(),
                "successUrl", format("http://localhost:%d/api/v1/payments/toss/success", port),
                "failUrl", format("http://localhost:%d/api/v1/payments/toss/fail", port)));

        Thread.sleep(600_000);
    }

    private void openTossPayment(String orderId, long amount, Map<String, Object> paymentDetails)
            throws Exception
    {
        int port = URI.create(testRestTemplate.getRootUri()).getPort();
        String customerKey = paymentDetails.get("customerKey").toString();
        String successUrl = paymentDetails.get("successUrl").toString();
        String failUrl = paymentDetails.get("failUrl").toString();
        successUrl = successUrl.replace("localhost:8080", "localhost:" + port);
        failUrl = failUrl.replace("localhost:8080", "localhost:" + port);

        File file = File.createTempFile("toss", ".html");
        try (FileWriter out = new FileWriter(file)) {
            out.write(format("<html>\n" +
                    "<head>\n" +
                    "  <title></title>\n" +
                    "  <script src=\"https://js.tosspayments.com/v1/payment-widget\"></script>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<div id='payment-method'></div>\n" +
                    "<script>\n" +
                    "    const clientKey = 'test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq';\n" +
                    "    const customerKey = '%s';\n" +
                    "    //const paymentWidget = PaymentWidget(clientKey, customerKey); // 회원 결제\n" +
                    "    const paymentWidget = PaymentWidget(clientKey, PaymentWidget.ANONYMOUS); // 비회원 결제\n" +
                    "    function doPayment() {\n" +
                    "      const result = paymentWidget.requestPayment({\n" +
                    "        orderId: '%s',\n" +
                    "        orderName: '토스 티셔츠 외 2건',\n" +
                    "        successUrl: '%s',\n" +
                    "        failUrl: '%s',\n" +
                    "        customerEmail: 'customer123@gmail.com',\n" +
                    "        customerName: '김토스'\n" +
                    "      });\n" +
                    "      console.log('end payment');\n" +
                    "      console.log(result);\n" +
                    "   }\n" +
                    "   paymentWidget.renderPaymentMethods('#payment-method', %d); \n" +
                    "</script>\n" +
                    "<body>\n" +
                    "<input type='button' name='payment' value='결제' onClick='doPayment();'></input>\n" +
                    "</body>\n" +
                    "</html>\n" +
                    "", customerKey, orderId, successUrl, failUrl, amount));
        }

        List<String> cmds = new ArrayList<>();
        cmds.add("open");
        cmds.add(file.toURI().toString());
        ProcessBuilder build = new ProcessBuilder(cmds);
        build.start();
    }

    private void deleteCart(String cartCode)
    {
        cartService.deleteCart(cartService.findByCode(cartCode).get());
    }
}
