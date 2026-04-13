package com.aitestgen.settings;

import com.intellij.openapi.options.Configurable;

import javax.swing.*;
import java.awt.*;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

/**
 * Settings UI panel accessible via Settings > Tools > AI Test Generator.
 * Provides controls for API key, model selection, and generation preferences.
 */
public final class PluginSettingsConfigurable implements Configurable {

    private static final String[] MODEL_OPTIONS = {
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "mixtral-8x7b-32768"
    };

    private JPasswordField apiKeyField;
    private JComboBox<String> modelComboBox;
    private JSpinner maxTokensSpinner;
    private JCheckBox copyToClipboardCheckBox;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "AI Test Generator";
    }

    @Override
    public @Nullable JComponent createComponent() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // API Key
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel("API Key:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        apiKeyField = new JPasswordField(40);
        apiKeyField.setText(PluginSettings.getApiKey());
        panel.add(apiKeyField, gbc);

        row++;

        // Model selection
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel("Model:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        modelComboBox = new JComboBox<>(MODEL_OPTIONS);
        modelComboBox.setSelectedItem(PluginSettingsState.getInstance().getModelName());
        panel.add(modelComboBox, gbc);

        row++;

        // Max tokens
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel("Max Tokens:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
                PluginSettingsState.getInstance().getMaxTokens(),
                PluginSettingsState.MIN_MAX_TOKENS,
                PluginSettingsState.MAX_MAX_TOKENS,
                512
        );
        maxTokensSpinner = new JSpinner(spinnerModel);
        panel.add(maxTokensSpinner, gbc);

        row++;

        // Copy to clipboard
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        copyToClipboardCheckBox = new JCheckBox(
                "Also copy generated test to clipboard",
                PluginSettingsState.getInstance().isCopyToClipboard()
        );
        panel.add(copyToClipboardCheckBox, gbc);

        // Spacer to push everything to the top
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weighty = 1;
        panel.add(new JPanel(), gbc);

        return panel;
    }

    @Override
    public boolean isModified() {
        PluginSettingsState state = PluginSettingsState.getInstance();
        String currentApiKey = PluginSettings.getApiKey();
        String enteredApiKey = new String(apiKeyField.getPassword());

        return !enteredApiKey.equals(currentApiKey)
                || !String.valueOf(modelComboBox.getSelectedItem()).equals(state.getModelName())
                || (int) maxTokensSpinner.getValue() != state.getMaxTokens()
                || copyToClipboardCheckBox.isSelected() != state.isCopyToClipboard();
    }

    @Override
    public void apply() {
        PluginSettings.setApiKey(new String(apiKeyField.getPassword()));

        PluginSettingsState state = PluginSettingsState.getInstance();
        state.setModelName(String.valueOf(modelComboBox.getSelectedItem()));
        state.setMaxTokens((int) maxTokensSpinner.getValue());
        state.setCopyToClipboard(copyToClipboardCheckBox.isSelected());
    }

    @Override
    public void reset() {
        apiKeyField.setText(PluginSettings.getApiKey());

        PluginSettingsState state = PluginSettingsState.getInstance();
        modelComboBox.setSelectedItem(state.getModelName());
        maxTokensSpinner.setValue(state.getMaxTokens());
        copyToClipboardCheckBox.setSelected(state.isCopyToClipboard());
    }
}
