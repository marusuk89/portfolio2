package com.itda.shop.order;

import com.itda.core.business.services.customer.CustomerService;
import com.itda.core.business.services.order.ProductOrderService;
import com.itda.core.business.services.store.StoreService;
import com.itda.core.common.CodeGenerator;
import com.itda.core.init.InitializationDatabase;
import com.itda.core.model.common.EntityList;
import com.itda.core.model.customer.Customer;
import com.itda.core.model.order.PlaceOrderStatus;
import com.itda.core.model.order.ProductOrder;
import com.itda.core.model.order.ProductOrderCriteria;
import com.itda.core.model.order.ProductOrderStatus;
import com.itda.core.model.store.Store;
import com.itda.core.utils.DataTestSupport;
import com.itda.shop.application.config.ShopApplicationConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ShopApplicationConfiguration.class}, initializers = {ConfigDataApplicationContextInitializer.class})
@ActiveProfiles("local")
public class ProductOrderSearchTest
{
    @Autowired
    private InitializationDatabase initializationDatabase;

    @Autowired
    private ProductOrderService productOrderService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private DataTestSupport dataTestSupport;

    private Customer customer;
    private Store store;

    @BeforeEach
    public void setup()
            throws Exception
    {
        dataTestSupport.initialize(() -> initializationDatabase.init());
        if (customer == null) {
            customer = customerService.getByUsername("aaaa");
            dataTestSupport.createProductOrder("aaaa", ProductOrderStatus.PAID, PlaceOrderStatus.NOT_YET);
        }
        if (store == null) {
            store = storeService.findByCodeOrThrow(DataTestSupport.DEFAULT_STORE);
        }
    }

    @Test
    public void testSearchAll()
            throws Exception
    {
        ProductOrderCriteria criteria = new ProductOrderCriteria();
        EntityList<ProductOrder> productOrders = productOrderService.searchByCriteria(store, criteria);
        assertTrue(productOrders.getSize() > 0);
    }

    @Test
    public void testSearchLike()
            throws Exception
    {
        ProductOrderCriteria criteria = new ProductOrderCriteria();
        criteria.setOrder(CodeGenerator.createNextId().substring(0, 4)); // 202312
        EntityList<ProductOrder> productOrders = productOrderService.searchByCriteria(store, criteria);
        assertTrue(productOrders.getSize() > 0);
    }

    @Test
    public void testSearchAny()
            throws Exception
    {
        ProductOrderCriteria criteria = new ProductOrderCriteria();
        criteria.setAny("Full Name");
        EntityList<ProductOrder> productOrders = productOrderService.searchByCriteria(store, criteria);
        assertTrue(productOrders.getSize() > 0);

        criteria = new ProductOrderCriteria();
        criteria.setAny("Name"); // like
        productOrders = productOrderService.searchByCriteria(customer, criteria);
        assertTrue(productOrders.getSize() > 0);
    }

    @Test
    public void testSearchDates()
            throws Exception
    {
        ProductOrderCriteria criteria = new ProductOrderCriteria();
        criteria.setStartOrderedDateTime(Instant.ofEpochMilli(0).atOffset(ZoneOffset.UTC));
        criteria.setEndOrderedDateTime(Instant.now().plusSeconds(3600).atOffset(ZoneOffset.UTC));
        EntityList<ProductOrder> productOrders = productOrderService.searchByCriteria(store, criteria);
        assertTrue(productOrders.getSize() > 0);
        for (ProductOrder po : productOrders.getRecords()) {
            assertTrue(po.getAuditSection().getCreatedTimestamp().compareTo(Instant.ofEpochMilli(0)) > 0);
            assertTrue(po.getAuditSection().getCreatedTimestamp().compareTo(Instant.now().plusSeconds(3600)) < 0);
        }
    }
}
