package io.worldportal.app.service.impl;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import io.worldportal.app.model.RemoteProfile;
import io.worldportal.app.model.WorldEntry;
import io.worldportal.app.service.WorldService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StubWorldService implements WorldService {
    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("\"DisplayName\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern GAME_MODE_PATTERN = Pattern.compile("\"GameMode\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATCH_LINE_PATTERN = Pattern.compile("\"CreatedWithPatchline\"\\s*:\\s*\"([^\"]+)\"");

    @Override
    public List<WorldEntry> listLocalWorlds(String localWorldsPath) {
        if (localWorldsPath == null || localWorldsPath.isBlank()) {
            return Collections.emptyList();
        }

        Path worldsRoot = Paths.get(localWorldsPath);
        if (!Files.isDirectory(worldsRoot)) {
            return Collections.emptyList();
        }

        try (Stream<Path> worldDirs = Files.list(worldsRoot)) {
            return worldDirs
                    .filter(Files::isDirectory)
                    .map(this::toWorldEntry)
                    .filter(entry -> entry != null)
                    .sorted((left, right) -> right.getLastModified().compareTo(left.getLastModified()))
                    .collect(Collectors.toList());
        } catch (IOException exception) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<WorldEntry> listRemoteWorlds(RemoteProfile profile) {
        if (profile == null || profile.getHost() == null || profile.getHost().isBlank()) {
            return Collections.emptyList();
        }

        String remoteBasePath = profile.getRemoteBasePath();
        if (remoteBasePath == null || remoteBasePath.isBlank()) {
            return Collections.emptyList();
        }

        Session session = null;
        ChannelSftp channel = null;
        try {
            session = SshSessionFactory.createConnectedSession(profile);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(15000);

            List<WorldEntry> result = new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<ChannelSftp.LsEntry> entries = channel.ls(remoteBasePath);
            for (ChannelSftp.LsEntry entry : entries) {
                if (!entry.getAttrs().isDir()) {
                    continue;
                }
                String name = entry.getFilename();
                if (".".equals(name) || "..".equals(name)) {
                    continue;
                }

                String worldPath = remoteBasePath + "/" + name;
                String configPath = worldPath + "/universe/worlds/default/config.json";
                String metadataPath = worldPath + "/client_metadata.json";
                String displayName = readRemoteValue(channel, configPath, DISPLAY_NAME_PATTERN, name);
                String gameMode = readRemoteValue(channel, configPath, GAME_MODE_PATTERN, "Unknown");
                String patchLine = readRemoteValue(channel, metadataPath, PATCH_LINE_PATTERN, "Unknown");

                Instant lastPlayed;
                try {
                    SftpATTRS attrs = channel.stat(configPath);
                    lastPlayed = Instant.ofEpochSecond(attrs.getMTime());
                } catch (Exception ignored) {
                    lastPlayed = Instant.ofEpochSecond(entry.getAttrs().getMTime());
                }

                result.add(new WorldEntry(
                        name,
                        displayName,
                        worldPath,
                        null,
                        gameMode,
                        patchLine,
                        lastPlayed
                ));
            }

            result.sort((left, right) -> right.getLastModified().compareTo(left.getLastModified()));
            return result;
        } catch (Exception exception) {
            return Collections.emptyList();
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private WorldEntry toWorldEntry(Path worldDirectory) {
        Path worldConfig = worldDirectory.resolve("universe/worlds/default/config.json");
        if (!Files.exists(worldConfig)) {
            return null;
        }

        String name = readDisplayName(worldConfig);
        String gameMode = readValue(worldConfig, GAME_MODE_PATTERN, "Unknown");

        Path clientMetadata = worldDirectory.resolve("client_metadata.json");
        String patchLine = readValue(clientMetadata, PATCH_LINE_PATTERN, "Unknown");

        String previewImagePath = null;
        Path previewFile = worldDirectory.resolve("preview.png");
        if (Files.exists(previewFile)) {
            previewImagePath = previewFile.toString();
        }

        Instant lastPlayed = readLastPlayed(worldConfig);
        return new WorldEntry(
                worldDirectory.getFileName().toString(),
                name,
                worldDirectory.toString(),
                previewImagePath,
                gameMode,
                patchLine,
                lastPlayed
        );
    }

    private String readDisplayName(Path worldConfig) {
        String value = readValue(worldConfig, DISPLAY_NAME_PATTERN, null);
        if (value != null) {
            return value;
        }
        return worldConfig.getParent().getParent().getParent().getFileName().toString();
    }

    private Instant readLastPlayed(Path worldConfig) {
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(worldConfig);
            return lastModifiedTime.toInstant();
        } catch (IOException exception) {
            return Instant.EPOCH;
        }
    }

    private String readValue(Path file, Pattern pattern, String fallback) {
        if (file == null || !Files.exists(file)) {
            return fallback;
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IOException ignored) {
        }
        return fallback;
    }

    private String readRemoteValue(ChannelSftp channel, String remoteFile, Pattern pattern, String fallback) {
        try (var inputStream = channel.get(remoteFile)) {
            String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
