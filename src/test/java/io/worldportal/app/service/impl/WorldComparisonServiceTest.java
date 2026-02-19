package io.worldportal.app.service.impl;

import io.worldportal.app.model.WorldEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldComparisonServiceTest {

    @Test
    void marksMatchingLocalAndRemoteWorldsByUuidAndGameTime() {
        WorldEntry local = new WorldEntry();
        local.setName("LocalTest");
        local.setUuidBinary("abc123");
        local.setGameTimeIso("2026-02-07T12:00:00Z");

        WorldEntry remote = new WorldEntry();
        remote.setName("RemoteProd");
        remote.setUuidBinary("abc123");
        remote.setGameTimeIso("2026-02-07T12:00:00Z");

        new WorldComparisonService().annotateMatches(List.of(local), List.of(remote));

        assertEquals(1, local.getSameWorldReferences().size());
        assertEquals(1, remote.getSameWorldReferences().size());
        assertTrue(local.getSameWorldReferences().contains(remote));
        assertTrue(remote.getSameWorldReferences().contains(local));
    }

    @Test
    void supportsMultipleSameWorldMatches() {
        WorldEntry local = world("LocalTest", "abc123", "2026-02-07T12:00:00Z");
        WorldEntry remoteOne = world("RemoteA", "abc123", "2026-02-07T12:00:00Z");
        WorldEntry remoteTwo = world("RemoteB", "abc123", "2026-02-07T12:00:00Z");

        new WorldComparisonService().annotateMatches(List.of(local), List.of(remoteOne, remoteTwo));

        assertEquals(2, local.getSameWorldReferences().size());
        assertTrue(local.getSameWorldReferences().contains(remoteOne));
        assertTrue(local.getSameWorldReferences().contains(remoteTwo));
    }

    @Test
    void marksSameWorldWhenUuidMatchesEvenIfGameTimeDiffers() {
        WorldEntry local = world("LocalTest", "abc123", "2026-02-07T12:00:00Z");
        WorldEntry remote = world("RemoteProd", "abc123", "2026-02-07T13:00:00Z");

        new WorldComparisonService().annotateMatches(List.of(local), List.of(remote));

        assertEquals(1, local.getSameWorldReferences().size());
        assertEquals(1, remote.getSameWorldReferences().size());
        assertTrue(local.getSameWorldReferences().contains(remote));
        assertTrue(remote.getSameWorldReferences().contains(local));
    }

    private WorldEntry world(String name, String uuid, String gameTime) {
        WorldEntry world = new WorldEntry();
        world.setName(name);
        world.setUuidBinary(uuid);
        world.setGameTimeIso(gameTime);
        return world;
    }
}
