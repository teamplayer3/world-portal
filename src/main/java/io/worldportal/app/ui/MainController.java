package io.worldportal.app.ui;

import io.worldportal.app.config.ConnectionSettingsStore;
import io.worldportal.app.model.RemoteProfile;
import io.worldportal.app.model.WorldEntry;
import io.worldportal.app.service.TransferService;
import io.worldportal.app.service.WorldService;
import io.worldportal.app.service.impl.SshConnectionService;
import io.worldportal.app.service.impl.StubTransferService;
import io.worldportal.app.service.impl.StubWorldService;
import io.worldportal.app.service.impl.WhitelistService;
import io.worldportal.app.service.impl.WorldComparisonService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.MouseButton;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Scene;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Map;

public class MainController {

    @FXML
    private ListView<WorldEntry> localWorldsList;

    @FXML
    private ListView<WorldEntry> remoteWorldsList;

    @FXML
    private TextField hostField;

    @FXML
    private TextField portField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField remotePathField;

    @FXML
    private TextField localWorldsPathField;

    @FXML
    private ComboBox<String> authTypeCombo;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<String> publicKeyFileCombo;

    @FXML
    private Label passwordLabel;

    @FXML
    private Label publicKeyFileLabel;

    @FXML
    private Button refreshKeysButton;

    @FXML
    private HBox publicKeyContainer;

    @FXML
    private Label connectStatusLabel;

    @FXML
    private Button connectButton;

    @FXML
    private ProgressIndicator connectLoadingIndicator;

    @FXML
    private Button uploadButton;

    @FXML
    private Button downloadButton;

    @FXML
    private Button refreshButton;

    @FXML
    private ProgressIndicator transferProgressIndicator;

    @FXML
    private Label transferStatusLabel;

    private volatile boolean remoteConnectionBusy;
    private volatile boolean transferBusy;

    private final WorldService worldService;
    private final TransferService transferService;
    private final SshConnectionService sshConnectionService;
    private final WorldComparisonService worldComparisonService;
    private final WhitelistService whitelistService;
    private final ConnectionSettingsStore connectionSettingsStore;
    private final ObservableList<WorldEntry> localWorlds = FXCollections.observableArrayList();
    private final ObservableList<WorldEntry> remoteWorlds = FXCollections.observableArrayList();
    private final ConcurrentHashMap<String, Boolean> remotePreviewLoading = new ConcurrentHashMap<>();

    public MainController() {
        this(
                new StubWorldService(),
                new StubTransferService(),
                new SshConnectionService(),
                new WorldComparisonService(),
                new WhitelistService(),
                new ConnectionSettingsStore());
    }

    public MainController(WorldService worldService, TransferService transferService) {
        this(worldService, transferService, new SshConnectionService(), new WorldComparisonService(),
                new WhitelistService(),
                new ConnectionSettingsStore());
    }

    MainController(
            WorldService worldService,
            TransferService transferService,
            SshConnectionService sshConnectionService,
            WorldComparisonService worldComparisonService,
            WhitelistService whitelistService,
            ConnectionSettingsStore connectionSettingsStore) {
        this.worldService = worldService;
        this.transferService = transferService;
        this.sshConnectionService = sshConnectionService;
        this.worldComparisonService = worldComparisonService;
        this.whitelistService = whitelistService;
        this.connectionSettingsStore = connectionSettingsStore;
    }

