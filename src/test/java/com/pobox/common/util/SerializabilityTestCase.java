package com.pobox.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Extend me in order to test the serializability of a class. Override my {@link #createInstance() createInstance}
 * methods to provide me with an object to test against. The object's class must implement {@link java.io.Serializable
 * Serializable}.
 */
public abstract class SerializabilityTestCase {
    private Serializable obj;

    /**
     * Creates and returns an instance of the class under test.
     *
     * @return a new instance of the class under test
     */
    protected abstract Serializable createInstance();

    /**
     * Sets up the test fixture.
     *
     */
    @BeforeEach
    public void setUp() {
        obj = createInstance();
        // We want these assertions to yield errors, not failures.
        try {
            assertNotNull(obj, "createInstance() returned null");
        } catch (AssertionFailedError ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    /**
     * Verifies that an instance of the class under test can be serialized and deserialized without error.
     */
    @Test
    public final void testSerializability() throws Exception {
        byte[] frozenChunk;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.flush();
            oos.close();
            frozenChunk = baos.toByteArray();
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(frozenChunk);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Serializable thawed = (Serializable) ois.readObject();
        checkThawedObject(obj, thawed);
    }

    /**
     * Template method--override this to perform checks on the deserialized form of the object serialized in
     * {@link #testSerializability}. If not overridden, this asserts that the pre-serialization and deserialized forms
     * of the object compare equal via {@link Object#equals(Object) equals}.
     *
     * @param expected the pre-serialization form of the object
     * @param actual   the deserialized form of the object
     */
    protected void checkThawedObject(Serializable expected, Serializable actual) {
        assertEquals(expected, actual, "thawed object comparison");
    }
}
