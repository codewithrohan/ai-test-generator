package com.aitestgen.model;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for the GeneratedTestResult factory methods and accessors.
 */
public class GeneratedTestResultTest {

    @Test
    public void shouldCreateSuccessResult() {
        GeneratedTestResult result = GeneratedTestResult.success(
                "UserServiceTest",
                "public class UserServiceTest { @Test void test() {} }",
                "Generated 1 test method(s)"
        );

        assertTrue(result.isSuccess());
        assertEquals("UserServiceTest", result.getTestClassName());
        assertNotNull(result.getTestCode());
        assertEquals("Generated 1 test method(s)", result.getExplanation());
        assertNull(result.getErrorMessage());
    }

    @Test
    public void shouldCreateFailureResult() {
        GeneratedTestResult result = GeneratedTestResult.failure("Network timeout");

        assertFalse(result.isSuccess());
        assertEquals("Network timeout", result.getErrorMessage());
        assertNull(result.getTestClassName());
        assertNull(result.getTestCode());
        assertNull(result.getExplanation());
    }

    @Test
    public void shouldHandleEmptyStringsInSuccess() {
        GeneratedTestResult result = GeneratedTestResult.success("", "", "");

        assertTrue(result.isSuccess());
        assertEquals("", result.getTestClassName());
        assertEquals("", result.getTestCode());
        assertEquals("", result.getExplanation());
    }

    @Test
    public void shouldHandleEmptyErrorMessage() {
        GeneratedTestResult result = GeneratedTestResult.failure("");

        assertFalse(result.isSuccess());
        assertEquals("", result.getErrorMessage());
    }
}
