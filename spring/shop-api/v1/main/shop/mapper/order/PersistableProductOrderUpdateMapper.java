package com.itda.shop.mapper.order;

import com.itda.core.model.common.Mapper;
import com.itda.core.model.order.ProductOrder;
import com.itda.shop.model.order.PersistableProductOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PersistableProductOrderUpdateMapper
        implements Mapper<PersistableProductOrder, ProductOrder>
{
    public ProductOrder merge(PersistableProductOrder source, ProductOrder target)
    {
        target.setComments(source.getComments());

        return target;
    }

    @Override
    public ProductOrder createTarget()
    {
        return new ProductOrder();
    }
}
