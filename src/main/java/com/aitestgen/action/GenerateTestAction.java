package com.aitestgen.action;

import com.aitestgen.client.ClaudeApiClient;
import com.aitestgen.extractor.MethodContextExtractor;
import com.aitestgen.generator.TestFileWriter;
import com.aitestgen.model.GeneratedTestResult;
import com.aitestgen.model.MethodContext;
import com.aitestgen.prompt.PromptBuilder;
import com.aitestgen.settings.PluginSettings;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;

/**
 * Editor action that generates unit tests for the Java method under the caret.
 * Registered in plugin.xml for EditorPopupMenu and GenerateGroup.
 */
public final class GenerateTestAction extends AnAction {

    private static final String NOTIFICATION_GROUP_ID = "AiTestGenerator";

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiMethod method = findMethodAtCaret(e);
        e.getPresentation().setEnabledAndVisible(method != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        String apiKey = PluginSettings.getApiKey();
        if (apiKey.isEmpty()) {
            showNotification(project,
                    "API key not configured. Go to Settings > Tools > AI Test Generator.",
                    NotificationType.WARNING);
            return;
        }

        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (!(psiFile instanceof PsiJavaFile) || editor == null) {
            showNotification(project,
                    "Place the caret inside a Java method to generate tests.",
                    NotificationType.WARNING);
            return;
        }

        int offset = editor.getCaretModel().getOffset();

        new Task.Backgroundable(project, "Generating Unit Test with AI", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setFraction(0.1);
                indicator.setText("Extracting method context...");

                MethodContext context = ApplicationManager.getApplication().runReadAction(
                        (Computable<MethodContext>) () -> {
                            PsiElement element = psiFile.findElementAt(offset);
                            if (element == null) {
                                return null;
                            }
                            PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);
                            if (method == null) {
                                return null;
                            }
                            return MethodContextExtractor.extract(method);
                        });

                if (context == null) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            showNotification(project,
                                    "Could not find a Java method at the caret position.",
                                    NotificationType.WARNING));
                    return;
                }

                indicator.setFraction(0.3);
                indicator.setText("Building prompt...");

                String systemPrompt = PromptBuilder.buildSystemMessage();
                String userPrompt = PromptBuilder.buildPrompt(context);

                indicator.setFraction(0.4);
                indicator.setText("Calling Claude API...");

                ClaudeApiClient client = new ClaudeApiClient();
                GeneratedTestResult result = client.generate(systemPrompt, userPrompt);

                indicator.setFraction(0.9);

                if (!result.isSuccess()) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            showNotification(project, result.getErrorMessage(),
                                    NotificationType.ERROR));
                    return;
                }

                indicator.setText("Writing test file...");
                ApplicationManager.getApplication().invokeLater(() -> {
                    TestFileWriter.WriteResult writeResult = TestFileWriter.writeTestFile(
                            project,
                            context.getPackageName(),
                            result.getTestClassName(),
                            result.getTestCode());

                    if (writeResult == TestFileWriter.WriteResult.WRITTEN) {
                        showNotification(project,
                                result.getExplanation(),
                                NotificationType.INFORMATION);
                    } else if (writeResult == TestFileWriter.WriteResult.CLIPBOARD_FALLBACK) {
                        showNotification(project,
                                "Test copied to clipboard (could not create test file). "
                                        + result.getExplanation(),
                                NotificationType.WARNING);
                    }
                    // CANCELLED: user declined to overwrite — do nothing
                });

                indicator.setFraction(1.0);
            }
        }.queue();
    }

    /** Finds the PsiMethod at the current caret position, or null. */
    private static PsiMethod findMethodAtCaret(@NotNull AnActionEvent e) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (!(psiFile instanceof PsiJavaFile)) {
            return null;
        }

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return null;
        }

        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);
        if (element == null) {
            return null;
        }

        return PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);
    }

    /** Shows a balloon notification via the plugin's notification group. */
    private static void showNotification(Project project, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification("AI Test Generator", content, type)
                .notify(project);
    }
}
