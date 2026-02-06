package io.worldportal.app.config;

public class AppConfig {
    private final String appName;
    private final String appVersion;

    public AppConfig() {
        this("world-portal", "0.1.0-SNAPSHOT");
    }

    public AppConfig(String appName, String appVersion) {
        this.appName = appName;
        this.appVersion = appVersion;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return appVersion;
    }
}
