package com.itda.shop.model.order;

import com.itda.shop.model.entity.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReadableProductOrderAttribute
        extends Entity
{
    private String attributeName;
    private String attributeValue;
}
