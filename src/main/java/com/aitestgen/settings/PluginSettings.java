package com.aitestgen.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

/**
 * Utility class for secure API key storage via IntelliJ's PasswordSafe.
 * Uses the platform's native credential store (Keychain, Credential Manager, etc.).
 */
public final class PluginSettings {

    private static final String CREDENTIAL_KEY = "AiTestGenerator_GeminiApiKey";

    private PluginSettings() {
        // utility class
    }

    /** Retrieves the stored API key, or empty string if not set. */
    public static String getApiKey() {
        CredentialAttributes attrs = createCredentialAttributes();
        Credentials credentials = PasswordSafe.getInstance().get(attrs);
        if (credentials == null) {
            return "";
        }
        String password = credentials.getPasswordAsString();
        return password != null ? password : "";
    }

    /** Stores the API key securely in the IDE's password safe. */
    public static void setApiKey(String apiKey) {
        CredentialAttributes attrs = createCredentialAttributes();
        Credentials credentials = new Credentials("", apiKey);
        PasswordSafe.getInstance().set(attrs, credentials);
    }

    private static CredentialAttributes createCredentialAttributes() {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName("AiTestGenerator", CREDENTIAL_KEY)
        );
    }
}
