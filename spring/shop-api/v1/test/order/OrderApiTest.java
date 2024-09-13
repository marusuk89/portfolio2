package com.itda.shop.order;

import com.itda.core.business.services.order.ProductOrderService;
import com.itda.core.business.services.store.StoreService;
import com.itda.core.model.order.PlaceOrderStatus;
import com.itda.core.model.order.ProductOrderCount;
import com.itda.core.model.order.ProductOrderStatus;
import com.itda.shop.ServiceTestSupport;
import com.itda.shop.application.ShopApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

@SpringBootTest(classes = ShopApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("local")
public class OrderApiTest
        extends ServiceTestSupport
{
    @Autowired
    private StoreService storeService;

    @Autowired
    private ProductOrderService productOrderService;

    @BeforeEach
    public void setUp()
    {
    }

    @Test
    public void getTotalCount()
    {
        dataTestSupport.createProductOrder("aaaa", ProductOrderStatus.PAID, PlaceOrderStatus.NOT_YET);
        productOrderService.updateCountByStore(storeService.findByCodeOrThrow(DEFAULT_STORE));

        ResponseEntity<ProductOrderCount> orderCountResponse = httpAdminAuthGet("cccc", ProductOrderCount.class, "/admin/product-orders/counts");
        assertEquals(OK, orderCountResponse.getStatusCode());
        ProductOrderCount orderCount = orderCountResponse.getBody();
        assertTrue(orderCount.getPaidNormal() > 0);

        orderCountResponse = httpAdminAuthGet("super", ProductOrderCount.class, "/admin/product-orders/counts");
        assertEquals(OK, orderCountResponse.getStatusCode());
        orderCount = orderCountResponse.getBody();
        assertTrue(orderCount.getPaidNormal() > 0);
    }
}
