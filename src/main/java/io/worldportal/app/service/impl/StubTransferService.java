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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Predicate;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class StubTransferService implements TransferService {
    private static final long REMOTE_COMMAND_TIMEOUT_MILLIS = 30_000L;
    private static final DateTimeFormatter BACKUP_FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final Set<String> INCLUDED_ROOT_FILES = Set.of(
            "bans.json",
            "client_metadata.json",
            "config.json",
            "permissions.json",
            "preview.png",
            "whitelist.json");
    private static final Set<String> INCLUDED_ROOT_DIRECTORIES = Set.of("mods", "universe");
    private static final Set<String> INCLUDED_UNIVERSE_FILES = Set.of("memories.json", "memories.json.bak");
    private static final Set<String> INCLUDED_UNIVERSE_DIRECTORIES = Set.of("players", "worlds");

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

    @Override
    public void syncRemoteToLocalWorld(WorldEntry remoteWorld, WorldEntry localWorld, RemoteProfile profile) {
        if (remoteWorld == null || localWorld == null || profile == null) {
            return;
        }
        if (remoteWorld.getPath() == null || remoteWorld.getPath().isBlank()
                || localWorld.getPath() == null || localWorld.getPath().isBlank()) {
            return;
        }

        Path localTargetWorld = Paths.get(localWorld.getPath());
        Session session = null;
        ChannelSftp channel = null;
        try {
            Files.createDirectories(localTargetWorld);
            createUniverseBackup(localTargetWorld, LocalDateTime.now());
            session = SshSessionFactory.createConnectedSession(profile);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(15000);

            downloadIncludedEntries(channel, remoteWorld.getPath(), localTargetWorld);
            assertContainsFiles(localTargetWorld);
        } catch (Exception failure) {
            throw new RuntimeException("Sync failed.", failure);
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
    public void syncLocalToRemoteWorld(WorldEntry localWorld, WorldEntry remoteWorld, RemoteProfile profile) {
        if (localWorld == null || remoteWorld == null || profile == null) {
            return;
        }
        if (localWorld.getPath() == null || localWorld.getPath().isBlank()
                || remoteWorld.getPath() == null || remoteWorld.getPath().isBlank()) {
            return;
        }

        Path localWorldPath = Paths.get(localWorld.getPath());
        if (!Files.isDirectory(localWorldPath)) {
            return;
        }

        Session session = null;
        ChannelSftp channel = null;
        try {
            session = SshSessionFactory.createConnectedSession(profile);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(15000);
            createRemoteUniverseBackup(channel, remoteWorld.getPath(), LocalDateTime.now());

            uploadIncludedEntries(channel, localWorldPath, remoteWorld.getPath());
        } catch (Exception failure) {
            throw new RuntimeException("Sync failed.", failure);
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
    public void renameRemoteWorld(WorldEntry remoteWorld, String requestedFolderName, RemoteProfile profile) {
        if (remoteWorld == null || profile == null) {
            return;
        }
        String currentRemotePath = remoteWorld.getPath();
        if (currentRemotePath == null || currentRemotePath.isBlank()) {
            return;
        }

        String normalizedRequestedFolder = requestedFolderName == null ? "" : requestedFolderName.trim();
        if (normalizedRequestedFolder.isBlank()
                || normalizedRequestedFolder.contains("/")
                || normalizedRequestedFolder.contains("\\")) {
            throw new RuntimeException("World folder name cannot include path separators.");
        }

        String currentFolder = remoteLeafName(currentRemotePath);
        if (normalizedRequestedFolder.equals(currentFolder)) {
            return;
        }

        String parentPath = remoteParentPath(currentRemotePath);
        String targetRemotePath = parentPath + "/" + normalizedRequestedFolder;
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = SshSessionFactory.createConnectedSession(profile);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(15000);

            if (remoteExists(channel, targetRemotePath)) {
                throw new RuntimeException("A world folder with this name already exists.");
            }
            channel.rename(normalizeRemotePath(currentRemotePath), normalizeRemotePath(targetRemotePath));
        } catch (Exception failure) {
            throw new RuntimeException("Remote rename failed.", failure);
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
    public void deleteRemoteWorld(WorldEntry remoteWorld, RemoteProfile profile) {
        if (remoteWorld == null || profile == null) {
            return;
        }
        String remotePath = remoteWorld.getPath();
        if (remotePath == null || remotePath.isBlank()) {
            return;
        }

        Session session = null;
        ChannelSftp channel = null;
        try {
            session = SshSessionFactory.createConnectedSession(profile);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(15000);
            deleteRemoteDirectory(channel, normalizeRemotePath(remotePath));
        } catch (Exception failure) {
            throw new RuntimeException("Remote delete failed.", failure);
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

    Path createUniverseBackup(Path worldDirectory, LocalDateTime backupTime) throws IOException {
        Path backupDirectory = worldDirectory.resolve("backup");
        Files.createDirectories(backupDirectory);
        Path universeDirectory = worldDirectory.resolve("universe");
        String timestamp = BACKUP_FILE_TIME_FORMATTER.format(backupTime);
        Path backupArchive = backupDirectory.resolve(timestamp + ".zip");
        try (OutputStream out = Files.newOutputStream(backupArchive, StandardOpenOption.CREATE_NEW);
                ZipOutputStream zipOutputStream = new ZipOutputStream(out)) {
            for (String fileName : INCLUDED_UNIVERSE_FILES) {
                Path candidate = universeDirectory.resolve(fileName);
                if (Files.isRegularFile(candidate)) {
                    addFileToZip(universeDirectory, candidate, zipOutputStream);
                }
            }
            for (String directoryName : INCLUDED_UNIVERSE_DIRECTORIES) {
                Path directory = universeDirectory.resolve(directoryName);
                if (Files.isDirectory(directory)) {
                    addDirectoryToZip(universeDirectory, directory, zipOutputStream);
                }
            }
        }
        return backupArchive;
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

    private void createRemoteUniverseBackup(ChannelSftp channel, String remoteWorldPath, LocalDateTime backupTime)
            throws Exception {
        String normalizedRemoteWorldPath = normalizeRemotePath(remoteWorldPath);
        String remoteUniversePath = normalizedRemoteWorldPath + "/universe";
        if (!remoteExists(channel, remoteUniversePath)) {
            return;
        }
        String remoteBackupDirectory = normalizedRemoteWorldPath + "/backup";
        ensureRemoteDirectories(channel, remoteBackupDirectory);

        Path localArchive = Files.createTempFile("world-portal-sync-backup-", ".zip");
        try (OutputStream out = Files.newOutputStream(localArchive, StandardOpenOption.TRUNCATE_EXISTING);
                ZipOutputStream zipOutputStream = new ZipOutputStream(out)) {
            for (String fileName : INCLUDED_UNIVERSE_FILES) {
                String remoteFile = remoteUniversePath + "/" + fileName;
                if (remoteExists(channel, remoteFile)) {
                    addRemoteFileToZip(channel, remoteUniversePath, remoteFile, zipOutputStream);
                }
            }
            for (String directoryName : INCLUDED_UNIVERSE_DIRECTORIES) {
                String remoteDirectory = remoteUniversePath + "/" + directoryName;
                if (remoteExists(channel, remoteDirectory)) {
                    addRemoteDirectoryToZip(channel, remoteUniversePath, remoteDirectory, zipOutputStream);
                }
            }
        }

        String archiveName = BACKUP_FILE_TIME_FORMATTER.format(backupTime) + ".zip";
        String remoteArchivePath = remoteBackupDirectory + "/" + archiveName;
        try {
            channel.put(localArchive.toString(), remoteArchivePath);
        } finally {
            Files.deleteIfExists(localArchive);
        }
    }

    private void addRemoteDirectoryToZip(
            ChannelSftp channel,
            String remoteUniversePath,
            String remoteDirectoryPath,
            ZipOutputStream zipOutputStream) throws Exception {
        @SuppressWarnings("unchecked")
        List<ChannelSftp.LsEntry> entries = channel.ls(remoteDirectoryPath);
        for (ChannelSftp.LsEntry entry : entries) {
            String name = entry.getFilename();
            if (".".equals(name) || "..".equals(name)) {
                continue;
            }
            String remoteChild = remoteDirectoryPath + "/" + name;
            if (entry.getAttrs().isDir()) {
                addRemoteDirectoryToZip(channel, remoteUniversePath, remoteChild, zipOutputStream);
            } else {
                addRemoteFileToZip(channel, remoteUniversePath, remoteChild, zipOutputStream);
            }
        }
    }

    private void addRemoteFileToZip(
            ChannelSftp channel,
            String remoteUniversePath,
            String remoteFilePath,
            ZipOutputStream zipOutputStream) throws Exception {
        String entryName = toRelativeRemotePath(remoteUniversePath, remoteFilePath);
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        try (InputStream in = channel.get(remoteFilePath)) {
            in.transferTo(zipOutputStream);
        }
        zipOutputStream.closeEntry();
    }

    private String toRelativeRemotePath(String rootPath, String childPath) {
        String normalizedRoot = normalizeRemotePath(rootPath);
        String normalizedChild = normalizeRemotePath(childPath);
        String prefix = normalizedRoot + "/";
        if (normalizedChild.startsWith(prefix)) {
            return normalizedChild.substring(prefix.length());
        }
        return normalizedChild;
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

    private void deleteRemoteDirectory(ChannelSftp channel, String remoteDirectory) throws Exception {
        @SuppressWarnings("unchecked")
        List<ChannelSftp.LsEntry> entries = channel.ls(remoteDirectory);
        for (ChannelSftp.LsEntry entry : entries) {
            String name = entry.getFilename();
            if (".".equals(name) || "..".equals(name)) {
                continue;
            }
            String child = remoteDirectory + "/" + name;
            if (entry.getAttrs().isDir()) {
                deleteRemoteDirectory(channel, child);
            } else {
                channel.rm(child);
            }
        }
        channel.rmdir(remoteDirectory);
    }

    private String remoteParentPath(String remotePath) {
        String normalized = normalizeRemotePath(remotePath);
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        return normalized.substring(0, lastSlash);
    }

    private String remoteLeafName(String remotePath) {
        String normalized = normalizeRemotePath(remotePath);
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == normalized.length() - 1) {
            return normalized;
        }
        return normalized.substring(lastSlash + 1);
    }

    private String normalizeRemotePath(String remotePath) {
        return remotePath.replace('\\', '/').replaceAll("/+$", "");
    }
}
