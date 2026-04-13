package com.aitestgen.prompt;

import com.aitestgen.model.MethodContext;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Tests for PromptBuilder prompt construction logic.
 */
public class PromptBuilderTest {

    private MethodContext createContext(
            String className, String methodName, String methodBody,
            java.util.List<String> imports, java.util.List<String> fields,
            String superClass, java.util.List<String> annotations
    ) {
        return new MethodContext(
                className, methodName,
                "void " + methodName + "()",
                methodBody, "com.example",
                imports, fields, "java",
                superClass, annotations,
                Collections.emptyList()
        );
    }

    @Test
    public void shouldReturnNonEmptySystemMessage() {
        String system = PromptBuilder.buildSystemMessage();
        assertNotNull(system);
        assertFalse(system.isEmpty());
        assertTrue(system.contains("JUnit"));
    }

    @Test
    public void shouldIncludeClassAndMethodInfo() {
        MethodContext ctx = createContext("OrderService", "placeOrder",
                "public void placeOrder() {}",
                Collections.emptyList(), Collections.emptyList(),
                null, Collections.emptyList());

        String prompt = PromptBuilder.buildPrompt(ctx);

        assertTrue(prompt.contains("OrderService"));
        assertTrue(prompt.contains("placeOrder"));
        assertTrue(prompt.contains("com.example"));
        assertTrue(prompt.contains("java"));
    }

    @Test
    public void shouldIncludeImportsSection() {
        MethodContext ctx = createContext("Svc", "run", "void run() {}",
                Arrays.asList("import java.util.List;", "import java.util.Map;"),
                Collections.emptyList(), null, Collections.emptyList());

        String prompt = PromptBuilder.buildPrompt(ctx);

        assertTrue(prompt.contains("### Imports"));
        assertTrue(prompt.contains("import java.util.List;"));
        assertTrue(prompt.contains("import java.util.Map;"));
    }

    @Test
    public void shouldOmitImportsSectionWhenEmpty() {
        MethodContext ctx = createContext("Svc", "run", "void run() {}",
                Collections.emptyList(), Collections.emptyList(),
                null, Collections.emptyList());

        String prompt = PromptBuilder.buildPrompt(ctx);
        assertFalse(prompt.contains("### Imports"));
    }

    @Test
    public void shouldIncludeFieldsSection() {
        MethodContext ctx = createContext("Svc", "run", "void run() {}",
                Collections.emptyList(),
                Arrays.asList("private UserRepository repo;"),
                null, Collections.emptyList());

        String prompt = PromptBuilder.buildPrompt(ctx);

        assertTrue(prompt.contains("### Class Fields"));
        assertTrue(prompt.contains("private UserRepository repo;"));
    }

    @Test
    public void shouldOmitFieldsSectionWhenEmpty() {
        MethodContext ctx = createContext("Svc", "run", "void run() {}",
                Collections.emptyList(), Collections.emptyList(),
                null, Collections.emptyList());

        String prompt = PromptBuilder.buildPrompt(ctx);
        assertFalse(prompt.contains("### Class Fields"));
    }

    @Test
    public void shouldIncludeSuperClassSection() {
        MethodContext ctx = createContext("Svc", "run", "void run() {}",
                Collections.emptyList(), Collections.emptyList(),
                "com.example.BaseService", Collections.emptyList());

        String prompt = PromptBuilder.buildPrompt(ctx);

        assertTrue(prompt.contains("### Superclass"));
        assertTrue(prompt.contains("com.example.BaseService"));
    }

    @Test
    public void shouldOmitSuperClassSectionWhenNull() {
        MethodContext ctx = createContext("Svc", "run", "void run() {}",
                Collections.emptyList(), Collections.emptyList(),
                null, Collections.emptyList());

        String prompt = PromptBuilder.buildPrompt(ctx);
        assertFalse(prompt.contains("### Superclass"));
    }

    @Test
    public void shouldIncludeAnnotationsSection() {
        MethodContext ctx = createContext("Svc", "run", "void run() {}",
                Collections.emptyList(), Collections.emptyList(),
                null, Arrays.asList("@Override", "@Transactional"));

        String prompt = PromptBuilder.buildPrompt(ctx);

        assertTrue(prompt.contains("### Method Annotations"));
        assertTrue(prompt.contains("@Override"));
        assertTrue(prompt.contains("@Transactional"));
    }

    @Test
    public void shouldTruncateLongMethodBody() {
        char[] longBody = new char[PromptBuilder.MAX_CONTEXT_LENGTH + 500];
        java.util.Arrays.fill(longBody, 'x');
        String body = new String(longBody);

        MethodContext ctx = createContext("Svc", "run", body,
                Collections.emptyList(), Collections.emptyList(),
                null, Collections.emptyList());

        String prompt = PromptBuilder.buildPrompt(ctx);

        assertTrue(prompt.contains("// ... truncated ..."));
        assertFalse(prompt.contains(body));
    }

    @Test
    public void shouldNotTruncateShortMethodBody() {
        String body = "void run() { return; }";

        MethodContext ctx = createContext("Svc", "run", body,
                Collections.emptyList(), Collections.emptyList(),
                null, Collections.emptyList());

        String prompt = PromptBuilder.buildPrompt(ctx);

        assertFalse(prompt.contains("truncated"));
        assertTrue(prompt.contains(body));
    }

    @Test
    public void shouldIncludeRelatedTypesSection() {
        MethodContext ctx = new MethodContext(
                "OrderService", "placeOrder",
                "void placeOrder()",
                "public void placeOrder() {}",
                "com.example",
                Collections.emptyList(),
                Collections.emptyList(),
                "java", null,
                Collections.emptyList(),
                Arrays.asList(
                        "OrderItem(String sku, int quantity, double price)",
                        "Receipt.customerId() → String"
                )
        );

        String prompt = PromptBuilder.buildPrompt(ctx);

        assertTrue(prompt.contains("### Related Types"));
        assertTrue(prompt.contains("OrderItem(String sku, int quantity, double price)"));
        assertTrue(prompt.contains("Receipt.customerId() → String"));
    }

    @Test
    public void shouldOmitRelatedTypesSectionWhenEmpty() {
        MethodContext ctx = createContext("Svc", "run", "void run() {}",
                Collections.emptyList(), Collections.emptyList(),
                null, Collections.emptyList());

        String prompt = PromptBuilder.buildPrompt(ctx);
        assertFalse(prompt.contains("### Related Types"));
    }

    @Test
    public void shouldIncludeRequirementsSection() {
        MethodContext ctx = createContext("Svc", "run", "void run() {}",
                Collections.emptyList(), Collections.emptyList(),
                null, Collections.emptyList());

        String prompt = PromptBuilder.buildPrompt(ctx);

        assertTrue(prompt.contains("## Requirements"));
        assertTrue(prompt.contains("JUnit 5"));
        assertTrue(prompt.contains("Mockito"));
        assertTrue(prompt.contains("SvcTest"));
        assertTrue(prompt.contains("```java"));
    }
}
