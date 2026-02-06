package io.worldportal.app.service.impl;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.worldportal.app.model.RemoteProfile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

final class SshSessionFactory {
    private SshSessionFactory() {
    }

    static Session createConnectedSession(RemoteProfile profile) throws JSchException {
        JSch jsch = new JSch();
        int port = profile.getPort() > 0 ? profile.getPort() : 22;
        String host = valueOrEmpty(profile.getHost());
        String username = valueOrEmpty(profile.getUsername());
        String authType = valueOrEmpty(profile.getAuthType());

        if ("Public Key".equalsIgnoreCase(authType)) {
            String keyPath = resolvePrivateKeyPath(profile.getPublicKeyFilePath());
            if (keyPath.isBlank()) {
                throw new JSchException("Public key auth selected but no key file provided.");
            }
            jsch.addIdentity(keyPath);
        }

        Session session = jsch.getSession(username, host, port);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setTimeout(15000);

        if (!"Public Key".equalsIgnoreCase(authType)) {
            String password = profile.getPassword();
            if (password != null && !password.isBlank()) {
                session.setPassword(password);
            }
        }

        session.connect(15000);
        return session;
    }

    private static String resolvePrivateKeyPath(String selectedPublicKeyFile) {
        String keyPath = valueOrEmpty(selectedPublicKeyFile);
        if (keyPath.endsWith(".pub")) {
            Path privateKey = Paths.get(keyPath.substring(0, keyPath.length() - 4));
            if (Files.exists(privateKey)) {
                return privateKey.toString();
            }
        }
        return keyPath;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
