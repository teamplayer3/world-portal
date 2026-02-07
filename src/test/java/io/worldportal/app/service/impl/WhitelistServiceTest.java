package io.worldportal.app.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhitelistServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void loadReturnsDefaultsWhenWhitelistFileIsMissing() throws IOException {
        WhitelistService service = new WhitelistService();
        Path worldDir = tempDir.resolve("MyWorld");
        Files.createDirectories(worldDir);

        WhitelistService.WhitelistConfig config = service.load(worldDir);

        assertTrue(config.enabled());
        assertTrue(config.playerUuids().isEmpty());
    }

    @Test
    void saveAndLoadRoundTripKeepsEnabledAndPlayers() throws IOException {
        WhitelistService service = new WhitelistService();
        Path worldDir = tempDir.resolve("MyWorld");
        Files.createDirectories(worldDir);

        WhitelistService.WhitelistConfig saved = new WhitelistService.WhitelistConfig(
                false,
                List.of("123e4567-e89b-12d3-a456-426614174000", "00000000-0000-0000-0000-000000000001"));

        service.save(worldDir, saved);
        WhitelistService.WhitelistConfig loaded = service.load(worldDir);

        assertFalse(loaded.enabled());
        assertEquals(saved.playerUuids(), loaded.playerUuids());
    }

    @Test
    void loadUnderstandsLegacyArrayFormat() throws IOException {
        WhitelistService service = new WhitelistService();
        Path worldDir = tempDir.resolve("LegacyWorld");
        Files.createDirectories(worldDir);
        Files.writeString(worldDir.resolve("whitelist.json"), "[\"11111111-1111-1111-1111-111111111111\",\"2222\"]");

        WhitelistService.WhitelistConfig loaded = service.load(worldDir);

        assertTrue(loaded.enabled());
        assertEquals(List.of("11111111-1111-1111-1111-111111111111", "2222"), loaded.playerUuids());
    }

    @Test
    void loadReadsPlayersFromListField() throws IOException {
        WhitelistService service = new WhitelistService();
        Path worldDir = tempDir.resolve("JsonWorld");
        Files.createDirectories(worldDir);
        Files.writeString(
                worldDir.resolve("whitelist.json"),
                "{\"enabled\":false,\"list\":[\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\"bbbb\"]}");

        WhitelistService.WhitelistConfig loaded = service.load(worldDir);

        assertFalse(loaded.enabled());
        assertEquals(List.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "bbbb"), loaded.playerUuids());
    }
}
