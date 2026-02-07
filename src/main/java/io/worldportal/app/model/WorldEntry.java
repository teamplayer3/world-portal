package io.worldportal.app.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorldEntry {
    private String id;
    private String name;
    private String path;
    private String previewImagePath;
    private String gameMode;
    private String patchLine;
    private String uuidBinary;
    private String gameTimeIso;
    private final List<WorldEntry> sameWorldReferences = new ArrayList<>();
    private Instant lastModified;

    public WorldEntry() {
    }

    public WorldEntry(
            String id,
            String name,
            String path,
            String previewImagePath,
            String gameMode,
            String patchLine,
            Instant lastModified
    ) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.previewImagePath = previewImagePath;
        this.gameMode = gameMode;
        this.patchLine = patchLine;
        this.lastModified = lastModified;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    public String getPreviewImagePath() {
        return previewImagePath;
    }

    public void setPreviewImagePath(String previewImagePath) {
        this.previewImagePath = previewImagePath;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public String getPatchLine() {
        return patchLine;
    }

    public void setPatchLine(String patchLine) {
        this.patchLine = patchLine;
    }

    public String getUuidBinary() {
        return uuidBinary;
    }

    public void setUuidBinary(String uuidBinary) {
        this.uuidBinary = uuidBinary;
    }

    public String getGameTimeIso() {
        return gameTimeIso;
    }

    public void setGameTimeIso(String gameTimeIso) {
        this.gameTimeIso = gameTimeIso;
    }

    public List<WorldEntry> getSameWorldReferences() {
        return Collections.unmodifiableList(sameWorldReferences);
    }

    public void clearSameWorldReferences() {
        sameWorldReferences.clear();
    }

    public void addSameWorldReference(WorldEntry otherWorld) {
        if (otherWorld == null || otherWorld == this) {
            return;
        }
        if (!sameWorldReferences.contains(otherWorld)) {
            sameWorldReferences.add(otherWorld);
        }
    }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
