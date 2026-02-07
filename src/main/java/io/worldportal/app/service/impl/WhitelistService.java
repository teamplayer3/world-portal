package io.worldportal.app.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WhitelistService {
    private static final Pattern ENABLED_PATTERN = Pattern.compile("\\\"enabled\\\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIST_ARRAY_PATTERN = Pattern.compile("\\\"list\\\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern PLAYERS_ARRAY_PATTERN = Pattern.compile("\\\"players\\\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern ARRAY_STRING_PATTERN = Pattern.compile("\\\"([^\\\"]+)\\\"");

    public WhitelistConfig load(Path worldDirectory) throws IOException {
        Path whitelistFile = whitelistFile(worldDirectory);
        if (!Files.exists(whitelistFile)) {
            return new WhitelistConfig(true, List.of());
        }

        String content = Files.readString(whitelistFile, StandardCharsets.UTF_8);
        return parse(content);
    }

    public void save(Path worldDirectory, WhitelistConfig config) throws IOException {
        Files.createDirectories(worldDirectory);
        Path whitelistFile = whitelistFile(worldDirectory);

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"enabled\": ").append(config.enabled()).append(",\n");
        json.append("  \"list\": [");

        for (int index = 0; index < config.playerUuids().size(); index++) {
            if (index > 0) {
                json.append(", ");
            }
            json.append("\"").append(escape(config.playerUuids().get(index))).append("\"");
        }
        json.append("]\n");
        json.append("}\n");

        Files.writeString(whitelistFile, json.toString(), StandardCharsets.UTF_8);
    }

    private WhitelistConfig parse(String content) {
        Matcher enabledMatcher = ENABLED_PATTERN.matcher(content);
        boolean enabled = true;
        if (enabledMatcher.find()) {
            enabled = Boolean.parseBoolean(enabledMatcher.group(1));
        }

        String playersText = extractPlayersText(content);

        Matcher uuidMatcher = ARRAY_STRING_PATTERN.matcher(playersText);
        Set<String> players = new LinkedHashSet<>();
        while (uuidMatcher.find()) {
            String uuid = uuidMatcher.group(1);
            if (uuid != null && !uuid.isBlank()) {
                players.add(uuid.trim());
            }
        }

        return new WhitelistConfig(enabled, new ArrayList<>(players));
    }

    private String extractPlayersText(String content) {
        Matcher listMatcher = LIST_ARRAY_PATTERN.matcher(content);
        if (listMatcher.find()) {
            return listMatcher.group(1);
        }

        Matcher playersMatcher = PLAYERS_ARRAY_PATTERN.matcher(content);
        if (playersMatcher.find()) {
            return playersMatcher.group(1);
        }
        return content;
    }

    private Path whitelistFile(Path worldDirectory) {
        return worldDirectory.resolve("whitelist.json");
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record WhitelistConfig(boolean enabled, List<String> playerUuids) {
        public WhitelistConfig {
            playerUuids = List.copyOf(playerUuids == null ? List.of() : playerUuids);
        }
    }
}
