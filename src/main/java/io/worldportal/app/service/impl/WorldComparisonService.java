package io.worldportal.app.service.impl;

import io.worldportal.app.model.WorldEntry;

import java.util.List;

public class WorldComparisonService {

    public void annotateMatches(List<WorldEntry> localWorlds, List<WorldEntry> remoteWorlds) {
        clearReferences(localWorlds);
        clearReferences(remoteWorlds);

        for (WorldEntry local : localWorlds) {
            for (WorldEntry remote : remoteWorlds) {
                if (isSameWorld(local, remote)) {
                    local.addSameWorldReference(remote);
                    remote.addSameWorldReference(local);
                }
            }
        }
    }

    private boolean isSameWorld(WorldEntry left, WorldEntry right) {
        if (left == null || right == null) {
            return false;
        }
        if (isBlank(left.getUuidBinary()) || isBlank(right.getUuidBinary())) {
            return false;
        }
        if (isBlank(left.getGameTimeIso()) || isBlank(right.getGameTimeIso())) {
            return false;
        }
        return left.getUuidBinary().equals(right.getUuidBinary())
                && left.getGameTimeIso().equals(right.getGameTimeIso());
    }

    private void clearReferences(List<WorldEntry> worlds) {
        if (worlds == null) {
            return;
        }
        for (WorldEntry world : worlds) {
            if (world != null) {
                world.clearSameWorldReferences();
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
