package io.worldportal.app.service.impl;

import io.worldportal.app.model.RemoteProfile;

public class SshConnectionService {
    private volatile boolean connected;
    private volatile String lastErrorMessage;

    public boolean connect(RemoteProfile profile) {
        String validationError = validateProfile(profile);
        if (validationError != null) {
            connected = false;
            lastErrorMessage = validationError;
            return false;
        }

        String attemptError = attemptConnect(profile);
        connected = attemptError == null;
        lastErrorMessage = attemptError;
        return connected;
    }

    public void disconnect() {
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    protected String attemptConnect(RemoteProfile profile) {
        try {
            var session = SshSessionFactory.createConnectedSession(profile);
            session.disconnect();
            return null;
        } catch (Exception exception) {
            return translateException(exception);
        }
    }

    private String validateProfile(RemoteProfile profile) {
        if (profile == null) {
            return "Connection profile is missing.";
        }
        if (isBlank(profile.getHost())) {
            return "Host is required.";
        }
        if (isBlank(profile.getUsername())) {
            return "Username is required.";
        }

        String authType = profile.getAuthType();
        if ("Public Key".equalsIgnoreCase(authType)) {
            if (isBlank(profile.getPublicKeyFilePath())) {
                return "Public key file is required.";
            }
            return null;
        }
        if (isBlank(profile.getPassword())) {
            return "Password is required.";
        }
        return null;
    }

    private String translateException(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "Connection failed.";
        }

        String lower = message.toLowerCase();
        if (lower.contains("auth fail") || lower.contains("authentication") || lower.contains("permission denied")) {
            return "Authentication failed.";
        }
        if (lower.contains("unknownhost") || lower.contains("unknown host") || lower.contains("name or service not known")) {
            return "Host not found.";
        }
        if (lower.contains("refused") || lower.contains("timed out") || lower.contains("timeout") || lower.contains("connection reset")) {
            return "Unable to reach SSH server.";
        }
        return "Connection failed: " + message;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
