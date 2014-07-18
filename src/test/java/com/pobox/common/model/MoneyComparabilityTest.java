package com.pobox.common.model;

import com.pobox.common.util.ComparabilityTestCase;

public class MoneyComparabilityTest extends ComparabilityTestCase<Money> {

    @Override
    protected Money createEqualInstance() throws Exception {
        return Money.dollars(22.34);
    }

    @Override
    protected Money createGreaterInstance() throws Exception {
        return Money.dollars(7812.99);
    }

    @Override
    protected Money createLessInstance() throws Exception {
        return Money.dollars(12.99);
    }
}
