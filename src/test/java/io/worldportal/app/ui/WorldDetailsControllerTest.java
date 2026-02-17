package io.worldportal.app.ui;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldDetailsControllerTest {

    @Test
    void deleteConfirmationRequiresExactWorldName() {
        assertTrue(WorldDetailsController.matchesDeleteConfirmation("SkyHold", "SkyHold"));
        assertTrue(WorldDetailsController.matchesDeleteConfirmation("SkyHold", "  SkyHold  "));
        assertFalse(WorldDetailsController.matchesDeleteConfirmation("SkyHold", "skyhold"));
        assertFalse(WorldDetailsController.matchesDeleteConfirmation("SkyHold", "SkyHold1"));
    }

    @Test
    void deleteWorldDirectoryRemovesNestedFilesAndFolders() throws Exception {
        Path worldDir = Files.createTempDirectory("world-delete-test");
        Path nested = Files.createDirectories(worldDir.resolve("universe/worlds/default"));
        Files.writeString(nested.resolve("config.json"), "{}");
        Files.writeString(worldDir.resolve("client_metadata.json"), "{}");

        WorldDetailsController.deleteWorldDirectory(worldDir);

        assertFalse(Files.exists(worldDir));
    }

    @Test
    void folderRenameValidationRejectsExistingSiblingFolder() throws Exception {
        Path savesRoot = Files.createTempDirectory("world-rename-validation");
        Path currentWorldFolder = Files.createDirectories(savesRoot.resolve("SkyHold"));
        Files.createDirectories(savesRoot.resolve("AlreadyExists"));

        String validationError = WorldDetailsController.validateFolderRename(currentWorldFolder, "AlreadyExists");
        assertEquals("A world folder with this name already exists.", validationError);
    }

    @Test
    void worldDetailsChangeDetectionTracksNameAndFolderInputs() {
        assertFalse(WorldDetailsController.hasWorldIdentityChanges("SkyHold", "sky-hold", "SkyHold", "sky-hold"));
        assertTrue(WorldDetailsController.hasWorldIdentityChanges("SkyHold", "sky-hold", "Sky Keep", "sky-hold"));
        assertTrue(WorldDetailsController.hasWorldIdentityChanges("SkyHold", "sky-hold", "SkyHold", "sky-keep"));
    }

    @Test
    void updateWorldDisplayNameRewritesConfigDisplayName() throws Exception {
        Path worldDir = Files.createTempDirectory("world-rename-config");
        Path configDir = Files.createDirectories(worldDir.resolve("universe/worlds/default"));
        Path configFile = configDir.resolve("config.json");
        Files.writeString(configFile, "{ \"DisplayName\": \"Old Name\" }", StandardCharsets.UTF_8);

        WorldDetailsController.updateWorldDisplayName(worldDir, "New Name");

        String updatedConfig = Files.readString(configFile, StandardCharsets.UTF_8);
        assertTrue(updatedConfig.contains("\"DisplayName\": \"New Name\""));
    }

    @Test
    void whitelistChangeDetectionTracksEnabledFlagAndPlayerList() {
        assertFalse(WorldDetailsController.hasWhitelistChanges(true, List.of("a", "b"), true, List.of("a", "b")));
        assertTrue(WorldDetailsController.hasWhitelistChanges(true, List.of("a", "b"), false, List.of("a", "b")));
        assertTrue(WorldDetailsController.hasWhitelistChanges(true, List.of("a", "b"), true, List.of("a", "b", "c")));
    }
}
