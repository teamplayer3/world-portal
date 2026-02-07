package io.worldportal.app.service.impl;

import io.worldportal.app.model.RemoteProfile;
import io.worldportal.app.model.WorldEntry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StubWorldServicePreviewTest {

    @Test
    void downloadRemotePreviewReturnsNullForInvalidInput() {
        StubWorldService service = new StubWorldService();

        assertNull(service.downloadRemotePreview(null, null));
        assertNull(service.downloadRemotePreview(new WorldEntry(), null));
    }

    @Test
    void downloadRemotePreviewReturnsCachedFileWhenPresent() throws IOException {
        StubWorldService service = new StubWorldService();
        RemoteProfile profile = new RemoteProfile();
        profile.setHost("example.com");

        WorldEntry world = new WorldEntry();
        world.setId("MyRemoteWorld");
        world.setPath("/remote/worlds/MyRemoteWorld");

        Path cacheFile = service.previewCacheFile(profile, world);
        Files.createDirectories(cacheFile.getParent());
        Files.writeString(cacheFile, "preview");

        try {
            String result = service.downloadRemotePreview(world, profile);
            assertEquals(cacheFile.toString(), result);
        } finally {
            Files.deleteIfExists(cacheFile);
        }
    }
}
