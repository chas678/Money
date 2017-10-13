package com.pobox.common.model;

import com.pobox.common.util.SerializabilityTestCase;

import java.io.Serializable;

public class MoneySerializabilityTest extends SerializabilityTestCase {
    @Override
    protected Serializable createInstance() {
        return Money.dollars(22.34);
    }
}
