package com.pobox.common.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    public void setUp() throws Exception {
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

    // use ratio allocate to solve Foemmels Conundrum
    @Test
    public void AllocateLongArray() {
        long[] allocation = {3, 7};
        Money[] result = Money.dollars(0.05).allocate(allocation);
        assertEquals(Money.dollars(0.02), result[0]);
        assertEquals(Money.dollars(0.03), result[1]);
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
    public void DeepCopyShouldReturnNewObject() throws Exception {
        Money aMon = aMoney.deepCopy();
        assertThat(aMon, is(not(sameInstance(aMoney))));
        assertThat(aMon, is(equalTo(aMoney)));
    }

}
