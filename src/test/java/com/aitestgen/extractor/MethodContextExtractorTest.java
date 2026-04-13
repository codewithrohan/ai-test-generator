package com.aitestgen.extractor;

import com.aitestgen.model.MethodContext;

import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.Collection;

/**
 * Tests for MethodContextExtractor using the IntelliJ test framework.
 * BasePlatformTestCase provides a lightweight IDE environment with PSI support.
 */
public class MethodContextExtractorTest extends BasePlatformTestCase {

    /** Configures a Java file and extracts the first PsiMethod found. */
    private PsiMethod configureAndFindMethod(String javaCode) {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("TestSubject.java", javaCode);
        Collection<PsiMethod> methods = PsiTreeUtil.findChildrenOfType(file, PsiMethod.class);
        assertFalse("Expected at least one method", methods.isEmpty());
        return methods.iterator().next();
    }

    public void testExtractBasicMethod() {
        PsiMethod method = configureAndFindMethod(
                "package com.example;\n"
                        + "public class Calculator {\n"
                        + "    public int add(int a, int b) {\n"
                        + "        return a + b;\n"
                        + "    }\n"
                        + "}"
        );

        MethodContext ctx = MethodContextExtractor.extract(method);

        assertEquals("Calculator", ctx.getClassName());
        assertEquals("add", ctx.getMethodName());
        assertEquals("com.example", ctx.getPackageName());
        assertEquals("java", ctx.getSourceLanguage());
        assertTrue(ctx.getMethodBody().contains("return a + b"));
        assertTrue(ctx.getMethodSignature().contains("add"));
        assertTrue(ctx.getMethodSignature().contains("int"));
    }

    public void testExtractImports() {
        PsiMethod method = configureAndFindMethod(
                "package com.example;\n"
                        + "import java.util.List;\n"
                        + "import java.util.Map;\n"
                        + "public class Svc {\n"
                        + "    public void run() {}\n"
                        + "}"
        );

        MethodContext ctx = MethodContextExtractor.extract(method);

        assertEquals(2, ctx.getImports().size());
        assertTrue(ctx.getImports().get(0).contains("java.util.List"));
        assertTrue(ctx.getImports().get(1).contains("java.util.Map"));
    }

    public void testExtractFields() {
        PsiMethod method = configureAndFindMethod(
                "package com.example;\n"
                        + "public class Svc {\n"
                        + "    private String name;\n"
                        + "    private int count;\n"
                        + "    public void run() {}\n"
                        + "}"
        );

        MethodContext ctx = MethodContextExtractor.extract(method);

        assertEquals(2, ctx.getFieldDeclarations().size());
        assertTrue(ctx.getFieldDeclarations().get(0).contains("name"));
        assertTrue(ctx.getFieldDeclarations().get(1).contains("count"));
    }

    public void testExtractAnnotations() {
        PsiMethod method = configureAndFindMethod(
                "package com.example;\n"
                        + "public class Svc extends Base {\n"
                        + "    @Override\n"
                        + "    public void run() {}\n"
                        + "}"
        );

        MethodContext ctx = MethodContextExtractor.extract(method);

        assertEquals(1, ctx.getMethodAnnotations().size());
        assertTrue(ctx.getMethodAnnotations().get(0).contains("Override"));
    }

    public void testExtractNoSuperClassForObject() {
        PsiMethod method = configureAndFindMethod(
                "package com.example;\n"
                        + "public class Plain {\n"
                        + "    public void run() {}\n"
                        + "}"
        );

        MethodContext ctx = MethodContextExtractor.extract(method);

        assertNull(ctx.getSuperClassName());
    }

    public void testExtractMethodWithNoParams() {
        PsiMethod method = configureAndFindMethod(
                "package com.example;\n"
                        + "public class Svc {\n"
                        + "    public String getName() { return \"test\"; }\n"
                        + "}"
        );

        MethodContext ctx = MethodContextExtractor.extract(method);

        assertEquals("getName", ctx.getMethodName());
        assertTrue(ctx.getMethodSignature().contains("String"));
        assertTrue(ctx.getFieldDeclarations().isEmpty());
    }

    public void testExtractEmptyPackage() {
        PsiMethod method = configureAndFindMethod(
                "public class NoPackage {\n"
                        + "    public void doIt() {}\n"
                        + "}"
        );

        MethodContext ctx = MethodContextExtractor.extract(method);

        assertEquals("", ctx.getPackageName());
        assertEquals("NoPackage", ctx.getClassName());
    }
}
