package io.worldportal.app.service.impl;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.Session;
import io.worldportal.app.model.RemoteProfile;
import io.worldportal.app.model.WorldEntry;
import io.worldportal.app.service.TransferService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class StubTransferService implements TransferService {
    @Override
    public void uploadWorld(WorldEntry world, RemoteProfile profile) {
        if (world == null || profile == null || world.getPath() == null || world.getPath().isBlank()) {
            return;
        }

        Path localWorldPath = Paths.get(world.getPath());
        if (!Files.isDirectory(localWorldPath)) {
            return;
        }

        String remoteBase = profile.getRemoteBasePath();
        if (remoteBase == null || remoteBase.isBlank()) {
            return;
        }

        String remoteWorldPath = remoteBase + "/" + localWorldPath.getFileName();
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = SshSessionFactory.createConnectedSession(profile);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(15000);

            ensureRemoteDirectories(channel, remoteWorldPath);
            uploadDirectory(channel, localWorldPath, remoteWorldPath);
        } catch (Exception ignored) {
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    @Override
    public void downloadWorld(WorldEntry world, RemoteProfile profile) {
        if (world == null || profile == null || world.getPath() == null || world.getPath().isBlank()) {
            return;
        }

        String localWorldsPath = profile.getLocalWorldsPath();
        if (localWorldsPath == null || localWorldsPath.isBlank()) {
            return;
        }

        Path localTargetRoot = Paths.get(localWorldsPath);
        Path localTargetWorld = localTargetRoot.resolve(world.getId() != null ? world.getId() : "DownloadedWorld");

        Session session = null;
        ChannelSftp channel = null;
        try {
            Files.createDirectories(localTargetRoot);
            session = SshSessionFactory.createConnectedSession(profile);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(15000);

            downloadDirectory(channel, world.getPath(), localTargetWorld);
        } catch (Exception ignored) {
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private void uploadDirectory(ChannelSftp channel, Path localDirectory, String remoteDirectory) throws Exception {
        ensureRemoteDirectories(channel, remoteDirectory);
        try (var paths = Files.list(localDirectory)) {
            List<Path> children = paths.toList();
            for (Path child : children) {
                String remoteChild = remoteDirectory + "/" + child.getFileName();
                if (Files.isDirectory(child)) {
                    uploadDirectory(channel, child, remoteChild);
                } else if (Files.isRegularFile(child)) {
                    channel.put(child.toString(), remoteChild);
                }
            }
        }
    }

    private void downloadDirectory(ChannelSftp channel, String remoteDirectory, Path localDirectory) throws Exception {
        Files.createDirectories(localDirectory);

        @SuppressWarnings("unchecked")
        List<ChannelSftp.LsEntry> entries = channel.ls(remoteDirectory);
        for (ChannelSftp.LsEntry entry : entries) {
            String name = entry.getFilename();
            if (".".equals(name) || "..".equals(name)) {
                continue;
            }
            String remoteChild = remoteDirectory + "/" + name;
            Path localChild = localDirectory.resolve(name);
            SftpATTRS attrs = entry.getAttrs();
            if (attrs.isDir()) {
                downloadDirectory(channel, remoteChild, localChild);
            } else {
                Files.createDirectories(localChild.getParent());
                channel.get(remoteChild, localChild.toString());
            }
        }
    }

    private void ensureRemoteDirectories(ChannelSftp channel, String remoteDirectory) throws Exception {
        String normalized = remoteDirectory.replace("\\", "/");
        String[] segments = normalized.split("/");
        StringBuilder current = new StringBuilder();
        if (normalized.startsWith("/")) {
            current.append("/");
        }
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            if (current.length() > 1 && current.charAt(current.length() - 1) != '/') {
                current.append('/');
            } else if (current.length() == 1 && current.charAt(0) != '/') {
                current.append('/');
            }
            current.append(segment);
            String currentPath = current.toString();
            try {
                channel.stat(currentPath);
            } catch (Exception exception) {
                channel.mkdir(currentPath);
            }
        }
    }
}
