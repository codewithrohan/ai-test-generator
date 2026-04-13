package com.aitestgen.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable data class holding extracted method information from the PSI tree.
 * Contains everything the LLM needs to generate meaningful tests.
 */
public final class MethodContext {

    private final String className;
    private final String methodName;
    private final String methodSignature;
    private final String methodBody;
    private final String packageName;
    private final List<String> imports;
    private final List<String> fieldDeclarations;
    private final String sourceLanguage;
    private final String superClassName;
    private final List<String> methodAnnotations;

    /** Creates a new MethodContext with all required fields. */
    public MethodContext(
            String className,
            String methodName,
            String methodSignature,
            String methodBody,
            String packageName,
            List<String> imports,
            List<String> fieldDeclarations,
            String sourceLanguage,
            String superClassName,
            List<String> methodAnnotations
    ) {
        this.className = Objects.requireNonNull(className, "className must not be null");
        this.methodName = Objects.requireNonNull(methodName, "methodName must not be null");
        this.methodSignature = Objects.requireNonNull(methodSignature, "methodSignature must not be null");
        this.methodBody = Objects.requireNonNull(methodBody, "methodBody must not be null");
        this.packageName = Objects.requireNonNull(packageName, "packageName must not be null");
        this.imports = Collections.unmodifiableList(Objects.requireNonNull(imports, "imports must not be null"));
        this.fieldDeclarations = Collections.unmodifiableList(
                Objects.requireNonNull(fieldDeclarations, "fieldDeclarations must not be null"));
        this.sourceLanguage = Objects.requireNonNull(sourceLanguage, "sourceLanguage must not be null");
        this.superClassName = superClassName;
        this.methodAnnotations = Collections.unmodifiableList(
                Objects.requireNonNull(methodAnnotations, "methodAnnotations must not be null"));
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public String getMethodBody() {
        return methodBody;
    }

    public String getPackageName() {
        return packageName;
    }

    public List<String> getImports() {
        return imports;
    }

    public List<String> getFieldDeclarations() {
        return fieldDeclarations;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public List<String> getMethodAnnotations() {
        return methodAnnotations;
    }

    /** Returns a human-readable summary for debugging. */
    public String toSummaryString() {
        return String.format(
                "MethodContext{class='%s', method='%s', language='%s', fields=%d, imports=%d}",
                className, methodName, sourceLanguage,
                fieldDeclarations.size(), imports.size()
        );
    }
}
