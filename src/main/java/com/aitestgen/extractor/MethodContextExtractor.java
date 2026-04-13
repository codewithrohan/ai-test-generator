package com.aitestgen.extractor;

import com.aitestgen.model.MethodContext;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extracts method context from the IntelliJ PSI tree.
 * All methods must be called inside a ReadAction.
 */
public final class MethodContextExtractor {

    private MethodContextExtractor() {
        // utility class
    }

    /**
     * Extracts comprehensive context from a PsiMethod for test generation.
     * Must be called within ReadAction.compute().
     */
    public static MethodContext extract(PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            throw new IllegalArgumentException("Method has no containing class");
        }

        PsiFile containingFile = method.getContainingFile();
        if (!(containingFile instanceof PsiJavaFile)) {
            throw new IllegalArgumentException("Method is not in a Java file");
        }
        PsiJavaFile javaFile = (PsiJavaFile) containingFile;

        String className = containingClass.getName() != null
                ? containingClass.getName()
                : "UnknownClass";

        String methodName = method.getName();
        String methodSignature = buildMethodSignature(method);
        String methodBody = method.getText();
        String packageName = javaFile.getPackageName();
        List<String> imports = extractImports(javaFile);
        List<String> fieldDeclarations = extractFields(containingClass);
        List<String> annotations = extractAnnotations(method);
        String superClassName = extractSuperClassName(containingClass);

        return new MethodContext(
                className,
                methodName,
                methodSignature,
                methodBody,
                packageName,
                imports,
                fieldDeclarations,
                "java",
                superClassName,
                annotations
        );
    }

    /** Builds a readable method signature string. */
    private static String buildMethodSignature(PsiMethod method) {
        StringBuilder sb = new StringBuilder();

        PsiType returnType = method.getReturnType();
        if (returnType != null) {
            sb.append(returnType.getPresentableText()).append(" ");
        }

        sb.append(method.getName()).append("(");

        PsiParameter[] params = method.getParameterList().getParameters();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(params[i].getType().getPresentableText());
            sb.append(" ");
            sb.append(params[i].getName());
        }

        sb.append(")");
        return sb.toString();
    }

    private static List<String> extractImports(PsiJavaFile javaFile) {
        if (javaFile.getImportList() == null) {
            return Collections.emptyList();
        }

        PsiImportStatement[] importStatements = javaFile.getImportList().getImportStatements();
        List<String> imports = new ArrayList<>(importStatements.length);
        for (PsiImportStatement stmt : importStatements) {
            imports.add(stmt.getText());
        }
        return imports;
    }

    private static List<String> extractFields(PsiClass containingClass) {
        PsiField[] fields = containingClass.getFields();
        List<String> fieldDeclarations = new ArrayList<>(fields.length);
        for (PsiField field : fields) {
            fieldDeclarations.add(field.getText());
        }
        return fieldDeclarations;
    }

    private static List<String> extractAnnotations(PsiMethod method) {
        PsiAnnotation[] annotations = method.getAnnotations();
        List<String> annotationTexts = new ArrayList<>(annotations.length);
        for (PsiAnnotation annotation : annotations) {
            annotationTexts.add(annotation.getText());
        }
        return annotationTexts;
    }

    private static String extractSuperClassName(PsiClass containingClass) {
        PsiClass superClass = containingClass.getSuperClass();
        if (superClass == null) {
            return null;
        }
        String qualifiedName = superClass.getQualifiedName();
        if ("java.lang.Object".equals(qualifiedName)) {
            return null;
        }
        return qualifiedName;
    }
}
