package io.worldportal.app.model;

public class RemoteProfile {
    private String host;
    private int port;
    private String username;
    private String remoteBasePath;
    private String authType;
    private String password;
    private String publicKeyFilePath;
    private String localWorldsPath;

    public RemoteProfile() {
    }

    public RemoteProfile(String host, int port, String username, String remoteBasePath, String authType) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.remoteBasePath = remoteBasePath;
        this.authType = authType;
    }

    public RemoteProfile(
            String host,
            int port,
            String username,
            String remoteBasePath,
            String authType,
            String password,
            String publicKeyFilePath,
            String localWorldsPath
    ) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.remoteBasePath = remoteBasePath;
        this.authType = authType;
        this.password = password;
        this.publicKeyFilePath = publicKeyFilePath;
        this.localWorldsPath = localWorldsPath;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRemoteBasePath() {
        return remoteBasePath;
    }

    public void setRemoteBasePath(String remoteBasePath) {
        this.remoteBasePath = remoteBasePath;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPublicKeyFilePath() {
        return publicKeyFilePath;
    }

    public void setPublicKeyFilePath(String publicKeyFilePath) {
        this.publicKeyFilePath = publicKeyFilePath;
    }

    public String getLocalWorldsPath() {
        return localWorldsPath;
    }

    public void setLocalWorldsPath(String localWorldsPath) {
        this.localWorldsPath = localWorldsPath;
    }
}
