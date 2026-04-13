package com.aitestgen.model;

/**
 * Immutable result from the LLM test generation call.
 * Use factory methods success() and failure() to create instances.
 */
public final class GeneratedTestResult {

    private final String testClassName;
    private final String testCode;
    private final String explanation;
    private final boolean success;
    private final String errorMessage;

    private GeneratedTestResult(
            String testClassName,
            String testCode,
            String explanation,
            boolean success,
            String errorMessage
    ) {
        this.testClassName = testClassName;
        this.testCode = testCode;
        this.explanation = explanation;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    /** Creates a successful result with generated test code. */
    public static GeneratedTestResult success(String testClassName, String testCode, String explanation) {
        return new GeneratedTestResult(testClassName, testCode, explanation, true, null);
    }

    /** Creates a failure result with an error message. */
    public static GeneratedTestResult failure(String errorMessage) {
        return new GeneratedTestResult(null, null, null, false, errorMessage);
    }

    public String getTestClassName() {
        return testClassName;
    }

    public String getTestCode() {
        return testCode;
    }

    public String getExplanation() {
        return explanation;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
