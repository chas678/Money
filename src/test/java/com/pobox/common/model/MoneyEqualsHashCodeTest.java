package com.pobox.common.model;

import com.pobox.common.util.EqualsHashCodeTestCase;

public class MoneyEqualsHashCodeTest extends EqualsHashCodeTestCase {
    @Override
    protected Object createInstance() {
        return Money.dollars(22.34);
    }

    @Override
    protected Object createNotEqualInstance() {
        return Money.dollars(7812.99);
    }
}
