package com.aitestgen.generator;

import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.io.IOException;

/**
 * Tests for TestFileWriter using the IntelliJ test framework.
 */
public class TestFileWriterTest extends BasePlatformTestCase {

    private static final String TEST_CODE =
            "package com.example;\n\n"
                    + "import org.junit.jupiter.api.Test;\n"
                    + "import static org.junit.jupiter.api.Assertions.*;\n\n"
                    + "public class CalcTest {\n"
                    + "    @Test\n"
                    + "    void shouldAdd() {\n"
                    + "        assertEquals(3, 1 + 2);\n"
                    + "    }\n"
                    + "}\n";

    private VirtualFile getContentRoot() {
        VirtualFile[] roots = ProjectRootManager.getInstance(getProject()).getContentRoots();
        return roots.length > 0 ? roots[0] : null;
    }

    public void testWriteTestFileCreatesFileInCorrectLocation() {
        boolean result = TestFileWriter.writeTestFile(
                getProject(), "com.example", "CalcTest", TEST_CODE);

        assertTrue("Expected file write to succeed", result);

        VirtualFile baseDir = getContentRoot();
        assertNotNull(baseDir);

        VirtualFile testFile = baseDir.findFileByRelativePath(
                "src/test/java/com/example/CalcTest.java");
        assertNotNull("Test file should exist at conventional path", testFile);
        assertFileContains(testFile, "CalcTest");
        assertFileContains(testFile, "shouldAdd");
    }

    public void testWriteTestFileWithEmptyPackage() {
        boolean result = TestFileWriter.writeTestFile(
                getProject(), "", "SimpleTest",
                "public class SimpleTest { @Test void test() {} }");

        assertTrue("Expected file write to succeed", result);

        VirtualFile baseDir = getContentRoot();
        VirtualFile testFile = baseDir.findFileByRelativePath(
                "src/test/java/SimpleTest.java");
        assertNotNull("Test file should exist at root of test sources", testFile);
    }

    public void testWriteTestFileOverwritesExisting() {
        TestFileWriter.writeTestFile(
                getProject(), "com.example", "CalcTest",
                "public class CalcTest { /* first version */ }");

        boolean result = TestFileWriter.writeTestFile(
                getProject(), "com.example", "CalcTest", TEST_CODE);

        assertTrue("Expected overwrite to succeed", result);

        VirtualFile baseDir = getContentRoot();
        VirtualFile testFile = baseDir.findFileByRelativePath(
                "src/test/java/com/example/CalcTest.java");
        assertNotNull(testFile);
        assertFileContains(testFile, "shouldAdd");
    }

    private void assertFileContains(VirtualFile file, String expected) {
        try {
            String content = new String(file.contentsToByteArray(), file.getCharset());
            assertTrue("File should contain '" + expected + "'", content.contains(expected));
        } catch (IOException e) {
            fail("Could not read file: " + e.getMessage());
        }
    }
}
