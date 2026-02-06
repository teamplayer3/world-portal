package io.worldportal.app.service;

import io.worldportal.app.model.RemoteProfile;
import io.worldportal.app.model.WorldEntry;

public interface TransferService {
    void uploadWorld(WorldEntry world, RemoteProfile profile);

    void downloadWorld(WorldEntry world, RemoteProfile profile);
}
