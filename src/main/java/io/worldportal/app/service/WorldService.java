package io.worldportal.app.service;

import io.worldportal.app.model.RemoteProfile;
import io.worldportal.app.model.WorldEntry;

import java.util.List;

public interface WorldService {
    List<WorldEntry> listLocalWorlds(String localWorldsPath);

    List<WorldEntry> listRemoteWorlds(RemoteProfile profile);
}
