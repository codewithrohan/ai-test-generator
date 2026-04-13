package com.aitestgen.generator;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;

import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Creates or locates a test file at the conventional src/test/java/ path
 * and inserts generated test code. Falls back to clipboard on failure.
 */
public final class TestFileWriter {

    private TestFileWriter() {
        // utility class
    }

    /**
     * Writes generated test code to the appropriate test file.
     * Must be called on the EDT (Event Dispatch Thread).
     *
     * @return true if the file was written successfully, false if clipboard fallback was used
     */
    public static boolean writeTestFile(
            Project project,
            String packageName,
            String testClassName,
            String testCode
    ) {
        AtomicBoolean success = new AtomicBoolean(false);
        try {
            WriteCommandAction.runWriteCommandAction(project, "Generate Test", null, () -> {
                VirtualFile testRoot = findOrCreateTestRoot(project);
                if (testRoot == null) {
                    copyToClipboard(testCode);
                    return;
                }

                VirtualFile packageDir = findOrCreatePackageDir(testRoot, packageName);
                if (packageDir == null) {
                    copyToClipboard(testCode);
                    return;
                }

                PsiDirectory psiDir = PsiManager.getInstance(project).findDirectory(packageDir);
                if (psiDir == null) {
                    copyToClipboard(testCode);
                    return;
                }

                String fileName = testClassName + ".java";
                PsiFile existingFile = psiDir.findFile(fileName);
                if (existingFile != null) {
                    existingFile.delete();
                }

                PsiFile testFile = PsiFileFactory.getInstance(project)
                        .createFileFromText(fileName, com.intellij.lang.java.JavaLanguage.INSTANCE, testCode);

                PsiFile addedFile = (PsiFile) psiDir.add(testFile);

                JavaCodeStyleManager.getInstance(project).optimizeImports(addedFile);
                CodeStyleManager.getInstance(project).reformat(addedFile);

                success.set(true);
            });

            if (!success.get()) {
                copyToClipboard(testCode);
            }
            return success.get();
        } catch (Exception e) {
            copyToClipboard(testCode);
            return false;
        }
    }

    /** Finds the src/test/java directory relative to the project content root. */
    private static VirtualFile findOrCreateTestRoot(Project project) {
        VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentRoots();
        VirtualFile baseDir = roots.length > 0 ? roots[0] : null;
        if (baseDir == null) {
            return null;
        }

        VirtualFile testJava = baseDir.findFileByRelativePath("src/test/java");
        if (testJava != null) {
            return testJava;
        }

        try {
            return VfsUtil.createDirectoryIfMissing(baseDir, "src/test/java");
        } catch (IOException e) {
            return null;
        }
    }

    /** Creates nested package directories under the test root. */
    private static VirtualFile findOrCreatePackageDir(VirtualFile testRoot, String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return testRoot;
        }

        String relativePath = packageName.replace('.', '/');
        VirtualFile existing = testRoot.findFileByRelativePath(relativePath);
        if (existing != null) {
            return existing;
        }

        try {
            return VfsUtil.createDirectoryIfMissing(testRoot, relativePath);
        } catch (IOException e) {
            return null;
        }
    }

    /** Copies test code to the system clipboard as a fallback. */
    static void copyToClipboard(String testCode) {
        CopyPasteManager.getInstance().setContents(new StringSelection(testCode));
    }
}
