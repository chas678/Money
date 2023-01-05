package com.pobox.common.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A set of assert methods specially targeted to comparable objects.
 */
public class ComparableAssert {
    /**
     * Don't let anyone have access to this constructor.
     */
    private ComparableAssert() {
    }

    /**
     * Asserts that the <tt>actual</tt> object is lesser than the <tt>limit</tt> object. Throws an
     * <tt>AssertionFailedError</tt> if it is greater or equal.
     */
    public static <T extends Comparable<T>> void assertLesser(String message, T equal1, T actual) {
        assertNotNull(equal1, message);
        assertNotNull(actual, message);
        if (0 >= equal1.compareTo(actual)) {
            failLesser(message, equal1, actual);
        }
    }

    /**
     * Asserts that the <tt>actual</tt> object is lesser than the <tt>limit</tt> object. Throws an
     * <tt>AssertionFailedError</tt> if it is greater or equal.
     */
    public static <T extends Comparable<T>> void assertLesser(T equal1, T actual) {
        assertLesser(null, equal1, actual);
    }

    /**
     * Asserts that the <tt>actual</tt> object is not lesser than the <tt>limit</tt> object. Throws an
     * <tt>AssertionFailedError</tt> if it is lesser.
     */
    public static <T extends Comparable<T>> void assertNotLesser(String message, T limit, T actual) {
        assertNotNull(limit, message);
        assertNotNull(actual, message);
        if (0 < limit.compareTo(actual)) {
            failNotLesser(message, limit, actual);
        }
    }

    /**
     * Asserts that the <tt>actual</tt> object is not lesser than the <tt>limit</tt> object. Throws an
     * <tt>AssertionFailedError</tt> if it is lesser.
     */
    public static <T extends Comparable<T>> void assertNotLesser(T limit, T actual) {
        assertNotLesser(null, limit, actual);
    }

    /**
     * Asserts that the <tt>expected</tt> and <tt>actual</tt> are equals (comparables). Throws an
     * <tt>AssertionFailedError</tt> if it is lesser or equal.
     */
    public static <T extends Comparable<T>> void assertEquals(String message, T equal1, T actual) {
        assertNotNull(equal1, message);
        assertNotNull(actual, message);
        if (0 != equal1.compareTo(actual)) {
            failNotEquals(message, equal1, actual);
        }
    }

    /**
     * Asserts that the <tt>expected</tt> and <tt>actual</tt> are equals (comparables). Throws an
     * <tt>AssertionFailedError</tt> if it is lesser or equal.
     */
    public static <T extends Comparable<T>> void assertEquals(T equal1, T actual) {
        assertEquals(null, equal1, actual);
    }

    /**
     * Asserts that the <tt>expected</tt> and <tt>actual</tt> are not equals (comparables). Throws an
     * <tt>AssertionFailedError</tt> if it is lesser or equal.
     */
    public static <T extends Comparable<T>> void assertNotEquals(String message, T expected, T actual) {
        assertNotNull(expected, message);
        assertNotNull(actual, message);
        if (0 == expected.compareTo(actual)) {
            failEquals(message, expected);
        }
    }

    /**
     * Asserts that the <tt>expected</tt> and <tt>actual</tt> are not equals (comparables). Throws an
     * <tt>AssertionFailedError</tt> if it is lesser or equal.
     */
    public static <T extends Comparable<T>> void assertNotEquals(T expected, T actual) {
        assertNotEquals(null, expected, actual);
    }

    /**
     * Asserts that the <tt>actual</tt> object is greater than the <tt>limit</tt> object. Throws an
     * <tt>AssertionFailedError</tt> if it is lesser or equal.
     */
    public static <T extends Comparable<T>> void assertGreater(String message, T less, T actual) {
        assertNotNull(less, message);
        assertNotNull(actual, message);
        if (0 <= less.compareTo(actual)) {
            failGreater(message, less, actual);
        }
    }

    /**
     * Asserts that the <tt>actual</tt> object is greater than the <tt>limit</tt> object. Throws an
     * <tt>AssertionFailedError</tt> if it is lesser or equal.
     */
    public static <T extends Comparable<T>> void assertGreater(T less, T actual) {
        assertGreater(null, less, actual);
    }

    /**
     * Asserts that the <tt>actual</tt> object is not greater than the <tt>limit</tt> object. Throws an
     * <tt>AssertionFailedError</tt> if it is greater.
     */
    public static <T extends Comparable<T>> void assertNotGreater(String message, T limit, T actual) {
        assertNotNull(limit, message);
        assertNotNull(actual, message);
        if (0 > limit.compareTo(actual)) {
            failNotGreater(message, limit, actual);
        }
    }

    /**
     * Asserts that the <tt>actual</tt> object is not greater than the <tt>limit</tt> object. Throws an
     * <tt>AssertionFailedError</tt> if it is greater.
     */
    public static <T extends Comparable<T>> void assertNotGreater(T limit, T actual) {
        assertNotGreater(null, limit, actual);
    }

    private static void failGreater(String message, Object limit, Object actual) {
        String formatted = "";
        if (null != message) {
            formatted = message + " ";
        }
        fail(formatted + "expected greater than:<" + limit + "> but was:<" + actual + ">");
    }

    private static void failNotGreater(String message, Object limit, Object actual) {
        String formatted = "";
        if (null != message) {
            formatted = message + " ";
        }
        fail(formatted + "expected not greater than:<" + limit + "> but was:<" + actual + ">");
    }

    private static void failLesser(String message, Object limit, Object actual) {
        String formatted = "";
        if (null != message) {
            formatted = message + " ";
        }
        fail(formatted + "expected lesser than:<" + limit + "> but was:<" + actual + ">");
    }

    private static void failNotLesser(String message, Object limit, Object actual) {
        String formatted = "";
        if (null != message) {
            formatted = message + " ";
        }
        fail(formatted + "expected not lesser than:<" + limit + "> but was:<" + actual + ">");
    }

    private static void failNotEquals(String message, Object expected, Object actual) {
        String formatted = "";
        if (null != message) {
            formatted = message + " ";
        }
        fail(formatted + "expected equals to:<" + expected + "> but was:<" + actual + ">");
    }

    private static void failEquals(String message, Object expected) {
        String formatted = "";
        if (null != message) {
            formatted = message + " ";
        }
        fail(formatted + "expected not equals to:<" + expected + ">");
    }
}
