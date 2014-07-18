package com.pobox.common.model;

import java.io.Serializable;

import com.pobox.common.util.SerializabilityTestCase;

public class MoneySerializabilityTest extends SerializabilityTestCase {
    @Override
    protected Serializable createInstance() {
        return Money.dollars(22.34);
    }
}
