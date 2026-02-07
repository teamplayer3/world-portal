package io.worldportal.app.service.impl;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.Session;
import io.worldportal.app.model.RemoteProfile;
import io.worldportal.app.model.WorldEntry;
import io.worldportal.app.service.TransferService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Predicate;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class StubTransferService implements TransferService {
    private static final long REMOTE_COMMAND_TIMEOUT_MILLIS = 30_000L;
    private static final Set<String> INCLUDED_ROOT_FILES = Set.of(
            "bans.json",
            "client_metadata.json",
            "config.json",
            "permissions.json",
            "preview.png",
            "whitelist.json");
    private static final Set<String> INCLUDED_ROOT_DIRECTORIES = Set.of("mods", "universe");

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

        String worldDirName = localWorldPath.getFileName().toString();
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = SshSessionFactory.createConnectedSession(profile);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(15000);
            ChannelSftp sftpChannel = channel;

            ensureRemoteDirectories(channel, remoteBase);
            String uniqueWorldDirName = resolveUniqueName(
                    worldDirName,
                    name -> remoteExists(sftpChannel, remoteBase + "/" + name));
            String remoteWorldPath = remoteBase + "/" + uniqueWorldDirName;

            uploadIncludedEntries(channel, localWorldPath, remoteWorldPath);
        } catch (Exception failure) {
            throw new RuntimeException("Upload failed.", failure);
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
        String requestedName = world.getId() != null ? world.getId() : "DownloadedWorld";
        String uniqueLocalDirName = resolveUniqueName(requestedName,
                name -> Files.exists(localTargetRoot.resolve(name)));
        Path localTargetWorld = localTargetRoot.resolve(uniqueLocalDirName);

        Session session = null;
        ChannelSftp channel = null;
        try {
            Files.createDirectories(localTargetRoot);
            session = SshSessionFactory.createConnectedSession(profile);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(15000);

            downloadIncludedEntries(channel, world.getPath(), localTargetWorld);
            assertContainsFiles(localTargetWorld);
        } catch (Exception failure) {
            cleanupLocalWorldDirectory(localTargetWorld);
            throw new RuntimeException("Download failed.", failure);
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

    Path createWorldArchive(Path worldDirectory) throws IOException {
        Path archive = Files.createTempFile("world-portal-upload-", ".zip");
        try (OutputStream out = Files.newOutputStream(archive, StandardOpenOption.TRUNCATE_EXISTING);
                ZipOutputStream zipOutputStream = new ZipOutputStream(out)) {
            for (String fileName : INCLUDED_ROOT_FILES) {
                Path candidate = worldDirectory.resolve(fileName);
                if (Files.isRegularFile(candidate)) {
                    addFileToZip(worldDirectory, candidate, zipOutputStream);
                }
            }
            for (String directoryName : INCLUDED_ROOT_DIRECTORIES) {
                Path directory = worldDirectory.resolve(directoryName);
                if (Files.isDirectory(directory)) {
                    addDirectoryToZip(worldDirectory, directory, zipOutputStream);
                }
            }
        }
        return archive;
    }

    void extractWorldArchive(Path archivePath, Path targetDirectory) throws IOException {
        Files.createDirectories(targetDirectory);
        try (InputStream in = Files.newInputStream(archivePath);
                ZipInputStream zipInputStream = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path output = targetDirectory.resolve(entry.getName()).normalize();
                if (!output.startsWith(targetDirectory)) {
                    zipInputStream.closeEntry();
                    continue;
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                    zipInputStream.closeEntry();
                    continue;
                }

                Files.createDirectories(output.getParent());
                Files.copy(zipInputStream, output, StandardCopyOption.REPLACE_EXISTING);
                zipInputStream.closeEntry();
            }
        }
    }

    private void addDirectoryToZip(Path rootDirectory, Path directory, ZipOutputStream zipOutputStream)
            throws IOException {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.toList()) {
                if (Files.isDirectory(path)) {
                    continue;
                }
                addFileToZip(rootDirectory, path, zipOutputStream);
            }
        }
    }

    private void addFileToZip(Path rootDirectory, Path file, ZipOutputStream zipOutputStream) throws IOException {
        String entryName = rootDirectory.relativize(file).toString().replace('\\', '/');
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        Files.copy(file, zipOutputStream);
        zipOutputStream.closeEntry();
    }

    private void uploadIncludedEntries(ChannelSftp channel, Path localWorldPath, String remoteWorldPath)
            throws Exception {
        ensureRemoteDirectories(channel, remoteWorldPath);

        for (String fileName : INCLUDED_ROOT_FILES) {
            Path localFile = localWorldPath.resolve(fileName);
            if (Files.isRegularFile(localFile)) {
                channel.put(localFile.toString(), remoteWorldPath + "/" + fileName);
            }
        }

        for (String dirName : INCLUDED_ROOT_DIRECTORIES) {
            Path localDirectory = localWorldPath.resolve(dirName);
            if (Files.isDirectory(localDirectory)) {
                uploadDirectory(channel, localDirectory, remoteWorldPath + "/" + dirName);
            }
        }
    }

    private void downloadIncludedEntries(ChannelSftp channel, String remoteWorldPath, Path localTargetWorld)
            throws Exception {
        Files.createDirectories(localTargetWorld);

        for (String fileName : INCLUDED_ROOT_FILES) {
            String remoteFile = remoteWorldPath + "/" + fileName;
            if (!remoteExists(channel, remoteFile)) {
                continue;
            }
            Path localFile = localTargetWorld.resolve(fileName);
            Files.createDirectories(localFile.getParent());
            channel.get(remoteFile, localFile.toString());
        }

        for (String dirName : INCLUDED_ROOT_DIRECTORIES) {
            String remoteDir = remoteWorldPath + "/" + dirName;
            if (!remoteExists(channel, remoteDir)) {
                continue;
            }
            downloadDirectory(channel, remoteDir, localTargetWorld.resolve(dirName));
        }
    }

    void assertContainsFiles(Path worldDirectory) throws IOException {
        if (!Files.isDirectory(worldDirectory)) {
            throw new IOException("Downloaded world directory was not created.");
        }
        try (var files = Files.walk(worldDirectory)) {
            boolean hasFile = files.anyMatch(Files::isRegularFile);
            if (!hasFile) {
                throw new IOException("Downloaded world contains no files.");
            }
        }
    }

    static String resolveUniqueName(String baseName, Predicate<String> alreadyExists) {
        String trimmedBase = (baseName == null || baseName.isBlank()) ? "World" : baseName.trim();
        if (!alreadyExists.test(trimmedBase)) {
            return trimmedBase;
        }
        int suffix = 1;
        while (alreadyExists.test(trimmedBase + "_" + suffix)) {
            suffix++;
        }
        return trimmedBase + "_" + suffix;
    }

    private void runRemoteCommand(Session session, String command) throws Exception {
        ChannelExec exec = (ChannelExec) session.openChannel("exec");
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        try {
            exec.setCommand(command);
            exec.setInputStream(null);
            exec.setErrStream(errorStream);
            exec.connect(15000);

            long startedAt = System.currentTimeMillis();
            while (!exec.isClosed()) {
                if (System.currentTimeMillis() - startedAt > REMOTE_COMMAND_TIMEOUT_MILLIS) {
                    throw new IOException("Remote command timed out.");
                }
                Thread.sleep(50);
            }

            if (exec.getExitStatus() != 0) {
                String stderr = errorStream.toString().trim();
                throw new IOException(stderr.isEmpty() ? "Remote command failed." : stderr);
            }
        } finally {
            if (exec.isConnected()) {
                exec.disconnect();
            }
        }
    }

    private String escapeRemote(String path) {
        return path.replace("'", "'\\''");
    }

    private boolean remoteExists(ChannelSftp channel, String remotePath) {
        try {
            channel.stat(remotePath);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private void cleanupLocalWorldDirectory(Path worldDirectory) {
        if (worldDirectory == null || !Files.exists(worldDirectory)) {
            return;
        }
        try (var paths = Files.walk(worldDirectory)) {
            paths.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
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
