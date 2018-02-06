package com.pobox.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Extend me in order to test a class's functional compliance with the <code>equals</code> and <code>hashCode</code>
 * contract.
 * <p>
 * Override my {@link #createInstance() createInstance} and {@link #createNotEqualInstance() createNotEqualInstance}
 * methods to provide me with objects to test against. Both methods should return objects that are of the same class.
 * <p>
 * <b>WARNING</b>: Extend me only if your class overrides <code>equals</code> to test for equivalence. If your class's
 * <code>equals</code> tests for identity or preserves the behavior from <code>Object</code>, I'm not interested,
 * because I expect <code>createInstance</code> to return equivalent but distinct objects.
 *
 * @see Object#equals(Object)
 * @see Object#hashCode()
 */
public abstract class EqualsHashCodeTestCase {
    private static final int NUM_ITERATIONS = 20;
    private Object eq1;
    private Object eq2;
    private Object eq3;
    private Object neq;

    /**
     * Creates and returns an instance of the class under test.
     *
     * @return a new instance of the class under test; each object returned from this method should compare equal to
     * each other.
     * @throws Exception
     */
    protected abstract Object createInstance();

    /**
     * Creates and returns an instance of the class under test.
     *
     * @return a new instance of the class under test; each object returned from this method should compare equal to
     * each other, but not to the objects returned from {@link #createInstance() createInstance}.
     * @throws Exception
     */
    protected abstract Object createNotEqualInstance();

    /**
     * Sets up the test fixture.
     *
     * @throws Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        eq1 = createInstance();
        eq2 = createInstance();
        eq3 = createInstance();
        neq = createNotEqualInstance();
        // We want these assertions to yield errors, not failures.
        try {
            assertNotNull(eq1, "createInstance() returned null");
            assertNotNull(eq2, "2nd createInstance() returned null");
            assertNotNull(eq3, "3rd createInstance() returned null");
            assertNotNull(neq, "createNotEqualInstance() returned null");
            assertNotSame(eq1, eq2);
            assertNotSame(eq1, eq3);
            assertNotSame(eq1, neq);
            assertNotSame(eq2, eq3);
            assertNotSame(eq2, neq);
            assertNotSame(eq3, neq);
            assertSame(eq1.getClass(), eq2.getClass(), "1st and 2nd equal instances of different classes");
            assertSame(eq1.getClass(), eq3.getClass(), "1st and 3rd equal instances of different classes");
            assertSame(eq1.getClass(), neq.getClass(), "1st equal instance and not-equal instance of different " +
                    "classes");
        } catch (AssertionFailedError ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    /**
     * Tests whether <code>equals</code> holds up against a new <code>Object</code> (should always be <code>false</code>
     * ).
     */
    @Test
    public final void testEqualsAgainstNewObject() {
        Object o = new Object();
        assertNotEquals(eq1, o);
        assertNotEquals(eq2, o);
        assertNotEquals(eq3, o);
        assertNotEquals(neq, o);
    }

    /**
     * Asserts that two objects are not equal. Throws an <tt>AssertionFailedError</tt> if they are equal.
     */
    public void assertNotEquals(Object expected, Object actual) {
        if ((expected == null && actual == null) || (expected != null && expected.equals(actual))) {
            fail("expected not equals to: <" + expected + ">");
        }
    }

    /**
     * Tests whether <code>equals</code> holds up against <code>null</code>.
     */
    @Test
    public final void testEqualsAgainstNull() {
        assertThat("null vs. 1st", eq1, not(equalTo(null)));
        assertThat("null vs. 2nd", eq2, not(equalTo(null)));
        assertThat("null vs. 3rd", eq3, not(equalTo(null)));
        assertThat("null vs. not-equal", neq, not(equalTo(null)));
    }

    /**
     * Tests whether <code>equals</code> holds up against objects that should not compare equal.
     */
    @Test
    public final void testEqualsAgainstUnequalObjects() {
        assertThat("1st vs. not-equal", eq1, not(equalTo(neq)));
        assertThat("2nd vs. not-equal", eq2, not(equalTo(neq)));
        assertThat("3rd vs. not-equal", eq3, not(equalTo(neq)));
        assertThat("not-equal vs. 1st", neq, not(equalTo(eq1)));
        assertThat("not-equal vs. 2nd", neq, not(equalTo(eq2)));
        assertThat("not-equal vs. 3rd", neq, not(equalTo(eq3)));
    }

    /**
     * Tests whether <code>equals</code> is <em>consistent</em>.
     */
    @RepeatedTest(value = NUM_ITERATIONS, name = RepeatedTest.SHORT_DISPLAY_NAME)
    public final void testEqualsIsConsistentAcrossInvocations() {
        testEqualsAgainstNewObject();
        testEqualsAgainstNull();
        testEqualsAgainstUnequalObjects();
        testEqualsIsReflexive();
        testEqualsIsSymmetricAndTransitive();
    }

    /**
     * Tests whether <code>equals</code> is <em>reflexive</em>.
     */
    @Test
    public final void testEqualsIsReflexive() {
        assertEquals(eq1, eq1, "1st equal instance");
        assertEquals(eq2, eq2, "2nd equal instance");
        assertEquals(eq3, eq3, "3rd equal instance");
        assertEquals(neq, neq, "not-equal instance");
    }

    /**
     * Tests whether <code>equals</code> is <em>symmetric</em> and <em>transitive</em>.
     */
    @Test
    public final void testEqualsIsSymmetricAndTransitive() {
        assertEquals(eq1, eq2, "1st vs. 2nd");
        assertEquals(eq2, eq1, "2nd vs. 1st");
        assertEquals(eq1, eq3, "1st vs. 3rd");
        assertEquals(eq3, eq1, "3rd vs. 1st");
        assertEquals(eq2, eq3, "2nd vs. 3rd");
        assertEquals(eq3, eq2, "3rd vs. 2nd");
    }

    /**
     * Tests the <code>hashCode</code> contract.
     */
    @Test
    public final void testHashCodeContract() {
        assertEquals(eq1.hashCode(), eq2.hashCode(), "1st vs. 2nd");
        assertEquals(eq1.hashCode(), eq3.hashCode(), "1st vs. 3rd");
        assertEquals(eq2.hashCode(), eq3.hashCode(), "2nd vs. 3rd");
    }

    /**
     * Tests the consistency of <code>hashCode</code>.
     */
    @Test
    public final void testHashCodeIsConsistentAcrossInvocations() {
        int eq1Hash = eq1.hashCode();
        int eq2Hash = eq2.hashCode();
        int eq3Hash = eq3.hashCode();
        int neqHash = neq.hashCode();
        for (int i = 0; i < NUM_ITERATIONS; ++i) {
            assertEquals(eq1Hash, eq1.hashCode(), "1st equal instance");
            assertEquals(eq2Hash, eq2.hashCode(), "2nd equal instance");
            assertEquals(eq3Hash, eq3.hashCode(), "3rd equal instance");
            assertEquals(neqHash, neq.hashCode(), "not-equal instance");
        }
    }
}
