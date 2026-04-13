package com.aitestgen.prompt;

import com.aitestgen.model.MethodContext;

import java.util.List;

/**
 * Constructs structured prompts for the Claude API from extracted method context.
 * Pure string logic with no IntelliJ dependencies.
 */
public final class PromptBuilder {

    /** Maximum method body length to prevent excessive token usage. */
    static final int MAX_CONTEXT_LENGTH = 8000;

    private static final String SYSTEM_PROMPT = String.join("\n",
            "You are a senior Java developer specializing in writing comprehensive unit tests.",
            "You write clean, well-structured JUnit 5 tests following the Arrange-Act-Assert pattern.",
            "You use Mockito for mocking dependencies and write descriptive @DisplayName annotations.",
            "You always generate compilable code with correct imports."
    );

    private PromptBuilder() {
        // utility class
    }

    /** Returns the system prompt for the Claude API system field. */
    public static String buildSystemMessage() {
        return SYSTEM_PROMPT;
    }

    /** Builds the user prompt from extracted method context. */
    public static String buildPrompt(MethodContext context) {
        StringBuilder sb = new StringBuilder();

        sb.append("Generate comprehensive JUnit 5 tests for the following method.\n\n");
        sb.append("## Source Method\n");
        sb.append("Package: ").append(context.getPackageName()).append("\n");
        sb.append("Class: ").append(context.getClassName()).append("\n");
        sb.append("Language: ").append(context.getSourceLanguage()).append("\n\n");

        appendImports(sb, context.getImports());
        appendFields(sb, context.getFieldDeclarations());
        appendRelatedTypes(sb, context.getRelatedTypeSignatures());
        appendAnnotations(sb, context.getMethodAnnotations());
        appendSuperClass(sb, context.getSuperClassName());
        appendMethodBody(sb, context.getMethodBody());
        appendRequirements(sb, context.getClassName(), context.getMethodName());

        return sb.toString();
    }

    private static void appendImports(StringBuilder sb, List<String> imports) {
        if (imports.isEmpty()) {
            return;
        }
        sb.append("### Imports\n");
        for (String imp : imports) {
            sb.append(imp).append("\n");
        }
        sb.append("\n");
    }

    private static void appendFields(StringBuilder sb, List<String> fieldDeclarations) {
        if (fieldDeclarations.isEmpty()) {
            return;
        }
        sb.append("### Class Fields (potential dependencies to mock)\n");
        for (String field : fieldDeclarations) {
            sb.append(field).append("\n");
        }
        sb.append("\n");
    }

    private static void appendRelatedTypes(StringBuilder sb, List<String> relatedTypeSignatures) {
        if (relatedTypeSignatures.isEmpty()) {
            return;
        }
        sb.append("### Related Types (use these exact signatures — do not guess constructors)\n");
        for (String sig : relatedTypeSignatures) {
            sb.append("- ").append(sig).append("\n");
        }
        sb.append("\n");
    }

    private static void appendAnnotations(StringBuilder sb, List<String> annotations) {
        if (annotations.isEmpty()) {
            return;
        }
        sb.append("### Method Annotations\n");
        for (String annotation : annotations) {
            sb.append(annotation).append("\n");
        }
        sb.append("\n");
    }

    private static void appendSuperClass(StringBuilder sb, String superClassName) {
        if (superClassName == null || superClassName.isEmpty()) {
            return;
        }
        sb.append("### Superclass\n");
        sb.append(superClassName).append("\n\n");
    }

    private static void appendMethodBody(StringBuilder sb, String methodBody) {
        sb.append("### Method Under Test\n```java\n");
        if (methodBody.length() > MAX_CONTEXT_LENGTH) {
            sb.append(methodBody, 0, MAX_CONTEXT_LENGTH);
            sb.append("\n// ... truncated ...\n");
        } else {
            sb.append(methodBody);
        }
        sb.append("\n```\n\n");
    }

    private static void appendRequirements(StringBuilder sb, String className, String methodName) {
        sb.append("## Requirements\n");
        sb.append("1. Use JUnit 5 (org.junit.jupiter.api.*)\n");
        sb.append("2. Use Mockito for mocking dependencies\n");
        sb.append("3. Generate a test class named ").append(className).append("Test\n");
        sb.append("4. Include:\n");
        sb.append("   a. A happy-path test for normal expected behavior\n");
        sb.append("   b. Edge case tests (null inputs, empty collections, boundary values)\n");
        sb.append("   c. Error case tests where applicable\n");
        sb.append("   d. @DisplayName annotations with descriptive names\n");
        sb.append("   e. Comments explaining mock setup rationale\n");
        sb.append("5. Follow the Arrange-Act-Assert pattern\n");
        sb.append("6. The test class must compile independently with proper imports\n\n");
        sb.append("## Output Format\n");
        sb.append("Return ONLY the complete Java test class source code, ");
        sb.append("wrapped in ```java code fences.\n");
        sb.append("Do not include explanations outside the code block.\n");
    }
}
