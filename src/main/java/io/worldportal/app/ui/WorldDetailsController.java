package io.worldportal.app.ui;

import io.worldportal.app.model.RemoteProfile;
import io.worldportal.app.model.WorldEntry;
import io.worldportal.app.service.TransferService;
import io.worldportal.app.service.impl.StubTransferService;
import io.worldportal.app.service.impl.WhitelistService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class WorldDetailsController {
    private static final Pattern DISPLAY_NAME_FIELD_PATTERN = Pattern.compile("\\\"DisplayName\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    private static final DateTimeFormatter GAME_WORLD_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("MMM d, yyyy HH:mm 'UTC'", Locale.ENGLISH)
            .withZone(ZoneOffset.UTC);

    @FXML
    private HBox detailsWindowDragBar;

    @FXML
    private Button detailsWindowMinimizeButton;

    @FXML
    private Button detailsWindowCloseButton;

    @FXML
    private TextField detailsNameInput;

    @FXML
    private TextField detailsFolderInput;

    @FXML
    private Button detailsSaveIdentityButton;

    @FXML
    private Label detailsIdentityStatusLabel;

    @FXML
    private Label detailsGameModeLabel;

    @FXML
    private Label detailsGameWorldTimeLabel;

    @FXML
    private Label detailsPatchLabel;

    @FXML
    private Label detailsLastPlayedLabel;

    @FXML
    private Label detailsWhitelistTitle;

    @FXML
    private CheckBox detailsWhitelistEnabledCheckBox;

    @FXML
    private ListView<String> detailsWhitelistList;

    @FXML
    private TextField detailsUuidInput;

    @FXML
    private Button detailsAddUuidButton;

    @FXML
    private Button detailsRemoveUuidButton;

    @FXML
    private Button detailsDeleteWorldButton;

    @FXML
    private Label detailsWhitelistStatusLabel;

    @FXML
    private ProgressIndicator detailsSaveLoadingIndicator;

    private final WhitelistService whitelistService;
    private final TransferService transferService;
    private volatile boolean detailsSaveBusy;

    public WorldDetailsController() {
        this(new WhitelistService(), new StubTransferService());
    }

    WorldDetailsController(WhitelistService whitelistService) {
        this(whitelistService, new StubTransferService());
    }

    WorldDetailsController(WhitelistService whitelistService, TransferService transferService) {
        this.whitelistService = whitelistService;
        this.transferService = transferService;
    }

    void initializeDialog(Stage stage, WorldEntry world, Runnable refreshListsAction) {
        initializeDialog(stage, world, refreshListsAction, false, null, transferService);
    }

    void initializeDialog(
            Stage stage,
            WorldEntry world,
            Runnable refreshListsAction,
            boolean serverWorld,
            RemoteProfile remoteProfile,
            TransferService transferService) {
        String initialWorldName = displayName(world);
        Path localPath = world.getPath() == null ? null : Paths.get(world.getPath());
        String initialFolderName = localPath != null && localPath.getFileName() != null
                ? localPath.getFileName().toString()
                : valueOrUnknown(world.getId());

        detailsNameInput.setText(initialWorldName);
        detailsFolderInput.setText(initialFolderName);
        detailsSaveIdentityButton.setVisible(false);
        detailsSaveIdentityButton.setManaged(false);
        detailsIdentityStatusLabel.setText("");
        detailsGameModeLabel.setText("GameMode: " + valueOrUnknown(world.getGameMode()));
        detailsGameWorldTimeLabel.setText("Game world time: " + formatGameWorldTime(world.getGameTimeIso()));
        detailsPatchLabel.setText("Patch: " + valueOrUnknown(world.getPatchLine()));
        detailsLastPlayedLabel.setText("Last played: " + formatLastPlayed(world.getLastModified()));
        setDetailsSaveBusy(false);

        configureDetailsWindowFrame(stage, detailsWindowDragBar, detailsWindowMinimizeButton, detailsWindowCloseButton);

        final Path[] localPathHolder = new Path[] { localPath };
        final String[] originalNameHolder = new String[] { initialWorldName };
        final String[] originalFolderHolder = new String[] { initialFolderName };
        final boolean[] originalWhitelistEnabledHolder = new boolean[] { true };
        final List<String> originalWhitelistPlayers = new ArrayList<>();
        boolean editable = isWorldDetailsEditable(localPath, serverWorld);
        boolean serverWorldEditable = serverWorld && editable;
        boolean localWorldEditable = !serverWorld && editable;

        if (!editable) {
            detailsNameInput.setDisable(true);
            detailsFolderInput.setDisable(true);
            detailsSaveIdentityButton.setDisable(true);
            detailsIdentityStatusLabel.setText("Renaming is only available for local world folders.");
            detailsWhitelistEnabledCheckBox.setDisable(true);
            detailsWhitelistList.setDisable(true);
            detailsUuidInput.setDisable(true);
            detailsAddUuidButton.setDisable(true);
            detailsRemoveUuidButton.setDisable(true);
            detailsDeleteWorldButton.setDisable(true);
            detailsWhitelistStatusLabel.setText("Whitelist editing is only available for local world folders.");
            return;
        }

        if (serverWorldEditable) {
            detailsIdentityStatusLabel.setText("");
        }

        Runnable updateSaveButton = () -> {
            boolean identityChanged = hasWorldIdentityChanges(
                    originalNameHolder[0],
                    originalFolderHolder[0],
                    detailsNameInput.getText(),
                    detailsFolderInput.getText());
            boolean whitelistChanged = hasWhitelistChanges(
                    originalWhitelistEnabledHolder[0],
                    originalWhitelistPlayers,
                    detailsWhitelistEnabledCheckBox.isSelected(),
                    detailsWhitelistList.getItems());
            boolean changed = identityChanged || whitelistChanged;
            detailsSaveIdentityButton.setVisible(changed);
            detailsSaveIdentityButton.setManaged(changed);
            if (!changed) {
                detailsIdentityStatusLabel.setText("");
            }
        };

        detailsNameInput.textProperty().addListener((obs, oldValue, newValue) -> updateSaveButton.run());
        detailsFolderInput.textProperty().addListener((obs, oldValue, newValue) -> updateSaveButton.run());
        detailsWhitelistEnabledCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> updateSaveButton.run());

        WhitelistService.WhitelistConfig loadedWhitelist;
        if (serverWorldEditable) {
            loadedWhitelist = loadServerWhitelistIntoDialog(
                    world,
                    remoteProfile,
                    transferService,
                    detailsWhitelistEnabledCheckBox,
                    detailsWhitelistList,
                    detailsWhitelistStatusLabel);
        } else {
            loadedWhitelist = loadWhitelistIntoDialog(
                    localPathHolder[0],
                    detailsWhitelistEnabledCheckBox,
                    detailsWhitelistList,
                    detailsWhitelistStatusLabel);
        }
        originalWhitelistEnabledHolder[0] = loadedWhitelist.enabled();
        originalWhitelistPlayers.clear();
        originalWhitelistPlayers.addAll(loadedWhitelist.playerUuids());
        detailsWhitelistList.getItems().addListener((ListChangeListener<String>) change -> updateSaveButton.run());
        updateSaveButton.run();

        detailsSaveIdentityButton.setOnAction(event -> {
            if (detailsSaveBusy) {
                return;
            }
            String requestedName = normalizeIdentityInput(detailsNameInput.getText());
            String requestedFolder = normalizeIdentityInput(detailsFolderInput.getText());
            boolean identityChanged = hasWorldIdentityChanges(
                    originalNameHolder[0],
                    originalFolderHolder[0],
                    requestedName,
                    requestedFolder);
            boolean whitelistChanged = hasWhitelistChanges(
                    originalWhitelistEnabledHolder[0],
                    originalWhitelistPlayers,
                    detailsWhitelistEnabledCheckBox.isSelected(),
                    detailsWhitelistList.getItems());
            if (!identityChanged && !whitelistChanged) {
                return;
            }

            if (identityChanged && localWorldEditable) {
                String folderValidation = validateFolderRename(localPathHolder[0], requestedFolder);
                if (folderValidation != null) {
                    detailsIdentityStatusLabel.setText(folderValidation);
                    return;
                }
            }

            String serverFolderValidation = validateServerWorldFolderEdit(originalFolderHolder[0], requestedFolder);
            if (serverWorldEditable && serverFolderValidation != null) {
                detailsIdentityStatusLabel.setText(serverFolderValidation);
                return;
            }

            boolean requestedWhitelistEnabled = detailsWhitelistEnabledCheckBox.isSelected();
            List<String> requestedWhitelistPlayers = new ArrayList<>(detailsWhitelistList.getItems());
            Path localPathSnapshot = localPathHolder[0];

            setDetailsSaveBusy(true);
            detailsIdentityStatusLabel.setText("Saving changes...");
            detailsWhitelistStatusLabel.setText("");

            runAsync(() -> {
                try {
                    SaveResult result = persistWorldDetailsChanges(
                            world,
                            localPathSnapshot,
                            serverWorldEditable,
                            remoteProfile,
                            transferService,
                            requestedName,
                            requestedFolder,
                            requestedWhitelistEnabled,
                            requestedWhitelistPlayers,
                            identityChanged,
                            whitelistChanged);
                    Platform.runLater(() -> {
                        localPathHolder[0] = result.updatedLocalPath();
                        if (identityChanged) {
                            world.setPath(result.updatedWorldPath());
                            world.setId(result.updatedWorldId());
                            world.setName(requestedName);
                            stage.setTitle("World Details - " + requestedName);
                            originalNameHolder[0] = requestedName;
                            originalFolderHolder[0] = requestedFolder;
                        }
                        originalWhitelistEnabledHolder[0] = requestedWhitelistEnabled;
                        originalWhitelistPlayers.clear();
                        originalWhitelistPlayers.addAll(requestedWhitelistPlayers);
                        updateSaveButton.run();
                        detailsWhitelistStatusLabel.setText("");
                        detailsIdentityStatusLabel.setText("Changes saved.");
                        setDetailsSaveBusy(false);
                        refreshListsAction.run();
                    });
                } catch (IOException exception) {
                    Platform.runLater(() -> {
                        detailsIdentityStatusLabel.setText("Failed to save changes: " + exception.getMessage());
                        setDetailsSaveBusy(false);
                    });
                }
            });
        });

        detailsAddUuidButton.setOnAction(event -> {
            String uuid = detailsUuidInput.getText() == null ? "" : detailsUuidInput.getText().trim();
            if (uuid.isBlank() || !isLikelyUuid(uuid)) {
                detailsWhitelistStatusLabel.setText("Invalid UUID format.");
                return;
            }
            if (!detailsWhitelistList.getItems().contains(uuid)) {
                detailsWhitelistList.getItems().add(uuid);
            }
            detailsUuidInput.clear();
            detailsWhitelistStatusLabel.setText("");
        });

        detailsRemoveUuidButton.setOnAction(event -> {
            String selected = detailsWhitelistList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                detailsWhitelistList.getItems().remove(selected);
            }
        });

        detailsDeleteWorldButton.setOnAction(event ->
                openDeleteWorldConfirmationWindow(
                        stage,
                        world,
                        localPathHolder[0],
                        refreshListsAction,
                        serverWorldEditable,
                        remoteProfile,
                        transferService));
    }

    private SaveResult persistWorldDetailsChanges(
            WorldEntry world,
            Path localPath,
            boolean serverWorldEditable,
            RemoteProfile profile,
            TransferService transferService,
            String requestedName,
            String requestedFolder,
            boolean requestedWhitelistEnabled,
            List<String> requestedWhitelistPlayers,
            boolean identityChanged,
            boolean whitelistChanged) throws IOException {
        if (serverWorldEditable) {
            String updatedRemotePath = saveServerWorldChanges(
                    world,
                    profile,
                    transferService,
                    requestedName,
                    requestedFolder,
                    requestedWhitelistEnabled,
                    requestedWhitelistPlayers,
                    identityChanged,
                    whitelistChanged);
            return new SaveResult(localPath, updatedRemotePath, requestedFolder);
        }

        Path targetFolder = localPath.resolveSibling(requestedFolder);
        if (identityChanged && !targetFolder.equals(localPath)) {
            Files.move(localPath, targetFolder);
        }
        if (identityChanged) {
            updateWorldDisplayName(targetFolder, requestedName);
        }
        if (whitelistChanged) {
            whitelistService.save(
                    targetFolder,
                    new WhitelistService.WhitelistConfig(requestedWhitelistEnabled, requestedWhitelistPlayers));
        }
        if (identityChanged) {
            return new SaveResult(targetFolder, targetFolder.toString(), requestedFolder);
        }
        return new SaveResult(targetFolder, world.getPath(), world.getId());
    }

    private String saveServerWorldChanges(
            WorldEntry world,
            RemoteProfile profile,
            TransferService transferService,
            String requestedName,
            String requestedFolder,
            boolean whitelistEnabled,
            List<String> whitelistPlayers,
            boolean identityChanged,
            boolean whitelistChanged) throws IOException {
        if (world == null || profile == null || transferService == null) {
            throw new IOException("Server world editing context is unavailable.");
        }
        Path stagingRoot = Files.createTempDirectory("world-portal-server-world-edit");
        Path stagingWorldPath = stagingRoot.resolve(valueOrUnknown(world.getId()));
        String remoteWorldPath = world.getPath();
        WorldEntry stagedLocalWorld = new WorldEntry();
        stagedLocalWorld.setPath(stagingWorldPath.toString());
        try {
            boolean folderChanged = !normalizeIdentityInput(valueOrUnknown(world.getId()))
                    .equals(normalizeIdentityInput(requestedFolder));
            if (folderChanged) {
                transferService.renameRemoteWorld(world, requestedFolder, profile);
                if (remoteWorldPath != null && !remoteWorldPath.isBlank()) {
                    int lastSlash = remoteWorldPath.lastIndexOf('/');
                    if (lastSlash >= 0) {
                        remoteWorldPath = remoteWorldPath.substring(0, lastSlash + 1) + requestedFolder;
                    }
                }
            }

            WorldEntry remoteWorldForSync = new WorldEntry();
            remoteWorldForSync.setPath(remoteWorldPath);
            transferService.syncRemoteToLocalWorld(remoteWorldForSync, stagedLocalWorld, profile);
            if (identityChanged) {
                updateWorldDisplayName(stagingWorldPath, requestedName);
            }
            if (whitelistChanged) {
                whitelistService.save(
                        stagingWorldPath,
                        new WhitelistService.WhitelistConfig(whitelistEnabled, whitelistPlayers));
            }
            transferService.syncLocalToRemoteWorld(stagedLocalWorld, remoteWorldForSync, profile);
            return remoteWorldPath;
        } catch (Exception exception) {
            throw new IOException("Failed to save server world changes: " + exception.getMessage(), exception);
        } finally {
            deleteWorldDirectory(stagingRoot);
        }
    }

    private void openDeleteWorldConfirmationWindow(
            Stage owner,
            WorldEntry world,
            Path worldPath,
            Runnable refreshListsAction,
            boolean serverWorld,
            RemoteProfile remoteProfile,
            TransferService transferService) {
        Stage confirmStage = new Stage();
        confirmStage.initOwner(owner);
        confirmStage.initModality(Modality.WINDOW_MODAL);
        confirmStage.initStyle(StageStyle.TRANSPARENT);
        confirmStage.setTitle("Delete World - " + displayName(world));

        FXMLLoader loader = new FXMLLoader(
                WorldDetailsController.class.getResource("/io/worldportal/app/world-delete-confirmation-view.fxml"));
        Region root;
        try {
            root = loader.load();
        } catch (IOException exception) {
            return;
        }

        Map<String, Object> namespace = loader.getNamespace();
        Label promptLabel = requiredNode(namespace, "deleteWorldPromptLabel", Label.class);
        TextField worldNameInput = requiredNode(namespace, "deleteWorldNameInput", TextField.class);
        Label statusLabel = requiredNode(namespace, "deleteWorldStatusLabel", Label.class);
        Button confirmButton = requiredNode(namespace, "confirmDeleteWorldButton", Button.class);
        Button cancelButton = requiredNode(namespace, "cancelDeleteWorldButton", Button.class);

        String expectedName = displayName(world);
        promptLabel.setText("Type \"" + expectedName + "\" to confirm deletion.");

        cancelButton.setOnAction(event -> confirmStage.close());
        confirmButton.setOnAction(event -> {
            if (!matchesDeleteConfirmation(expectedName, worldNameInput.getText())) {
                statusLabel.setText("World name does not match.");
                return;
            }

            confirmButton.setDisable(true);
            cancelButton.setDisable(true);
            statusLabel.setText("Deleting world...");

            runAsync(() -> {
                try {
                    if (serverWorld) {
                        deleteServerWorld(world, remoteProfile, transferService);
                    } else {
                        deleteWorldDirectory(worldPath);
                    }
                    Platform.runLater(() -> {
                        confirmStage.close();
                        owner.close();
                        refreshListsAction.run();
                    });
                } catch (IOException exception) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Failed to delete world: " + exception.getMessage());
                        confirmButton.setDisable(false);
                        cancelButton.setDisable(false);
                    });
                }
            });
        });

        Scene confirmationScene = new Scene(root, 420, 220);
        applyWorldDetailsTheme(confirmationScene);
        confirmStage.setScene(confirmationScene);
        confirmStage.show();
        Platform.runLater(worldNameInput::requestFocus);
    }

    private void deleteServerWorld(WorldEntry world, RemoteProfile profile, TransferService transferService) throws IOException {
        if (world == null || profile == null || transferService == null) {
            throw new IOException("Server world deletion context is unavailable.");
        }
        try {
            transferService.deleteRemoteWorld(world, profile);
        } catch (Exception exception) {
            throw new IOException("Failed to delete server world: " + exception.getMessage(), exception);
        }
    }

    static boolean matchesDeleteConfirmation(String expectedWorldName, String enteredWorldName) {
        if (expectedWorldName == null || enteredWorldName == null) {
            return false;
        }
        return expectedWorldName.equals(enteredWorldName.trim());
    }

    static boolean hasWorldIdentityChanges(
            String originalName,
            String originalFolder,
            String enteredName,
            String enteredFolder) {
        String normalizedOriginalName = normalizeIdentityInput(originalName);
        String normalizedOriginalFolder = normalizeIdentityInput(originalFolder);
        String normalizedEnteredName = normalizeIdentityInput(enteredName);
        String normalizedEnteredFolder = normalizeIdentityInput(enteredFolder);

        return !normalizedOriginalName.equals(normalizedEnteredName)
                || !normalizedOriginalFolder.equals(normalizedEnteredFolder);
    }

    static boolean hasWhitelistChanges(
            boolean originalEnabled,
            List<String> originalPlayers,
            boolean enteredEnabled,
            List<String> enteredPlayers) {
        List<String> baselinePlayers = originalPlayers == null ? List.of() : originalPlayers;
        List<String> currentPlayers = enteredPlayers == null ? List.of() : enteredPlayers;
        return originalEnabled != enteredEnabled || !baselinePlayers.equals(currentPlayers);
    }

    static boolean isWorldDetailsEditable(Path worldPath, boolean serverWorld) {
        return serverWorld || (worldPath != null && Files.isDirectory(worldPath));
    }

    static String validateServerWorldFolderEdit(String originalFolderName, String requestedFolderName) {
        String normalizedFolderName = normalizeIdentityInput(requestedFolderName);
        if (normalizedFolderName.isBlank()) {
            return "World folder name cannot be empty.";
        }
        if (normalizedFolderName.contains("/") || normalizedFolderName.contains("\\")) {
            return "World folder name cannot include path separators.";
        }
        return null;
    }

    static String validateFolderRename(Path worldFolderPath, String requestedFolderName) {
        if (worldFolderPath == null) {
            return "World folder is unavailable.";
        }

        String normalizedFolderName = normalizeIdentityInput(requestedFolderName);
        if (normalizedFolderName.isBlank()) {
            return "World folder name cannot be empty.";
        }
        if (normalizedFolderName.contains("/") || normalizedFolderName.contains("\\")) {
            return "World folder name cannot include path separators.";
        }

        Path requestedFolderPath = worldFolderPath.resolveSibling(normalizedFolderName);
        if (requestedFolderPath.equals(worldFolderPath)) {
            return null;
        }
        if (Files.exists(requestedFolderPath)) {
            return "A world folder with this name already exists.";
        }
        return null;
    }

    static void updateWorldDisplayName(Path worldFolderPath, String worldName) throws IOException {
        if (worldFolderPath == null) {
            throw new IOException("World folder is unavailable.");
        }

        String normalizedWorldName = normalizeIdentityInput(worldName);
        Path configPath = worldFolderPath.resolve("universe/worlds/default/config.json");
        if (!Files.exists(configPath)) {
            throw new IOException("World config file does not exist.");
        }

        String config = Files.readString(configPath, StandardCharsets.UTF_8);
        String escapedName = escapeJson(normalizedWorldName);
        Matcher displayNameMatcher = DISPLAY_NAME_FIELD_PATTERN.matcher(config);
        if (displayNameMatcher.find()) {
            String replacement = Matcher.quoteReplacement("\"DisplayName\": \"" + escapedName + "\"");
            config = displayNameMatcher.replaceFirst(replacement);
        } else {
            String insertion = "{\n  \"DisplayName\": \"" + escapedName + "\",";
            config = config.replaceFirst("\\{", Matcher.quoteReplacement(insertion));
        }
        Files.writeString(configPath, config, StandardCharsets.UTF_8);
    }

    static void deleteWorldDirectory(Path worldPath) throws IOException {
        if (worldPath == null || !Files.exists(worldPath)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(worldPath)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    static void applyWorldDetailsTheme(Scene scene) {
        scene.setFill(Color.TRANSPARENT);
    }

    private static String normalizeIdentityInput(String value) {
        return value == null ? "" : value.trim();
    }

    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private static void configureDetailsWindowFrame(
            Stage stage,
            HBox appBar,
            Button minimizeButton,
            Button closeButton) {
        minimizeButton.setOnAction(event -> stage.setIconified(true));
        closeButton.setOnAction(event -> stage.close());

        final double[] dragOffset = new double[2];
        appBar.setOnMousePressed(event -> {
            dragOffset[0] = stage.getX() - event.getScreenX();
            dragOffset[1] = stage.getY() - event.getScreenY();
        });
        appBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() + dragOffset[0]);
            stage.setY(event.getScreenY() + dragOffset[1]);
        });
    }

    private WhitelistService.WhitelistConfig loadWhitelistIntoDialog(
            Path worldPath,
            CheckBox enabledCheckBox,
            ListView<String> playerUuids,
            Label statusLabel) {
        try {
            WhitelistService.WhitelistConfig config = whitelistService.load(worldPath);
            enabledCheckBox.setSelected(config.enabled());
            playerUuids.setItems(FXCollections.observableArrayList(config.playerUuids()));
            statusLabel.setText("");
            return config;
        } catch (IOException exception) {
            enabledCheckBox.setSelected(true);
            playerUuids.setItems(FXCollections.observableArrayList());
            statusLabel.setText("Failed to load whitelist: " + exception.getMessage());
            return new WhitelistService.WhitelistConfig(true, List.of());
        }
    }

    private WhitelistService.WhitelistConfig loadServerWhitelistIntoDialog(
            WorldEntry world,
            RemoteProfile profile,
            TransferService transferService,
            CheckBox enabledCheckBox,
            ListView<String> playerUuids,
            Label statusLabel) {
        if (world == null || profile == null || transferService == null) {
            enabledCheckBox.setSelected(true);
            playerUuids.setItems(FXCollections.observableArrayList());
            statusLabel.setText("Failed to load whitelist: server world context is unavailable.");
            return new WhitelistService.WhitelistConfig(true, List.of());
        }

        Path stagingRoot = null;
        try {
            stagingRoot = Files.createTempDirectory("world-portal-server-world-load");
            Path stagingWorldPath = stagingRoot.resolve(valueOrUnknown(world.getId()));
            WorldEntry stagedLocalWorld = new WorldEntry();
            stagedLocalWorld.setPath(stagingWorldPath.toString());

            transferService.syncRemoteToLocalWorld(world, stagedLocalWorld, profile);
            WhitelistService.WhitelistConfig config = whitelistService.load(stagingWorldPath);
            enabledCheckBox.setSelected(config.enabled());
            playerUuids.setItems(FXCollections.observableArrayList(config.playerUuids()));
            statusLabel.setText("");
            return config;
        } catch (Exception exception) {
            enabledCheckBox.setSelected(true);
            playerUuids.setItems(FXCollections.observableArrayList());
            statusLabel.setText("Failed to load whitelist: " + exception.getMessage());
            return new WhitelistService.WhitelistConfig(true, List.of());
        } finally {
            if (stagingRoot != null) {
                try {
                    deleteWorldDirectory(stagingRoot);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private String displayName(WorldEntry world) {
        if (world == null) {
            return "Unknown";
        }
        if (world.getName() != null && !world.getName().isBlank()) {
            return world.getName();
        }
        return valueOrUnknown(world.getId());
    }

    private String valueOrUnknown(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }
        return value;
    }

    private String formatLastPlayed(Instant value) {
        if (value == null || value.equals(Instant.EPOCH)) {
            return "Unknown";
        }
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(value);
    }

    static String formatGameWorldTime(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }
        String normalized = value.trim();
        try {
            return GAME_WORLD_TIME_FORMATTER.format(Instant.parse(normalized));
        } catch (Exception ignored) {
            return normalized;
        }
    }

    private boolean isLikelyUuid(String uuid) {
        return uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    @SuppressWarnings("unchecked")
    private static <T> T requiredNode(Map<String, Object> namespace, String id, Class<T> expectedType) {
        Object value = namespace.get(id);
        if (value == null) {
            throw new IllegalStateException("Missing node: " + id);
        }
        if (!expectedType.isInstance(value)) {
            throw new IllegalStateException("Unexpected node type for '" + id + "'");
        }
        return (T) value;
    }

    private void runAsync(Runnable work) {
        Thread worker = new Thread(() -> {
            try {
                work.run();
            } catch (Exception ignored) {
            }
        }, "world-portal-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void setDetailsSaveBusy(boolean busy) {
        detailsSaveBusy = busy;
        detailsNameInput.setDisable(busy);
        detailsFolderInput.setDisable(busy);
        detailsWhitelistEnabledCheckBox.setDisable(busy);
        detailsWhitelistList.setDisable(busy);
        detailsUuidInput.setDisable(busy);
        detailsAddUuidButton.setDisable(busy);
        detailsRemoveUuidButton.setDisable(busy);
        detailsDeleteWorldButton.setDisable(busy);
        detailsSaveIdentityButton.setDisable(busy);
        detailsWindowMinimizeButton.setDisable(busy);
        detailsWindowCloseButton.setDisable(busy);
        if (detailsSaveLoadingIndicator != null) {
            detailsSaveLoadingIndicator.setVisible(busy);
            detailsSaveLoadingIndicator.setManaged(busy);
        }
    }

    private record SaveResult(Path updatedLocalPath, String updatedWorldPath, String updatedWorldId) {
    }
}
