package com.pobox.common.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

/**
 * Money class, Represents a monetary value.
 * 
 * For a full description see the book Patterns of Enterprise Application Architecture (Martin Fowler) page 488 or
 * http://www.martinfowler.com/eaaCatalog/money.html
 * 
 * A large proportion of the computers in this world manipulate money, so it's always puzzled me that money isn't
 * actually a first class data type in any mainstream programming language. The lack of a type causes problems, the most
 * obvious surrounding currencies. If all your calculations are done in a single currency, this isn't a huge problem,
 * but once you involve multiple currencies you want to avoid adding your dollars to your yen without taking the
 * currency differences into account. The more subtle problem is with rounding. Monetary calculations are often rounded
 * to the smallest currency unit. When you do this it's easy to lose pennies (or your local equivalent) because of
 * rounding errors.
 * 
 * The good thing about object-oriented programming is that you can fix these problems by creating a Money class that
 * handles them. Of course, it's still surprising that none of the mainstream base class libraries actually do this. -
 * Martin Fowler
 * 
 * ISO4217 codes for currency http://www.xe.com/iso4217.htm
 * 
 * Example Usage:
 * 
 * aMoney = Money.dollars(12.98); bMoney = Money.dollars(-11.98); Money expected = Money.dollars(1.00); Money result =
 * aMoney.add(bMoney); assert expected.equals(result) : "expected a dollar result after operation";
 * 
 * 
 * Notes on Representing money :
 * 
 * use BigDecimal, int, or long (BigDecimal is the recommended default) the int and long forms represent pennies (or the
 * equivalent, of course) BigDecimal is a little more inconvenient to use, but has built-in rounding modes double or
 * float are not recommended, since they always carry small rounding differences the Currency class encapsulates
 * standard identifiers for the world's currencies
 * 
 * Number of digits :
 * 
 * <=9 : use int, long , or BigDecimal <=18 : use long or BigDecimal >18 : use BigDecimal
 * 
 * Reminders for BigDecimal :
 * 
 * the recommended constructor is BigDecimal(String), not BigDecimal(double) - see javadoc BigDecimal objects are
 * immutable - operations always return new objects, and never modify the state of existing objects the ROUND_HALF_EVEN
 * style of rounding introduces the least bias. It is also called bankers' rounding, or round-to-even.
 * 
 * 
 */
@Slf4j
public class Money implements Comparable<Money>, Serializable, Cloneable {
    private static final long serialVersionUID = 42L;
    private static final int[] CENTS = { 1, 10, 100, 1000 };

    /**
     * Amount of money in currency.
     * 
     * 'If you give us $92,233,720,368,547,758.09 we'll write you a version that uses BigInteger"
     * 
     * -Martin Fowler
     */
    private long amount;
    /**
     * Currency amount of money is in.
     */
    private Currency currency;

    /**
     * Convenience constructor - assumes USD.
     * 
     * @param amount
     * @return a USD Money object
     */
    public static Money dollars(final double amount) {
        return new Money(amount, Currency.getInstance("USD"));
    }

    NumberFormat getFormatter() {
        NumberFormat formatter = NumberFormat.getInstance();
        if (formatter instanceof DecimalFormat) {
            DecimalFormat decimalFormatter = (DecimalFormat) formatter;
            decimalFormatter.applyPattern("#,##0.00 \u00A4\u00A4");

            decimalFormatter.setCurrency(currency);
            DecimalFormatSymbols symbols = decimalFormatter.getDecimalFormatSymbols();
            symbols.setInternationalCurrencySymbol(currency.getCurrencyCode());
            symbols.setCurrencySymbol(currency.getSymbol());
            decimalFormatter.setDecimalFormatSymbols(symbols);
        }

        formatter.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        formatter.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        log.trace("Formatter setup: {}", formatter.toString());
        return formatter;
    }

    private Money() {
        // private no arg constructor
    }

    /**
     * Cloning ctor.
     * 
     * @param src
     */
    public Money(final Money src) {
        this.amount = src.amount;
        this.currency = Currency.getInstance(src.currency.getCurrencyCode());
    }

