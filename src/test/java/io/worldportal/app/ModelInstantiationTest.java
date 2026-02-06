package io.worldportal.app;

import io.worldportal.app.model.RemoteProfile;
import io.worldportal.app.model.WorldEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelInstantiationTest {

    @Test
    void worldEntryGettersAndSettersWork() {
        Instant now = Instant.now();
        WorldEntry worldEntry = new WorldEntry();
        worldEntry.setId("w-1");
        worldEntry.setName("Creative World");
        worldEntry.setPath("/home/user/worlds/creative");
        worldEntry.setPreviewImagePath("/home/user/worlds/creative/preview.png");
        worldEntry.setGameMode("Adventure");
        worldEntry.setPatchLine("pre-release");
        worldEntry.setLastModified(now);

        assertEquals("w-1", worldEntry.getId());
        assertEquals("Creative World", worldEntry.getName());
        assertEquals("/home/user/worlds/creative", worldEntry.getPath());
        assertEquals("/home/user/worlds/creative/preview.png", worldEntry.getPreviewImagePath());
        assertEquals("Adventure", worldEntry.getGameMode());
        assertEquals("pre-release", worldEntry.getPatchLine());
        assertEquals(now, worldEntry.getLastModified());
    }

    @Test
    void remoteProfileGettersAndSettersWork() {
        RemoteProfile profile = new RemoteProfile();
        profile.setHost("example.com");
        profile.setPort(22);
        profile.setUsername("player");
        profile.setRemoteBasePath("/srv/game/worlds");
        profile.setAuthType("Password");
        profile.setPassword("secret");
        profile.setPublicKeyFilePath("/home/user/.ssh/id_ed25519.pub");
        profile.setLocalWorldsPath("/home/user/.local/share/Hytale/UserData/Saves");

        assertEquals("example.com", profile.getHost());
        assertEquals(22, profile.getPort());
        assertEquals("player", profile.getUsername());
        assertEquals("/srv/game/worlds", profile.getRemoteBasePath());
        assertEquals("Password", profile.getAuthType());
        assertEquals("secret", profile.getPassword());
        assertEquals("/home/user/.ssh/id_ed25519.pub", profile.getPublicKeyFilePath());
        assertEquals("/home/user/.local/share/Hytale/UserData/Saves", profile.getLocalWorldsPath());
    }
}
