package io.worldportal.app.model;

import java.time.Instant;

public class WorldEntry {
    private String id;
    private String name;
    private String path;
    private String previewImagePath;
    private String gameMode;
    private String patchLine;
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

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
