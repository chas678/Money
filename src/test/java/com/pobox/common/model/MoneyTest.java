package com.pobox.common.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoneyTest {
    private Money aMoney;
    private Money bMoney;

    public MoneyTest() {
        // Setting default locale so test runs on other localized machines.
        Locale.setDefault(Locale.US);
        //        Locale.setDefault(new Locale("sv","SE"));
    }

    @BeforeEach
    public void setUp() {
        aMoney = Money.dollars(23.45);
        bMoney = Money.dollars(12133.456);
    }

    @Test
    public void Add() {
        Money expected = Money.dollars(12156.91);
        Money result = aMoney.add(bMoney);
        assertEquals(expected, result);
    }

    @Test
    public void AddDiffCurrencies() {
        bMoney = new Money(1.11, Currency.getInstance("NZD"));
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> aMoney.add(bMoney));
        assertEquals("Cannot compare different currencies.", exception.getMessage());
    }

    @Test
    public void AddNegative() {
        aMoney = Money.dollars(12.98);
        bMoney = Money.dollars(-11.98);
        Money expected = Money.dollars(1.00);
        Money result = aMoney.add(bMoney);
        assertEquals(expected, result);
    }

    @Test
    public void AddNull() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> aMoney.add(null));
        assertEquals("Cannot compare money to null.", exception.getMessage());
    }

    @Test
    public void AddZero() {
        aMoney = Money.dollars(11.98);
        bMoney = Money.dollars(00.00);
        Money expected = Money.dollars(11.98);
        Money result = aMoney.add(bMoney);
        assertEquals(expected, result);
    }

    @Test
    public void AllocateInt() {
        // this is the 'unfair' allocate - the first get the remainder
        aMoney = Money.dollars(0.05);
        Money[] monies = aMoney.allocate(2);
        assertEquals(Money.dollars(0.03), monies[0]);
        assertEquals(Money.dollars(0.02), monies[1]);
    }

    @Test
    public void AllocateIntMoreRemainder() {
        // this is the 'unfair' allocate - the first get the remainder
        aMoney = Money.dollars(0.08);
        Money[] monies = aMoney.allocate(3);
        assertEquals(Money.dollars(0.03), monies[0]);
        assertEquals(Money.dollars(0.03), monies[1]);
        assertEquals(Money.dollars(0.02), monies[2]);
    }

    @Test
    public void AllocateIntNoRemainder() {
        // this is the 'unfair' allocate - the first get the remainder
        aMoney = Money.dollars(0.09);
        Money[] monies = aMoney.allocate(3);
        assertEquals(Money.dollars(0.03), monies[0]);
        assertEquals(Money.dollars(0.03), monies[1]);
        assertEquals(Money.dollars(0.03), monies[2]);
    }

    @Test
    public void AllocateIntDoesNotTruncateAmountToInt() {
        // 8_589_934_599 minor units = 2^33 + 7, well past Integer.MAX_VALUE.
        // Construct via the BigDecimal ctor so the value lands exactly in the long
        // amount field — bypassing the (long, Currency) ctor's centFactor multiplier.
        long minorUnits = (1L << 33) + 7L; // 8_589_934_599
        Money huge = new Money(BigDecimal.valueOf(minorUnits, 2),
                Currency.getInstance("USD"),
                RoundingMode.UNNECESSARY);
        Money[] split = huge.allocate(10);

        long sum = Arrays.stream(split)
                .mapToLong(m -> m.getAmount().movePointRight(2).longValueExact())
                .sum();
        assertEquals(minorUnits, sum, "allocate must conserve total amount");

        long high = minorUnits / 10 + 1;
        long low = minorUnits / 10;
        long expectedHighCount = minorUnits % 10; // 9
        long highCount = Arrays.stream(split)
                .filter(m -> m.getAmount().movePointRight(2).longValueExact() == high)
                .count();
        assertEquals(expectedHighCount, highCount);
        long lowCount = Arrays.stream(split)
                .filter(m -> m.getAmount().movePointRight(2).longValueExact() == low)
                .count();
        assertEquals(10 - expectedHighCount, lowCount);
    }

    @Test
    public void AllocateIntNegativeAmountConservesTotal() {
        Money debit = Money.dollars(-0.05);
        Money[] split = debit.allocate(2);
        Money sum = Arrays.stream(split).reduce(Money.dollars(0), Money::add);
        assertEquals(Money.dollars(-0.05), sum);
    }

    @Test
    public void AllocateIntNegativeAmountDistributesRemainderConsistently() {
        // -$0.08 / 3: low=-3, high=-2; "first slots get the larger share" semantics
        // mean the first 2 slots are -0.03 (more-negative), the last is -0.02.
        Money debit = Money.dollars(-0.08);
        Money[] split = debit.allocate(3);
        assertEquals(Money.dollars(-0.03), split[0]);
        assertEquals(Money.dollars(-0.03), split[1]);
        assertEquals(Money.dollars(-0.02), split[2]);
    }

    // use ratio allocate to solve Foemmels Conundrum
    @Test
    public void AllocateLongArray() {
        long[] allocation = {3, 7};
        Money[] result = Money.dollars(0.05).allocate(allocation);
        assertEquals(Money.dollars(0.02), result[0]);
        assertEquals(Money.dollars(0.03), result[1]);
    }

    @Test
    public void AllocateRatiosOverflowFailsLoud() {
        // amount * ratios[0] overflows long — must throw, not wrap silently.
        Money big = new Money(BigDecimal.valueOf(Long.MAX_VALUE / 1_000L, 2),
                Currency.getInstance("USD"),
                RoundingMode.UNNECESSARY);
        long[] ratios = {1_000_000L, 1L};
        assertThrows(ArithmeticException.class, () -> big.allocate(ratios));
    }

    @Test
    public void AllocateRatiosSumOverflowFailsLoud() {
        // Sum of ratios overflows long — must throw, not wrap silently.
        long[] ratios = {Long.MAX_VALUE, 1L};
        assertThrows(ArithmeticException.class, () -> Money.dollars(1.00).allocate(ratios));
    }

    @Test
    public void AllocateRatiosRejectsNegativeRatio() {
        long[] ratios = {1L, -1L, 1L};
        assertThrows(IllegalArgumentException.class, () -> Money.dollars(1.00).allocate(ratios));
    }

    @Test
    public void AllocateRatiosReturnsIndependentInstances() {
        Money source = Money.dollars(1.00);
        Money[] a = source.allocate(new long[]{1L, 1L, 1L});
        Money[] b = source.allocate(new long[]{1L, 1L, 1L});
        assertEquals(a[0], b[0]);
        assertNotSame(a[0], b[0]);
        // No two slots in one allocation may alias the same Money instance — protects
        // the value-type contract that older `results[i].amount++` code violated.
        for (int i = 0; i < a.length; i++) {
            for (int j = i + 1; j < a.length; j++) {
                assertNotSame(a[i], a[j], "result slots must be independent instances");
            }
        }
    }

    @Test
    public void AllocateRatiosWithZeroRatioGivesZeroSlot() {
        Money[] result = Money.dollars(1.00).allocate(new long[]{0L, 1L, 1L});
        assertEquals(Money.dollars(0), result[0]);
    }

    @Test
    public void AllocateRatiosSingleRatioGivesFullAmount() {
        Money[] result = Money.dollars(1.00).allocate(new long[]{1L});
        assertEquals(1, result.length);
        assertEquals(Money.dollars(1.00), result[0]);
    }

    @Test
    public void AllocateIntSingleSlotGivesFullAmount() {
        Money[] result = Money.dollars(1.00).allocate(1);
        assertEquals(1, result.length);
        assertEquals(Money.dollars(1.00), result[0]);
    }

    @Test
    public void AllocationResultIsImmutable() {
        Money source = Money.dollars(1.00);
        Money.AllocationResult result = source.allocateToResult(3);
        List<Money> list = result.allocations();
        assertThrows(UnsupportedOperationException.class, () -> list.set(0, null));
        assertThrows(UnsupportedOperationException.class, () -> list.add(Money.dollars(0)));
    }

    @Test
    public void AllocationResultRoundtripsThroughGet() {
        Money source = Money.dollars(1.00);
        Money.AllocationResult result = source.allocateToResult(3);
        assertEquals(3, result.size());
        assertEquals(Money.dollars(0.34), result.get(0));
        assertEquals(Money.dollars(0.33), result.get(1));
        assertEquals(Money.dollars(0.33), result.get(2));
    }

    @Test
    public void Amount() {
        BigDecimal expectedAmount = BigDecimal.valueOf(2345L, 2);
        assertEquals(expectedAmount, aMoney.getAmount());
    }

    @Test
    public void AmountNegative() {
        aMoney = Money.dollars(-12123.87612);
        BigDecimal expectedAmount = BigDecimal.valueOf(-1212388, 2);
        assertEquals(expectedAmount, aMoney.getAmount());
    }

    @Test
    public void CurrencyAUD() {
        aMoney = new Money(213L, Currency.getInstance("CAD"));
        assertEquals("CAD", aMoney.getCurrency().getCurrencyCode());
        assertEquals(2, aMoney.getCurrency().getDefaultFractionDigits());

        // if in UK Locale - shouold be a ISO AUD symbol
        assertEquals("CA$", aMoney.getCurrency().getSymbol(Locale.UK));// ALT
        // 0163
        // but if in Australia then should be a $ symbol
        assertEquals("$", aMoney.getCurrency().getSymbol(Locale.CANADA)); //
    }

    @Test
    public void CurrencyCNY() { // Chinese RMB
        aMoney = new Money(213L, Currency.getInstance("CNY"));
        assertEquals("CNY", aMoney.getCurrency().getCurrencyCode());
        assertEquals(2, aMoney.getCurrency().getDefaultFractionDigits());

        // ISO4217 CNY symbol
        assertEquals("CN¥", aMoney.getCurrency().getSymbol());
        // check locale -  not ¥
    }

    @Test
    public void CurrencyGBP() {
        aMoney = new Money(213L, Currency.getInstance("GBP"));
        assertEquals("GBP", aMoney.getCurrency().getCurrencyCode());
        assertEquals(2, aMoney.getCurrency().getDefaultFractionDigits());

        // if in UK Locale - should be a £ symbol
        assertEquals("£", aMoney.getCurrency().getSymbol(Locale.UK)); // ALT
        // 0163
        // but if not - like in Aus - should be ISO GBP symbol
        assertEquals("£", aMoney.getCurrency().getSymbol(Locale.GERMANY)); // Aus locale
    }

    @Test
    public void Dollars() {
        // convenience factory should create same thing as constructor
        aMoney = Money.dollars(2323.21);
        bMoney = new Money(2323.21, Currency.getInstance("USD"));

        assertEquals(aMoney, bMoney);
    }

    @Test
    public void DollarsUSNotAUS() {
        aMoney = Money.dollars(2323.21);
        bMoney = new Money(2323.21, Currency.getInstance("AUD"));

        assertNotEquals(aMoney, bMoney);
    }

    @Test
    public void GreaterThan() {
        assertFalse(aMoney.greaterThan(bMoney));
        assertTrue(bMoney.greaterThan(aMoney));
    }

    @Test
    public void GreaterThanDiffCurrencies() {
        bMoney = new Money(2323.21, Currency.getInstance("AUD"));
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> aMoney.greaterThan(bMoney));
        assertEquals("Cannot compare different currencies.", exception.getMessage());
    }

    @Test
    public void GreaterThanNull() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> aMoney.greaterThan(null));
        assertEquals("Cannot compare money to null.", exception.getMessage());
    }

    @Test
    public void LessThan() {
        assertTrue(aMoney.lessThan(bMoney));
        assertFalse(bMoney.lessThan(aMoney));
    }

    @Test
    public void LessThanDiffCurrencies() {
        bMoney = new Money(2323.21, Currency.getInstance("AUD"));
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> aMoney.lessThan(bMoney));
        assertEquals("Cannot compare different currencies.", exception.getMessage());
    }

    @Test
    public void LessThanNull() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> aMoney.lessThan(null));
        assertEquals("Cannot compare money to null.", exception.getMessage());
    }

    @Test
    public void MoneyBigDecimalCurrencyInt() {
        BigDecimal bdInput = BigDecimal.valueOf(220300, 2);
        Money expected = new Money(2203, Currency.getInstance("INR"));
        Money result = new Money(bdInput, Currency.getInstance("INR"), RoundingMode.CEILING);
        assertEquals(expected, result);
    }

    @Test
    public void MoneyDoubleCurrency() {
        double inputAmount = 23242.22;
        Currency currency = Currency.getInstance("GBP");
        Money money = new Money(inputAmount, currency);
        assertEquals("GBP", money.getCurrency().getCurrencyCode());
        BigDecimal expectedAmount = BigDecimal.valueOf(2324222L, 2);
        assertEquals(0, expectedAmount.compareTo(money.getAmount()));
    }

    @Test
    public void MoneyLongCurrency() {
        long inputAmount = 2324222L;
        Currency currency = Currency.getInstance("GBP");
        Money money = new Money(inputAmount, currency);
        assertEquals("GBP", money.getCurrency().getCurrencyCode());
        BigDecimal expectedAmount = BigDecimal.valueOf(2324222L, 0);
        assertEquals(0, expectedAmount.compareTo(money.getAmount()));
    }

    @Test
    public void MultiplyBigDecimal() {
        aMoney = Money.dollars(10.01);
        BigDecimal multiplier = BigDecimal.valueOf(3, 0);
        Money expected = Money.dollars(30.03);
        Money result = aMoney.multiply(multiplier);
        assertEquals(expected, result);
    }

    @Test
    public void MultiplyBigDecimalInt() {
        aMoney = Money.dollars(10.01);
        BigDecimal multiplier = BigDecimal.valueOf(22, 0);
        Money expected = Money.dollars(220.22);
        Money result = aMoney.multiply(multiplier, RoundingMode.CEILING);
        assertEquals(expected, result);
    }

    @Test
    public void MultiplyDouble() {
        aMoney = Money.dollars(10.01);
        double multiplier = 2.2;
        Money expected = Money.dollars(22.02);
        Money result = aMoney.multiply(multiplier);
        assertEquals(expected, result);
    }

    @Test
    public void Subtract() {
        Money expected = Money.dollars(-12110.01);
        Money result = aMoney.subtract(bMoney);
        assertEquals(expected, result);
    }

    @Test
    public void SubtractDiffCurrencies() {
        bMoney = new Money(1.11, Currency.getInstance("NZD"));
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> aMoney.subtract(bMoney));
        assertEquals("Cannot compare different currencies.", exception.getMessage());
    }

    @Test
    public void SubtractNegative() {
        aMoney = Money.dollars(12.98);
        bMoney = Money.dollars(-11.98);
        Money expected = Money.dollars(24.96);
        Money result = aMoney.subtract(bMoney);
        assertEquals(expected, result);
    }

    @Test
    public void subtractNull() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> aMoney.subtract(null));
        assertEquals("Cannot compare money to null.", exception.getMessage());
    }

    @Test
    public void SubtractZero() {
        aMoney = Money.dollars(11.98);
        bMoney = Money.dollars(0);
        Money expected = Money.dollars(11.98);
        Money result = aMoney.subtract(bMoney);
        assertEquals(expected, result);
    }

    @Test
    public void ToString() {
        aMoney = Money.dollars(214.33333333);
        String expected = "214.33 USD";
        String actual = aMoney.toString();
        assertEquals(expected, actual);
    }

    @Test
    public void ToStringEUR() {
        aMoney = new Money(123.45, Currency.getInstance(Locale.GERMANY));
        String expected = "123.45 EUR";
        String actual = aMoney.toString();
        assertEquals(expected, actual);
    }

    @Test
    public void ToStringFranceEUR() {
        aMoney = new Money(123.45, Currency.getInstance(Locale.FRANCE));
        String expected = "123.45 EUR";
        String actual = aMoney.toString();
        assertEquals(expected, actual);
    }

    @Test
    public void testThreadSafety() {
        aMoney = new Money(123.45, Currency.getInstance(Locale.FRANCE));
        bMoney = Money.dollars(11.98);
        NumberFormat nf2 = bMoney.getFormatter();
        NumberFormat nf1 = aMoney.getFormatter();
        assertThat(nf1.format(aMoney.getAmount().doubleValue()), is("123.45 EUR"));
        assertThat(nf2.format(bMoney.getAmount().doubleValue()), is("11.98 USD"));
    }

    @Test
    public void DeepCopyShouldReturnNewObject() {
        Money aMon = aMoney.deepCopy();
        assertThat(aMon, is(not(sameInstance(aMoney))));
        assertThat(aMon, is(equalTo(aMoney)));
    }

}