    /**
     * Creates a new money of the provided amount and currency.
     * 
     * User defined rounding mode is used - this method is alluded to in Fowlers book if not actually defined there.
     */
    public Money(final BigDecimal amount, final Currency currency, final int roundingMode) {
        this.currency = currency;
        BigDecimal amt = amount.movePointRight(currency.getDefaultFractionDigits());
        amt = amt.setScale(0, roundingMode);
        this.amount = amt.longValue();
    }

    /**
     * Creates a new money of the provided amount and currency.
     * 
     * Example: new Money( 1.48, Currency.getInstance("USD") )
     * 
     * @param amount
     *            Amount of Money.
     * @param currency
     *            Currency Money is to be measured in - iso4217.
     */
    @JsonCreator
    public Money(@JsonProperty("amount") final double amount, @JsonProperty("currency") final Currency currency) {
        this.currency = currency;
        this.amount = Math.round(amount * centFactor());
    }

    /**
     * Creates a new money of the provided amount and currency.
     * 
     * Example: new Money( 12, Currency.getInstance("CAD") )
     * 
     * @param amount
     *            Amount of Money.
     * @param currency
     *            Currency Money is to be measured in.
     */
    public Money(final long amount, final Currency currency) {
        this.currency = currency;
        this.amount = amount * centFactor();
    }

    /**
     * Simple addition ensuring matched Currency.
     * 
     * @param other
     * @return Money
     */
    public final Money add(final Money other) {
        assertSameCurrencyAs(other);
        return newMoney(amount + other.amount);
    }

    /**
     * Allocate money among multiple targets without losing CENTS.
     * 
     * @param n
     *            (number) of pots to allocate to.
     * @return Money Array of allocated amounts.
     */
    public final Money[] allocate(final int n) {
        Money lowResult = newMoney(amount / n);
        Money highResult = newMoney(lowResult.amount + 1);
        Money[] results = new Money[n];
        int remainder = (int) amount % n;
        for (int i = 0; i < remainder; i++) {
            results[i] = highResult;
        }
        for (int i = remainder; i < n; i++) {
            results[i] = lowResult;
        }
        return results;
    }

    /**
     * Allocate according to proscribed ratios. The base amounts are allocated by simple division, rounding down. So the
     * allocated amount will always be less than or equal to the total.
     * 
     * Remainder contains the unallocated amount. Which will always be a whole number less than 'i'. So simply gives
     * each receiver 1 until the money is gone.
     * 
     * For example, $100 allocated by the "ratios" (1, 1, 1) would yield ($34, $33, $33).
     * 
     * @param ratios
     *            to allocate the remainder to.
     * 
     * @return Money Array of allocated amounts.
     * 
     */
    public final Money[] allocate(final long[] ratios) {
        long total = 0;
        for (int i = 0; i < ratios.length; i++) {
            total += ratios[i];
        }
        long remainder = amount;
        Money[] results = new Money[ratios.length];
        for (int i = 0; i < results.length; i++) {
            results[i] = newMoney(amount * ratios[i] / total);
            remainder -= results[i].amount;
        }
        for (int i = 0; i < remainder; i++) {
            results[i].amount++;
        }
        return results;
    }

    /**
     * Amount accessor.
     */
    public final BigDecimal getAmount() {
        return BigDecimal.valueOf(amount, currency.getDefaultFractionDigits());
    }

    /**
     * throw exception if not valid comparison. Fowler's orgional definition: Assert.equals("money math mismatch",
     * currency, arg.currency );
     */
    private void assertSameCurrencyAs(final Money arg) {
        if (null == arg) {
            throw new IllegalArgumentException("Cannot compare money to null.");
        }
        if (!currency.equals(arg.getCurrency())) {
            throw new IllegalArgumentException("Cannot compare different currencies.");
        }
    }

    private int centFactor() {
        return CENTS[currency.getDefaultFractionDigits()];
    }

    /**
     * Comparison of Money objects used by Comparable interface. This method allows Money to be sorted.
     * 
     * @param other
     * @return Boolean int -1 if less than, 1 if greater than and 0 if equal to other
     * @throws ClassCastException
     *             if other is not a Money
     * @throws IllegalArgumentException
     *             if other Money is not of the same Currency
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public final int compareTo(final Money otherMoney) {
        assertSameCurrencyAs(otherMoney);
        return ComparisonChain.start().compare(this.amount, otherMoney.amount).result();
    }

    /**
     * Currency accessor.
     */
    public final Currency getCurrency() {
        return currency;
    }

