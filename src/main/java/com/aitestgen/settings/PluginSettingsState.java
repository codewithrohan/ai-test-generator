package com.aitestgen.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persisted application-level settings for the AI Test Generator plugin.
 * Stores model selection and generation preferences. API key is stored
 * separately in PasswordSafe via PluginSettings.
 */
@Service(Service.Level.APP)
@State(name = "AiTestGeneratorSettings", storages = @Storage("AiTestGeneratorSettings.xml"))
public final class PluginSettingsState implements PersistentStateComponent<PluginSettingsState> {

    public static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";
    public static final int DEFAULT_MAX_TOKENS = 4096;
    public static final int MIN_MAX_TOKENS = 1024;
    public static final int MAX_MAX_TOKENS = 8192;

    private String modelName = DEFAULT_MODEL;
    private int maxTokens = DEFAULT_MAX_TOKENS;
    private boolean copyToClipboard = false;

    /** Returns the application-level instance. */
    public static PluginSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(PluginSettingsState.class);
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public boolean isCopyToClipboard() {
        return copyToClipboard;
    }

    public void setCopyToClipboard(boolean copyToClipboard) {
        this.copyToClipboard = copyToClipboard;
    }

    @Override
    public @Nullable PluginSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull PluginSettingsState state) {
        this.modelName = state.modelName;
        this.maxTokens = state.maxTokens;
        this.copyToClipboard = state.copyToClipboard;
    }
}
