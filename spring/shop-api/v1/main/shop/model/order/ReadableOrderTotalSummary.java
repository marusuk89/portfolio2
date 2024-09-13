package com.itda.shop.model.order;

import com.itda.core.model.order.OrderTotalType;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter @Setter
public class ReadableOrderTotalSummary
{
    private String subTotal; //one time price for items
    private String total; //final price
    private String taxTotal; //total of taxes

    private Map<OrderTotalType, ReadableOrderTotal> totals = new HashMap<>(); //all other fees (tax, shipping ....)
}
