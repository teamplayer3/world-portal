package io.worldportal.app.service;

import io.worldportal.app.model.RemoteProfile;
import io.worldportal.app.model.WorldEntry;

public interface TransferService {
    void uploadWorld(WorldEntry world, RemoteProfile profile);

    void downloadWorld(WorldEntry world, RemoteProfile profile);

    default void syncRemoteToLocalWorld(WorldEntry remoteWorld, WorldEntry localWorld, RemoteProfile profile) {
        downloadWorld(remoteWorld, profile);
    }

    default void syncLocalToRemoteWorld(WorldEntry localWorld, WorldEntry remoteWorld, RemoteProfile profile) {
        uploadWorld(localWorld, profile);
    }

    default void renameRemoteWorld(WorldEntry remoteWorld, String requestedFolderName, RemoteProfile profile) {
        throw new UnsupportedOperationException("Remote rename is not supported.");
    }

    default void deleteRemoteWorld(WorldEntry remoteWorld, RemoteProfile profile) {
        throw new UnsupportedOperationException("Remote delete is not supported.");
    }
}
