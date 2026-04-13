package com.aitestgen.generator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
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

    /** Result of a writeTestFile call. */
    public enum WriteResult {
        /** File was written successfully. */
        WRITTEN,
        /** File could not be written; code was copied to the clipboard instead. */
        CLIPBOARD_FALLBACK,
        /** User declined to overwrite an existing file; nothing was changed. */
        CANCELLED
    }

    private TestFileWriter() {
        // utility class
    }

    /**
     * Writes generated test code to the appropriate test file.
     * Must be called on the EDT (Event Dispatch Thread).
     * Shows a confirmation dialog before overwriting an existing file.
     *
     * @return {@link WriteResult} indicating outcome
     */
    public static WriteResult writeTestFile(
            Project project,
            String packageName,
            String testClassName,
            String testCode
    ) {
        String fileName = testClassName + ".java";

        // Read-only pre-check — confirm before overwriting any existing file.
        // Dialog is suppressed in unit test mode; tests always proceed with overwrite.
        VirtualFile existing = findExistingTestFile(project, packageName, fileName);
        if (existing != null && !ApplicationManager.getApplication().isUnitTestMode()) {
            int answer = Messages.showYesNoDialog(
                    project,
                    "'" + fileName + "' already exists. Overwrite it?",
                    "Overwrite Existing Test File",
                    Messages.getWarningIcon()
            );
            if (answer != Messages.YES) {
                return WriteResult.CANCELLED;
            }
        }

        AtomicBoolean success = new AtomicBoolean(false);
        try {
            WriteCommandAction.runWriteCommandAction(project, "Generate Test", null, () -> {
                VirtualFile testRoot = findOrCreateTestRoot(project);
                if (testRoot == null) {
                    return;
                }

                VirtualFile packageDir = findOrCreatePackageDir(testRoot, packageName);
                if (packageDir == null) {
                    return;
                }

                PsiDirectory psiDir = PsiManager.getInstance(project).findDirectory(packageDir);
                if (psiDir == null) {
                    return;
                }

                PsiFile existingPsi = psiDir.findFile(fileName);
                if (existingPsi != null) {
                    existingPsi.delete(); // safe — user confirmed above
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
                return WriteResult.CLIPBOARD_FALLBACK;
            }
            return WriteResult.WRITTEN;
        } catch (Exception e) {
            copyToClipboard(testCode);
            return WriteResult.CLIPBOARD_FALLBACK;
        }
    }

    /**
     * Read-only check: returns the VirtualFile if a test file already exists at the
     * conventional path, or null if the path is clear.
     */
    private static VirtualFile findExistingTestFile(Project project, String packageName, String fileName) {
        VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentRoots();
        if (roots.length == 0) {
            return null;
        }
        VirtualFile testJava = roots[0].findFileByRelativePath("src/test/java");
        if (testJava == null) {
            return null;
        }
        String relativePath = (packageName != null && !packageName.isEmpty())
                ? packageName.replace('.', '/') + "/" + fileName
                : fileName;
        return testJava.findFileByRelativePath(relativePath);
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