    /**
     * Money instances must have same currency and amount to be equal.
     * 
     * @param other
     * @return boolean
     * @see java.lang.Object#equals(Object)
     */
    @Override
    public final boolean equals(final Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof Money)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        Money castOther = (Money) other;
        return Objects.equal(amount, castOther.amount) && Objects.equal(currency, castOther.currency);
    }

    /**
     * Convience implementation of greater than function.
     * 
     * @param other
     * @return Boolean True if money is greater than other
     */
    public final boolean greaterThan(final Money other) {
        return (compareTo(other) > 0);
    }

    /**
     * Hash value based on amount.
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public final int hashCode() {
        return Objects.hashCode(amount, currency);
    }

    public final boolean lessThan(final Money other) {
        return (compareTo(other) < 0);
    }

    /**
     * Money multiplication with default rounding mode.
     * 
     * 
     * Equivalent of multiply( amount, BigDecimal.ROUND_HALF_EVEN )
     * 
     * @param amount
     *            Multiplicator
     * @return Money
     */
    public final Money multiply(final BigDecimal amount) {
        return multiply(amount, BigDecimal.ROUND_HALF_EVEN);
    }

    /**
     * Money multiplication with user specified rounding mode.
     * 
     * @param amount
     *            Multiplicator
     * @param roundingMode
     *            Rounding mode as specified by BigDecimal
     * @return Money
     * @see java.math.BigDecimal#ROUND_UP
     * @see java.math.BigDecimal#ROUND_DOWN
     * @see java.math.BigDecimal#ROUND_CEILING
     * @see java.math.BigDecimal#ROUND_FLOOR
     * @see java.math.BigDecimal#ROUND_HALF_UP
     * @see java.math.BigDecimal#ROUND_HALF_DOWN
     * @see java.math.BigDecimal#ROUND_HALF_EVEN
     * @see java.math.BigDecimal#ROUND_UNNECESSARY
     */
    public Money multiply(final BigDecimal amount, final int roundingMode) {
        return new Money(getAmount().multiply(amount), currency, roundingMode);
    }

    /**
     * Money multiplication with default rounding mode.
     * 
     * Equivalent of multiply( amount, BigDecimal.ROUND_HALF_EVEN )
     * 
     * @param amount
     *            Multiplicator
     * @return Money
     */
    public final Money multiply(final double amount) {
        return multiply(new BigDecimal(amount));
    }

    /**
     * Used to return Money in the same curancy as this one.
     */

    private Money newMoney(final long amount) {
        Money money = new Money();
        money.currency = currency;
        money.amount = amount;
        return money;
    }

    /**
     * Simple addition ensuring matched Currency.
     * 
     * @param other
     * @return Money
     */
    public final Money subtract(final Money other) {
        assertSameCurrencyAs(other);
        return newMoney(amount - other.amount);
    }

    /**
     * Money representation (based on currancy). AMOUNT CODE 1.00 USD One dollar us 1.00 CAD One dollar canadian 1 JPY
     * One yen
     * 
     * You may wish to format the amount according to your locale using the following code fragment: Currency currency =
     * money.currency(); double amount = money.amount().doubleValue();
     * 
     * 
     * NumberFormat nf = NumberFormat.getCurrencyInstance(); nf.setCurrency(currency);
     * nf.setMinimumFractionDigits(currency.getDefaultFractionDigits());
     * nf.setMaximumFractionDigits(currency.getDefaultFractionDigits());
     * 
     * System.out.println(nf.format(amount)); This implementation only really returns the expected result for the local
     * currency. For non local currencies the Currency CODE is used the symbol as per the Currency.getSymbol()
     * specification - and the resulting string cannot be re-parsed back into a amount.
     * 
     * $1.00 or -$1.00 - USD printed in us local CAD1.00 or -CAD1.00 - CAD printed in us local
     * 
     */
    @Override
    public final String toString() {
        return getFormatter().format(getAmount().doubleValue());
    }

    /**
     * Deep copy/clone.
     * 
     * @return a new Money like this one.
     */
    public final Money deepCopy() {
        try {
            return clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("clone not supported", e);
        }
    }

    @Override
    public final Money clone() throws CloneNotSupportedException {
        return new Money(this);
    }
}
