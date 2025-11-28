package org.junit.jupiter.api;

import java.util.function.Supplier;

public class Assertions {

    public static void assertNotNull(Object actual) {
        if (actual == null) {
            throw new AssertionError("Expected non-null but was null");
        }
    }

    public static void assertNotNull(Object actual, String message) {
        if (actual == null) {
            throw new AssertionError(message);
        }
    }

    public static void assertNull(Object actual) {
        if (actual != null) {
            throw new AssertionError("Expected null but was: " + actual);
        }
    }

    public static void assertNull(Object actual, String message) {
        if (actual != null) {
            throw new AssertionError(message);
        }
    }

    public static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected true but was false");
        }
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertFalse(boolean condition) {
        if (condition) {
            throw new AssertionError("Expected false but was true");
        }
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError("Expected: " + expected + " but was: " + actual);
        }
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(message + " - Expected: " + expected + " but was: " + actual);
        }
    }

    public static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("Expected: " + expected + " but was: " + actual);
        }
    }

    public static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + " - Expected: " + expected + " but was: " + actual);
        }
    }

    public static void assertEquals(boolean expected, boolean actual) {
        if (expected != actual) {
            throw new AssertionError("Expected: " + expected + " but was: " + actual);
        }
    }

    public static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable) {
        try {
            executable.execute();
            throw new AssertionError("Expected " + expectedType.getName() + " to be thrown, but nothing was thrown");
        } catch (Throwable actualException) {
            if (expectedType.isInstance(actualException)) {
                return expectedType.cast(actualException);
            }
            throw new AssertionError("Expected " + expectedType.getName() + " but was " + actualException.getClass().getName(), actualException);
        }
    }

    public static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable, String message) {
        try {
            executable.execute();
            throw new AssertionError(message + ": Expected " + expectedType.getName() + " to be thrown");
        } catch (Throwable actualException) {
            if (expectedType.isInstance(actualException)) {
                return expectedType.cast(actualException);
            }
            throw new AssertionError(message + ": Expected " + expectedType.getName() + " but was " + actualException.getClass().getName(), actualException);
        }
    }

    @FunctionalInterface
    public interface Executable {
        void execute() throws Throwable;
    }
}