    @FXML
    private void initialize() {
        localWorldsList.setItems(localWorlds);
        localWorldsList.setCellFactory(listView -> new WorldCell(true));
        localWorldsList.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> syncTransferButtons());
        localWorldsList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                WorldEntry selected = localWorldsList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openWorldDetailsWindow(selected);
                }
            }
        });
        remoteWorldsList.setItems(remoteWorlds);
        remoteWorldsList.setCellFactory(listView -> new WorldCell(false));
        remoteWorldsList.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> syncTransferButtons());
        remoteWorldsList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                WorldEntry selected = remoteWorldsList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openWorldDetailsWindow(selected);
                }
            }
        });
        authTypeCombo.setItems(FXCollections.observableArrayList("Password", "Public Key"));
        authTypeCombo.getSelectionModel().selectFirst();
        authTypeCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateAuthInputState());
        publicKeyFileCombo.setItems(FXCollections.observableArrayList(loadPublicKeyFiles()));
        if (!publicKeyFileCombo.getItems().isEmpty()) {
            publicKeyFileCombo.getSelectionModel().selectFirst();
        }
        portField.setText("22");
        localWorldsPathField.setText(defaultLocalWorldsPath());

        applyCachedConnectionSettings();
        setRemoteConnectionBusy(false);
        setTransferBusy(false, "");
        updateAuthInputState();
        refreshLists();
    }

    @FXML
    private void onConnect() {
        if (remoteConnectionBusy) {
            return;
        }
        setRemoteConnectionBusy(true);
        Platform.runLater(() -> connectStatusLabel.setText(""));
        runAsync(() -> {
            boolean connected = sshConnectionService.connect(buildRemoteProfile());
            if (connected) {
                connectionSettingsStore.save(buildRemoteProfile());
                Platform.runLater(() -> connectStatusLabel.setText(""));
                refreshRemoteWorlds();
            } else {
                Platform.runLater(() -> {
                    remoteWorlds.clear();
                    String error = sshConnectionService.getLastErrorMessage();
                    connectStatusLabel.setText(error == null ? "Connection failed." : error);
                });
            }
            Platform.runLater(() -> {
                setRemoteConnectionBusy(false);
                syncTransferButtons();
            });
        });
    }

    @FXML
    private void onUpload() {
        WorldEntry selectedWorld = localWorldsList.getSelectionModel().getSelectedItem();
        if (selectedWorld == null) {
            return;
        }
        runTransferAsync(
                "Uploading...",
                "Upload finished.",
                () -> {
                    transferService.uploadWorld(selectedWorld, buildRemoteProfile());
                    refreshLists();
                });
    }

    @FXML
    private void onDownload() {
        WorldEntry selectedWorld = remoteWorldsList.getSelectionModel().getSelectedItem();
        if (selectedWorld == null) {
            return;
        }
        runTransferAsync(
                "Downloading...",
                "Download finished.",
                () -> {
                    transferService.downloadWorld(selectedWorld, buildRemoteProfile());
                    refreshLists();
                });
    }

    @FXML
    private void onRefresh() {
        if (transferBusy) {
            return;
        }
        runAsync(this::refreshLists);
    }

    @FXML
    private void onRefreshKeys() {
        List<String> keys = loadPublicKeyFiles();
        publicKeyFileCombo.setItems(FXCollections.observableArrayList(keys));
        if (!keys.isEmpty()) {
            publicKeyFileCombo.getSelectionModel().selectFirst();
        }
    }

    private void refreshLists() {
        List<WorldEntry> local = worldService.listLocalWorlds(getConfiguredLocalWorldsPath());
        if (!sshConnectionService.isConnected()) {
            worldComparisonService.annotateMatches(local, List.of());
            Platform.runLater(() -> {
                localWorlds.setAll(local);
                remoteWorlds.clear();
                syncTransferButtons();
            });
            return;
        }

        List<WorldEntry> remote = worldService.listRemoteWorlds(buildRemoteProfile());
        worldComparisonService.annotateMatches(local, remote);

        Platform.runLater(() -> {
            localWorlds.setAll(local);
            remoteWorlds.setAll(remote);
            for (WorldEntry world : remote) {
                maybeLoadRemotePreviewAsync(world);
            }
            syncTransferButtons();
        });
    }

    private void refreshRemoteWorlds() {
        if (!sshConnectionService.isConnected()) {
            Platform.runLater(() -> {
                remoteWorlds.clear();
                syncTransferButtons();
            });
            return;
        }
        List<WorldEntry> remote = worldService.listRemoteWorlds(buildRemoteProfile());
        List<WorldEntry> local = worldService.listLocalWorlds(getConfiguredLocalWorldsPath());
        worldComparisonService.annotateMatches(local, remote);

        Platform.runLater(() -> {
            localWorlds.setAll(local);
            remoteWorlds.setAll(remote);
            for (WorldEntry world : remote) {
                maybeLoadRemotePreviewAsync(world);
            }
            syncTransferButtons();
        });
    }

    private RemoteProfile buildRemoteProfile() {
        int parsedPort;
        try {
            parsedPort = Integer.parseInt(portField.getText());
        } catch (NumberFormatException exception) {
            parsedPort = 22;
        }
        return new RemoteProfile(
                hostField.getText(),
                parsedPort,
                usernameField.getText(),
                remotePathField.getText(),
                authTypeCombo.getValue(),
                passwordField.getText(),
                publicKeyFileCombo.getValue(),
                getConfiguredLocalWorldsPath());
    }

    public WorldService getWorldService() {
        return worldService;
    }

    public TransferService getTransferService() {
        return transferService;
    }

    public String getConfiguredLocalWorldsPath() {
        return localWorldsPathField.getText();
    }

    private String defaultLocalWorldsPath() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            String userHome = System.getProperty("user.home", "");
            return userHome + "\\AppData\\Roaming\\Hytale\\UserData\\Saves";
        }
        if (osName.contains("mac")) {
            String userHome = System.getProperty("user.home", "");
            return userHome + "/Library/Application Support/Hytale/UserData/Saves";
        }

        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        if (xdgDataHome == null || xdgDataHome.isBlank()) {
            String userHome = System.getProperty("user.home", "");
            xdgDataHome = userHome + "/.local/share";
        }
        return xdgDataHome + "/Hytale/UserData/Saves";
    }

    private void updateAuthInputState() {
        if (remoteConnectionBusy) {
            passwordField.setDisable(true);
            passwordLabel.setDisable(true);
            publicKeyFileCombo.setDisable(true);
            publicKeyFileLabel.setDisable(true);
            refreshKeysButton.setDisable(true);
            publicKeyContainer.setDisable(true);
            publicKeyContainer.setMouseTransparent(true);
            return;
        }

        boolean publicKeySelected = "Public Key".equalsIgnoreCase(authTypeCombo.getValue());

        boolean showPassword = !publicKeySelected;
        boolean showPublicKey = publicKeySelected;

        passwordField.setVisible(showPassword);
        passwordField.setManaged(showPassword);
        passwordField.setDisable(!showPassword);
        passwordLabel.setVisible(showPassword);
        passwordLabel.setManaged(showPassword);
        passwordLabel.setDisable(!showPassword);

        publicKeyFileCombo.setVisible(showPublicKey);
        publicKeyFileCombo.setManaged(showPublicKey);
        publicKeyFileCombo.setDisable(!showPublicKey);
        publicKeyFileLabel.setVisible(showPublicKey);
        publicKeyFileLabel.setManaged(showPublicKey);
        publicKeyFileLabel.setDisable(!showPublicKey);
        refreshKeysButton.setVisible(showPublicKey);
        refreshKeysButton.setManaged(showPublicKey);
        refreshKeysButton.setDisable(!showPublicKey);

        publicKeyContainer.setVisible(showPublicKey);
        publicKeyContainer.setManaged(showPublicKey);
        publicKeyContainer.setDisable(!showPublicKey);
        publicKeyContainer.setMouseTransparent(!showPublicKey);
    }

    private void setRemoteConnectionBusy(boolean busy) {
        remoteConnectionBusy = busy;

        hostField.setDisable(busy);
        portField.setDisable(busy);
        usernameField.setDisable(busy);
        remotePathField.setDisable(busy);
        authTypeCombo.setDisable(busy);
        if (connectButton != null) {
            connectButton.setDisable(busy);
        }
        if (connectLoadingIndicator != null) {
            connectLoadingIndicator.setVisible(busy);
            connectLoadingIndicator.setManaged(busy);
        }
        updateAuthInputState();
    }

    private void setTransferBusy(boolean busy, String statusText) {
        transferBusy = busy;
        syncTransferButtons();
        if (transferProgressIndicator != null) {
            transferProgressIndicator.setVisible(busy);
            transferProgressIndicator.setManaged(busy);
        }
        if (transferStatusLabel != null) {
            transferStatusLabel.setText(statusText == null ? "" : statusText);
        }
    }

    private void syncTransferButtons() {
        boolean connected = isRemoteConnected();
        boolean localSelected = localWorldsList != null && localWorldsList.getSelectionModel().getSelectedItem() != null;
        boolean remoteSelected = remoteWorldsList != null && remoteWorldsList.getSelectionModel().getSelectedItem() != null;
        if (uploadButton != null) {
            uploadButton.setDisable(transferBusy || !connected || !localSelected);
        }
        if (downloadButton != null) {
            downloadButton.setDisable(transferBusy || !connected || !remoteSelected);
        }
        if (refreshButton != null) {
            refreshButton.setDisable(transferBusy);
        }
    }

    protected boolean isRemoteConnected() {
        return sshConnectionService.isConnected();
    }

    private void runTransferAsync(String runningText, String successText, Runnable transferWork) {
        if (transferBusy) {
            return;
        }
        setTransferBusy(true, runningText);
        runAsync(() -> {
            String finalStatus = successText;
            try {
                transferWork.run();
            } catch (Exception exception) {
                String message = exception.getMessage();
                if (message == null || message.isBlank()) {
                    finalStatus = "Transfer failed.";
                } else {
                    finalStatus = "Transfer failed: " + message;
                }
            }
            String status = finalStatus;
            Platform.runLater(() -> setTransferBusy(false, status));
        });
    }

    private void openWorldDetailsWindow(WorldEntry world) {
        Stage stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("World Details - " + displayName(world));
        FXMLLoader loader = new FXMLLoader(
                MainController.class.getResource("/io/worldportal/app/world-details-view.fxml")
        );
        VBox root;
        try {
            root = loader.load();
        } catch (IOException exception) {
            return;
        }
        Map<String, Object> namespace = loader.getNamespace();
        HBox appBar = requiredNode(namespace, "detailsWindowDragBar", HBox.class);
        Button minimizeButton = requiredNode(namespace, "detailsWindowMinimizeButton", Button.class);
        Button closeButton = requiredNode(namespace, "detailsWindowCloseButton", Button.class);
        Label nameLabel = requiredNode(namespace, "detailsNameLabel", Label.class);
        Label folderLabel = requiredNode(namespace, "detailsFolderLabel", Label.class);
        Label gameModeLabel = requiredNode(namespace, "detailsGameModeLabel", Label.class);
        Label patchLabel = requiredNode(namespace, "detailsPatchLabel", Label.class);
        Label lastPlayedLabel = requiredNode(namespace, "detailsLastPlayedLabel", Label.class);
        Label whitelistTitle = requiredNode(namespace, "detailsWhitelistTitle", Label.class);
        CheckBox enabledCheckBox = requiredNode(namespace, "detailsWhitelistEnabledCheckBox", CheckBox.class);
        ListView<String> playerUuids = requiredStringListView(namespace, "detailsWhitelistList");
        TextField uuidInput = requiredNode(namespace, "detailsUuidInput", TextField.class);
        Button addPlayerButton = requiredNode(namespace, "detailsAddUuidButton", Button.class);
        Button removePlayerButton = requiredNode(namespace, "detailsRemoveUuidButton", Button.class);
        Button saveWhitelistButton = requiredNode(namespace, "detailsSaveWhitelistButton", Button.class);
        Label whitelistStatus = requiredNode(namespace, "detailsWhitelistStatusLabel", Label.class);

        nameLabel.setText("Name: " + displayName(world));
        folderLabel.setText("Folder: " + valueOrUnknown(world.getId()));
        gameModeLabel.setText("GameMode: " + valueOrUnknown(world.getGameMode()));
        patchLabel.setText("Patch: " + valueOrUnknown(world.getPatchLine()));
        lastPlayedLabel.setText("Last played: " + formatLastPlayed(world.getLastModified()));

        Scene detailsScene = new Scene(root, 520, 520);
        applyWorldDetailsTheme(
                detailsScene,
                root,
                List.of(nameLabel, folderLabel, gameModeLabel, patchLabel, lastPlayedLabel),
                whitelistTitle,
                appBar,
                minimizeButton,
                closeButton,
                enabledCheckBox,
                playerUuids,
                uuidInput,
                addPlayerButton,
                removePlayerButton,
                saveWhitelistButton,
                whitelistStatus
        );
        configureDetailsWindowFrame(stage, appBar, minimizeButton, closeButton);

        Path localPath = world.getPath() == null ? null : Paths.get(world.getPath());
        boolean editable = localPath != null && Files.isDirectory(localPath);

        if (!editable) {
            enabledCheckBox.setDisable(true);
            playerUuids.setDisable(true);
            uuidInput.setDisable(true);
            addPlayerButton.setDisable(true);
            removePlayerButton.setDisable(true);
            saveWhitelistButton.setDisable(true);
            whitelistStatus.setText("Whitelist editing is only available for local world folders.");
        } else {
            loadWhitelistIntoDialog(localPath, enabledCheckBox, playerUuids, whitelistStatus);

            addPlayerButton.setOnAction(event -> {
                String uuid = uuidInput.getText() == null ? "" : uuidInput.getText().trim();
                if (uuid.isBlank() || !isLikelyUuid(uuid)) {
                    whitelistStatus.setText("Invalid UUID format.");
                    return;
                }
                if (!playerUuids.getItems().contains(uuid)) {
                    playerUuids.getItems().add(uuid);
                }
                uuidInput.clear();
                whitelistStatus.setText("");
            });

            removePlayerButton.setOnAction(event -> {
                String selected = playerUuids.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    playerUuids.getItems().remove(selected);
                }
            });

            saveWhitelistButton.setOnAction(event -> {
                try {
                    whitelistService.save(
                            localPath,
                            new WhitelistService.WhitelistConfig(enabledCheckBox.isSelected(),
                                    new ArrayList<>(playerUuids.getItems())));
                    whitelistStatus.setText("Whitelist saved.");
                } catch (IOException exception) {
                    whitelistStatus.setText("Failed to save whitelist: " + exception.getMessage());
                }
            });
        }

        stage.setScene(detailsScene);
        stage.show();
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

    @SuppressWarnings("unchecked")
    private static ListView<String> requiredStringListView(Map<String, Object> namespace, String id) {
        return (ListView<String>) requiredNode(namespace, id, ListView.class);
    }

    static void applyWorldDetailsTheme(
            Scene scene,
            VBox root,
            List<Label> detailLabels,
            Label whitelistTitle,
            HBox appBar,
            Button minimizeButton,
            Button closeButton,
            CheckBox enabledCheckBox,
            ListView<String> playerUuids,
            TextField uuidInput,
            Button addPlayerButton,
            Button removePlayerButton,
            Button saveWhitelistButton,
            Label whitelistStatus) {
        String stylesheet = MainController.class.getResource("/io/worldportal/app/main-view.css").toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }

        root.getStyleClass().add("app-root");
        root.getStyleClass().add("details-root");
        appBar.getStyleClass().add("window-drag-bar");
        minimizeButton.getStyleClass().add("window-button");
        closeButton.getStyleClass().add("window-close-square-button");

        if (!detailLabels.isEmpty()) {
            detailLabels.get(0).getStyleClass().add("panel-title");
        }
        for (int i = 1; i < detailLabels.size(); i++) {
            detailLabels.get(i).getStyleClass().add("field-label");
        }

        whitelistTitle.getStyleClass().add("panel-title");
        enabledCheckBox.getStyleClass().add("details-checkbox");
        playerUuids.getStyleClass().add("worlds-list");
        uuidInput.getStyleClass().add("neon-input");
        addPlayerButton.getStyleClass().add("action-button");
        removePlayerButton.getStyleClass().add("action-button");
        saveWhitelistButton.getStyleClass().add("action-button");
        whitelistStatus.getStyleClass().add("status-label");
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

    private void loadWhitelistIntoDialog(
            Path worldPath,
            CheckBox enabledCheckBox,
            ListView<String> playerUuids,
            Label statusLabel) {
        try {
            WhitelistService.WhitelistConfig config = whitelistService.load(worldPath);
            enabledCheckBox.setSelected(config.enabled());
            playerUuids.setItems(FXCollections.observableArrayList(config.playerUuids()));
            statusLabel.setText("");
        } catch (IOException exception) {
            enabledCheckBox.setSelected(true);
            playerUuids.setItems(FXCollections.observableArrayList());
            statusLabel.setText("Failed to load whitelist: " + exception.getMessage());
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

    private boolean isLikelyUuid(String uuid) {
        return uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    private void applyCachedConnectionSettings() {
        RemoteProfile cached = connectionSettingsStore.load();
        if (cached == null) {
            return;
        }

        if (cached.getHost() != null && !cached.getHost().isBlank()) {
            hostField.setText(cached.getHost());
        }
        if (cached.getPort() > 0) {
            portField.setText(Integer.toString(cached.getPort()));
        }
        if (cached.getUsername() != null && !cached.getUsername().isBlank()) {
            usernameField.setText(cached.getUsername());
        }
        if (cached.getRemoteBasePath() != null && !cached.getRemoteBasePath().isBlank()) {
            remotePathField.setText(cached.getRemoteBasePath());
        }
        if (cached.getLocalWorldsPath() != null && !cached.getLocalWorldsPath().isBlank()) {
            localWorldsPathField.setText(cached.getLocalWorldsPath());
        }
        if (cached.getPassword() != null && !cached.getPassword().isBlank()) {
            passwordField.setText(cached.getPassword());
        }

        String authType = cached.getAuthType();
        if (authType != null && !authType.isBlank()) {
            if (!authTypeCombo.getItems().contains(authType)) {
                authTypeCombo.getItems().add(authType);
            }
            authTypeCombo.getSelectionModel().select(authType);
        }

        String publicKeyFile = cached.getPublicKeyFilePath();
        if (publicKeyFile != null && !publicKeyFile.isBlank()) {
            if (!publicKeyFileCombo.getItems().contains(publicKeyFile)) {
                publicKeyFileCombo.getItems().add(publicKeyFile);
            }
            publicKeyFileCombo.getSelectionModel().select(publicKeyFile);
        }
    }

    private List<String> loadPublicKeyFiles() {
        Path sshDir = Paths.get(System.getProperty("user.home", ""), ".ssh");
        if (!Files.isDirectory(sshDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(sshDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".pub"))
                    .map(Path::toString)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
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

    private void maybeLoadRemotePreviewAsync(WorldEntry world) {
        if (world == null || world.getId() == null || world.getId().isBlank()) {
            return;
        }
        if (world.getPreviewImagePath() != null && !world.getPreviewImagePath().isBlank()) {
            File preview = new File(world.getPreviewImagePath());
            if (preview.exists() && preview.length() > 0) {
                return;
            }
        }

        String key = world.getId();
        if (remotePreviewLoading.putIfAbsent(key, Boolean.TRUE) != null) {
            return;
        }

        Thread loader = new Thread(() -> {
            try {
                String previewPath = worldService.downloadRemotePreview(world, buildRemoteProfile());
                if (previewPath != null && !previewPath.isBlank()) {
                    world.setPreviewImagePath(previewPath);
                    Platform.runLater(() -> remoteWorldsList.refresh());
                }
            } finally {
                remotePreviewLoading.remove(key);
            }
        }, "remote-preview-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private static class WorldCell extends ListCell<WorldEntry> {
        private static final DateTimeFormatter LAST_PLAYED_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault());

        private final ImageView previewImageView = new ImageView();
        private final Label nameLabel = new Label();
        private final Label metaLabel = new Label();
        private final Label lastPlayedLabel = new Label();
        private final Label sameAsLabel = new Label();
        private final VBox textContainer = new VBox(4.0, nameLabel, metaLabel, lastPlayedLabel, sameAsLabel);
        private final Button openDirectoryButton = new Button("Open Dir");
        private final HBox content = new HBox(10.0, previewImageView, textContainer, openDirectoryButton);
        private final boolean openDirectoryEnabled;

        private WorldCell(boolean openDirectoryEnabled) {
            this.openDirectoryEnabled = openDirectoryEnabled;
            previewImageView.setFitWidth(96);
            previewImageView.setFitHeight(54);
            previewImageView.setPreserveRatio(true);
            previewImageView.getStyleClass().add("world-preview");
            nameLabel.getStyleClass().add("world-name");
            metaLabel.getStyleClass().add("world-meta");
            lastPlayedLabel.getStyleClass().add("world-last-played");
            sameAsLabel.getStyleClass().add("world-same-as");
            textContainer.getStyleClass().add("world-text-container");
            content.getStyleClass().add("world-cell-content");
            content.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(textContainer, Priority.ALWAYS);
            getStyleClass().add("world-cell");
            openDirectoryButton.setText(null);
            openDirectoryButton.setGraphic(createFolderIcon());
            openDirectoryButton.setFocusTraversable(false);
            openDirectoryButton.getStyleClass().add("folder-button");
            openDirectoryButton.setDisable(!openDirectoryEnabled);
            openDirectoryButton.setOnAction(event -> {
                if (!openDirectoryEnabled) {
                    event.consume();
                    return;
                }
                WorldEntry currentItem = getItem();
                if (currentItem != null) {
                    openDirectory(currentItem.getPath());
                }
                event.consume();
            });
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(WorldEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            nameLabel.setText(displayNameWithFolder(item));
            metaLabel.setText("GameMode: " + valueOrUnknown(item.getGameMode()) + "  |  Patch: "
                    + valueOrUnknown(item.getPatchLine()));
            lastPlayedLabel.setText("Last played: " + formatLastPlayed(item.getLastModified()));

            String sameAsText = sameAsText(item, openDirectoryEnabled ? "remote" : "local");
            boolean hasSameAs = sameAsText != null && !sameAsText.isBlank();
            sameAsLabel.setText(hasSameAs ? sameAsText : "");
            sameAsLabel.setVisible(hasSameAs);
            sameAsLabel.setManaged(hasSameAs);

            if (item.getPreviewImagePath() != null && !item.getPreviewImagePath().isBlank()) {
                File previewImage = new File(item.getPreviewImagePath());
                if (previewImage.exists()) {
                    previewImageView.setImage(new Image(previewImage.toURI().toString(), true));
                } else {
                    previewImageView.setImage(null);
                }
            } else {
                previewImageView.setImage(null);
            }

            setGraphic(content);
        }

        private String formatLastPlayed(Instant value) {
            if (value == null || value.equals(Instant.EPOCH)) {
                return "Unknown";
            }
            return LAST_PLAYED_FORMATTER.format(value);
        }

        private String valueOrUnknown(String value) {
            if (value == null || value.isBlank()) {
                return "Unknown";
            }
            return value;
        }

        private String sameAsText(WorldEntry item, String otherSide) {
            List<WorldEntry> refs = item.getSameWorldReferences();
            if (refs == null || refs.isEmpty()) {
                return "";
            }
            String names = refs.stream()
                    .map(this::displayName)
                    .collect(Collectors.joining(", "));
            return "same as: " + otherSide + " " + names;
        }

        private String displayName(WorldEntry world) {
            if (world == null) {
                return "Unknown";
            }
            if (world.getName() != null && !world.getName().isBlank()) {
                return world.getName();
            }
            if (world.getId() != null && !world.getId().isBlank()) {
                return world.getId();
            }
            return "Unknown";
        }

        private String displayNameWithFolder(WorldEntry item) {
            String worldName = item.getName() != null && !item.getName().isBlank() ? item.getName() : "Unknown";
            String folderName = item.getId() != null && !item.getId().isBlank() ? item.getId() : "Unknown";
            return worldName + " (" + folderName + ")";
        }

        private void openDirectory(String path) {
            if (path == null || path.isBlank()) {
                return;
            }
            File directory = new File(path);
            if (!directory.exists()) {
                return;
            }
            Thread openThread = new Thread(() -> {
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(directory);
                        return;
                    }
                    String osName = System.getProperty("os.name", "").toLowerCase();
                    if (osName.contains("win")) {
                        new ProcessBuilder("explorer.exe", directory.getAbsolutePath()).start();
                    } else if (osName.contains("mac")) {
                        new ProcessBuilder("open", directory.getAbsolutePath()).start();
                    } else {
                        new ProcessBuilder("xdg-open", directory.getAbsolutePath()).start();
                    }
                } catch (IOException ignored) {
                }
            }, "open-world-directory");
            openThread.setDaemon(true);
            openThread.start();
        }

        private Group createFolderIcon() {
            Rectangle tab = new Rectangle(2, 2, 7, 4);
            tab.setArcWidth(2);
            tab.setArcHeight(2);
            tab.setFill(Color.web("#f0c56a"));
            tab.setStroke(Color.web("#b2872f"));

            Rectangle body = new Rectangle(1, 5, 14, 9);
            body.setArcWidth(2);
            body.setArcHeight(2);
            body.setFill(Color.web("#f5cf7b"));
            body.setStroke(Color.web("#b2872f"));

            return new Group(tab, body);
        }
    }
}
