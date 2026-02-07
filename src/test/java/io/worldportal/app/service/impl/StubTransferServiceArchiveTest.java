package io.worldportal.app.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StubTransferServiceArchiveTest {

    @TempDir
    Path tempDir;

    @Test
    void createWorldArchiveIncludesOnlyTransferEntries() throws IOException {
        Path worldDir = tempDir.resolve("MyWorld");
        Files.createDirectories(worldDir.resolve("mods"));
        Files.createDirectories(worldDir.resolve("universe/worlds/default"));
        Files.createDirectories(worldDir.resolve("logs"));
        Files.writeString(worldDir.resolve("bans.json"), "{}");
        Files.writeString(worldDir.resolve("client_metadata.json"), "{}");
        Files.writeString(worldDir.resolve("config.json"), "{}");
        Files.writeString(worldDir.resolve("permissions.json"), "{}");
        Files.writeString(worldDir.resolve("preview.png"), "PNG");
        Files.writeString(worldDir.resolve("whitelist.json"), "[]");
        Files.writeString(worldDir.resolve("mods/mod-a.txt"), "mod");
        Files.writeString(worldDir.resolve("universe/worlds/default/config.json"), "{}");
        Files.writeString(worldDir.resolve("logs/ignored.log"), "ignore");
        Files.writeString(worldDir.resolve("readme.txt"), "ignore");

        StubTransferService service = new StubTransferService();

        Path archive = service.createWorldArchive(worldDir);

        assertNotNull(archive);
        assertTrue(Files.exists(archive));
        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            assertNotNull(zipFile.getEntry("bans.json"));
            assertNotNull(zipFile.getEntry("client_metadata.json"));
            assertNotNull(zipFile.getEntry("config.json"));
            assertNotNull(zipFile.getEntry("permissions.json"));
            assertNotNull(zipFile.getEntry("preview.png"));
            assertNotNull(zipFile.getEntry("whitelist.json"));
            assertNotNull(zipFile.getEntry("mods/mod-a.txt"));
            assertNotNull(zipFile.getEntry("universe/worlds/default/config.json"));
            assertTrue(zipFile.getEntry("logs/ignored.log") == null);
            assertTrue(zipFile.getEntry("readme.txt") == null);
        }
    }

    @Test
    void extractWorldArchiveRestoresDirectoryStructure() throws IOException {
        Path worldDir = tempDir.resolve("MyWorld");
        Files.createDirectories(worldDir.resolve("mods"));
        Files.writeString(worldDir.resolve("config.json"), "{}");
        Files.writeString(worldDir.resolve("mods/mod-a.txt"), "mod");

        StubTransferService service = new StubTransferService();
        Path archive = service.createWorldArchive(worldDir);
        Path extractTarget = tempDir.resolve("Extracted");

        service.extractWorldArchive(archive, extractTarget);

        assertTrue(Files.exists(extractTarget.resolve("config.json")));
        assertTrue(Files.exists(extractTarget.resolve("mods/mod-a.txt")));
        assertFalse(Files.exists(extractTarget.resolve("logs")));
    }

    @Test
    void resolveUniqueNameAddsNumericSuffixWhenNameAlreadyExists() {
        String unique = StubTransferService.resolveUniqueName(
                "MyWorld",
                Set.of("MyWorld", "MyWorld_1", "Other")::contains);

        assertTrue("MyWorld_2".equals(unique));
    }

    @Test
    void resolveUniqueNameReturnsOriginalWhenNotTaken() {
        String unique = StubTransferService.resolveUniqueName(
                "FreshWorld",
                Set.of("MyWorld", "MyWorld_1")::contains);

        assertTrue("FreshWorld".equals(unique));
    }

    @Test
    void assertContainsFilesThrowsForEmptyWorldDirectory() throws IOException {
        Path emptyWorld = tempDir.resolve("EmptyWorld");
        Files.createDirectories(emptyWorld);

        StubTransferService service = new StubTransferService();

        assertThrows(IOException.class, () -> service.assertContainsFiles(emptyWorld));
    }
}
