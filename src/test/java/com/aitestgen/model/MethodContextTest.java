package com.aitestgen.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for the MethodContext immutable data class.
 */
public class MethodContextTest {

    /** Creates a MethodContext with typical values for reuse in tests. */
    private MethodContext createSampleContext() {
        return new MethodContext(
                "UserService",
                "findById",
                "User findById(Long id)",
                "public User findById(Long id) { return repo.findById(id); }",
                "com.example.service",
                Arrays.asList("import java.util.List;", "import com.example.User;"),
                Arrays.asList("private UserRepository repo;"),
                "java",
                "BaseService",
                Arrays.asList("@Override")
        );
    }

    @Test
    public void shouldStoreAllFieldsCorrectly() {
        MethodContext ctx = createSampleContext();

        assertEquals("UserService", ctx.getClassName());
        assertEquals("findById", ctx.getMethodName());
        assertEquals("User findById(Long id)", ctx.getMethodSignature());
        assertEquals("com.example.service", ctx.getPackageName());
        assertEquals("java", ctx.getSourceLanguage());
        assertEquals("BaseService", ctx.getSuperClassName());
        assertEquals(2, ctx.getImports().size());
        assertEquals(1, ctx.getFieldDeclarations().size());
        assertEquals(1, ctx.getMethodAnnotations().size());
    }

    @Test
    public void shouldAllowNullSuperClassName() {
        MethodContext ctx = new MethodContext(
                "MyClass", "doStuff", "void doStuff()", "void doStuff() {}",
                "com.example", Collections.emptyList(), Collections.emptyList(),
                "java", null, Collections.emptyList()
        );
        assertNull(ctx.getSuperClassName());
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullClassName() {
        new MethodContext(
                null, "m", "sig", "body", "pkg",
                Collections.emptyList(), Collections.emptyList(),
                "java", null, Collections.emptyList()
        );
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullMethodName() {
        new MethodContext(
                "C", null, "sig", "body", "pkg",
                Collections.emptyList(), Collections.emptyList(),
                "java", null, Collections.emptyList()
        );
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldReturnUnmodifiableImports() {
        MethodContext ctx = createSampleContext();
        ctx.getImports().add("import java.io.*;");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldReturnUnmodifiableFieldDeclarations() {
        MethodContext ctx = createSampleContext();
        ctx.getFieldDeclarations().add("private int x;");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldReturnUnmodifiableAnnotations() {
        MethodContext ctx = createSampleContext();
        ctx.getMethodAnnotations().add("@Deprecated");
    }

    @Test
    public void shouldProduceReadableSummaryString() {
        MethodContext ctx = createSampleContext();
        String summary = ctx.toSummaryString();

        assertTrue(summary.contains("UserService"));
        assertTrue(summary.contains("findById"));
        assertTrue(summary.contains("java"));
    }

    @Test
    public void shouldHandleEmptyCollections() {
        MethodContext ctx = new MethodContext(
                "Empty", "run", "void run()", "void run() {}",
                "", Collections.emptyList(), Collections.emptyList(),
                "java", null, Collections.emptyList()
        );

        assertTrue(ctx.getImports().isEmpty());
        assertTrue(ctx.getFieldDeclarations().isEmpty());
        assertTrue(ctx.getMethodAnnotations().isEmpty());
        assertEquals("", ctx.getPackageName());
    }
}
