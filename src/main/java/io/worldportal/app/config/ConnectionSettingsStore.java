package io.worldportal.app.config;

import io.worldportal.app.model.RemoteProfile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ConnectionSettingsStore {
    private static final String KEY_HOST = "host";
    private static final String KEY_PORT = "port";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_REMOTE_PATH = "remotePath";
    private static final String KEY_AUTH_TYPE = "authType";
    private static final String KEY_PUBLIC_KEY = "publicKeyFile";
    private static final String KEY_LOCAL_WORLDS_PATH = "localWorldsPath";

    private final Path settingsFile;

    public ConnectionSettingsStore() {
        this(resolveSettingsFilePath(
                System.getProperty("os.name", ""),
                System.getProperty("user.home", "."),
                System.getenv("APPDATA"),
                System.getenv("XDG_CONFIG_HOME")
        ));
    }

    ConnectionSettingsStore(Path settingsFile) {
        this.settingsFile = settingsFile;
    }

    public void save(RemoteProfile profile) {
        if (profile == null) {
            return;
        }

        Properties properties = new Properties();
        properties.setProperty(KEY_HOST, valueOrEmpty(profile.getHost()));
        properties.setProperty(KEY_PORT, Integer.toString(profile.getPort()));
        properties.setProperty(KEY_USERNAME, valueOrEmpty(profile.getUsername()));
        properties.setProperty(KEY_REMOTE_PATH, valueOrEmpty(profile.getRemoteBasePath()));
        properties.setProperty(KEY_AUTH_TYPE, valueOrEmpty(profile.getAuthType()));
        if ("Public Key".equalsIgnoreCase(profile.getAuthType())) {
            properties.setProperty(KEY_PUBLIC_KEY, valueOrEmpty(profile.getPublicKeyFilePath()));
        }
        properties.setProperty(KEY_LOCAL_WORLDS_PATH, valueOrEmpty(profile.getLocalWorldsPath()));

        try {
            if (settingsFile.getParent() != null) {
                Files.createDirectories(settingsFile.getParent());
            }
            try (OutputStream outputStream = Files.newOutputStream(settingsFile)) {
                properties.store(outputStream, "world-portal last successful connection");
            }
        } catch (IOException ignored) {
        }
    }

    public RemoteProfile load() {
        if (!Files.exists(settingsFile)) {
            return null;
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(settingsFile)) {
            properties.load(inputStream);
        } catch (IOException exception) {
            return null;
        }

        int parsedPort;
        try {
            parsedPort = Integer.parseInt(properties.getProperty(KEY_PORT, "22"));
        } catch (NumberFormatException exception) {
            parsedPort = 22;
        }

        return new RemoteProfile(
                properties.getProperty(KEY_HOST, ""),
                parsedPort,
                properties.getProperty(KEY_USERNAME, ""),
                properties.getProperty(KEY_REMOTE_PATH, ""),
                properties.getProperty(KEY_AUTH_TYPE, "Password"),
                "",
                properties.getProperty(KEY_PUBLIC_KEY, ""),
                properties.getProperty(KEY_LOCAL_WORLDS_PATH, "")
        );
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    static Path resolveSettingsFilePath(String osName, String userHome, String appData, String xdgConfigHome) {
        String normalizedOs = osName == null ? "" : osName.toLowerCase();
        String normalizedHome = userHome == null || userHome.isBlank() ? "." : userHome;

        if (normalizedOs.contains("win")) {
            if (appData != null && !appData.isBlank()) {
                return Paths.get(appData, "world-portal", "connection.properties");
            }
            return Paths.get(normalizedHome, "AppData", "Roaming", "world-portal", "connection.properties");
        }

        if (xdgConfigHome != null && !xdgConfigHome.isBlank()) {
            return Paths.get(xdgConfigHome, "world-portal", "connection.properties");
        }
        return Paths.get(normalizedHome, ".config", "world-portal", "connection.properties");
    }
}
