package io.worldportal.app.config;

import io.worldportal.app.model.RemoteProfile;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConnectionSettingsStoreTest {

    @Test
    void resolvesWindowsDefaultSettingsPath() {
        Path resolved = ConnectionSettingsStore.resolveSettingsFilePath(
                "Windows 11",
                "C:\\Users\\alice",
                "C:\\Users\\alice\\AppData\\Roaming",
                null
        );

        assertEquals(
                Paths.get("C:\\Users\\alice\\AppData\\Roaming", "world-portal", "connection.properties"),
                resolved
        );
    }

    @Test
    void resolvesLinuxDefaultSettingsPathFromXdgConfigHome() {
        Path resolved = ConnectionSettingsStore.resolveSettingsFilePath(
                "Linux",
                "/home/alice",
                null,
                "/home/alice/.config"
        );

        assertEquals(
                Paths.get("/home/alice/.config", "world-portal", "connection.properties"),
                resolved
        );
    }

    @Test
    void loadReturnsNullWhenNoCacheExists() throws Exception {
        Path tempDir = Files.createTempDirectory("world-portal-store-test");
        ConnectionSettingsStore store = new ConnectionSettingsStore(tempDir.resolve("connection.properties"));

        RemoteProfile loaded = store.load();

        assertNull(loaded);
    }

    @Test
    void saveAndLoadRoundTripWorks() throws Exception {
        Path tempDir = Files.createTempDirectory("world-portal-store-test");
        Path file = tempDir.resolve("connection.properties");
        ConnectionSettingsStore store = new ConnectionSettingsStore(file);

        RemoteProfile profile = new RemoteProfile(
                "example.com",
                2222,
                "player",
                "/srv/worlds",
                "Public Key",
                "secret",
                "/home/user/.ssh/id_ed25519.pub",
                "/home/user/.local/share/Hytale/UserData/Saves"
        );

        store.save(profile);
        RemoteProfile loaded = store.load();

        assertNotNull(loaded);
        assertEquals("example.com", loaded.getHost());
        assertEquals(2222, loaded.getPort());
        assertEquals("player", loaded.getUsername());
        assertEquals("/srv/worlds", loaded.getRemoteBasePath());
        assertEquals("Public Key", loaded.getAuthType());
        assertEquals("", loaded.getPassword());
        assertEquals("/home/user/.ssh/id_ed25519.pub", loaded.getPublicKeyFilePath());
        assertEquals("/home/user/.local/share/Hytale/UserData/Saves", loaded.getLocalWorldsPath());
    }

    @Test
    void doesNotPersistPublicKeyWhenAuthTypeIsPassword() throws Exception {
        Path tempDir = Files.createTempDirectory("world-portal-store-test");
        Path file = tempDir.resolve("connection.properties");
        ConnectionSettingsStore store = new ConnectionSettingsStore(file);

        RemoteProfile profile = new RemoteProfile(
                "example.com",
                22,
                "player",
                "/srv/worlds",
                "Password",
                "",
                "/home/user/.ssh/id_ed25519.pub",
                "/home/user/.local/share/Hytale/UserData/Saves"
        );

        store.save(profile);

        String content = Files.readString(file);
        assertFalse(content.contains("publicKeyFile="));
    }
}
