package io.worldportal.app.ui;

import io.worldportal.app.model.RemoteProfile;
import io.worldportal.app.model.WorldEntry;
import io.worldportal.app.service.TransferService;
import io.worldportal.app.service.WorldService;
import io.worldportal.app.service.impl.StubTransferService;
import io.worldportal.app.service.impl.StubWorldService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

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

    private final WorldService worldService;
    private final TransferService transferService;
    private final ObservableList<WorldEntry> localWorlds = FXCollections.observableArrayList();
    private final ObservableList<WorldEntry> remoteWorlds = FXCollections.observableArrayList();

    public MainController() {
        this(new StubWorldService(), new StubTransferService());
    }

    public MainController(WorldService worldService, TransferService transferService) {
        this.worldService = worldService;
        this.transferService = transferService;
    }

    @FXML
    private void initialize() {
        localWorldsList.setItems(localWorlds);
        localWorldsList.setCellFactory(listView -> new LocalWorldCell());
        remoteWorldsList.setItems(remoteWorlds);
        authTypeCombo.setItems(FXCollections.observableArrayList("Password", "Public Key"));
        authTypeCombo.getSelectionModel().selectFirst();
        authTypeCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateAuthInputState());
        publicKeyFileCombo.setItems(FXCollections.observableArrayList(loadPublicKeyFiles()));
        if (!publicKeyFileCombo.getItems().isEmpty()) {
            publicKeyFileCombo.getSelectionModel().selectFirst();
        }
        updateAuthInputState();
        portField.setText("22");
        localWorldsPathField.setText(defaultLocalWorldsPath());
        refreshLists();
    }

    @FXML
    private void onConnect() {
        runAsync(this::refreshRemoteWorlds);
    }

    @FXML
    private void onUpload() {
        WorldEntry selectedWorld = localWorldsList.getSelectionModel().getSelectedItem();
        if (selectedWorld == null) {
            return;
        }
        runAsync(() -> transferService.uploadWorld(selectedWorld, buildRemoteProfile()));
    }

    @FXML
    private void onDownload() {
        WorldEntry selectedWorld = remoteWorldsList.getSelectionModel().getSelectedItem();
        if (selectedWorld == null) {
            return;
        }
        runAsync(() -> {
            transferService.downloadWorld(selectedWorld, buildRemoteProfile());
            refreshLists();
        });
    }

    @FXML
    private void onRefresh() {
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
        Platform.runLater(() -> localWorlds.setAll(local));
        refreshRemoteWorlds();
    }

    private void refreshRemoteWorlds() {
        List<WorldEntry> remote = worldService.listRemoteWorlds(buildRemoteProfile());
        Platform.runLater(() -> remoteWorlds.setAll(remote));
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
                getConfiguredLocalWorldsPath()
        );
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
        boolean publicKeySelected = "Public Key".equalsIgnoreCase(authTypeCombo.getValue());
        passwordField.setVisible(!publicKeySelected);
        passwordField.setManaged(!publicKeySelected);
        passwordLabel.setVisible(!publicKeySelected);
        passwordLabel.setManaged(!publicKeySelected);

        publicKeyFileCombo.setVisible(publicKeySelected);
        publicKeyFileCombo.setManaged(publicKeySelected);
        publicKeyFileLabel.setVisible(publicKeySelected);
        publicKeyFileLabel.setManaged(publicKeySelected);
        refreshKeysButton.setVisible(publicKeySelected);
        refreshKeysButton.setManaged(publicKeySelected);
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

    private static class LocalWorldCell extends ListCell<WorldEntry> {
        private static final DateTimeFormatter LAST_PLAYED_FORMATTER =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

        private final ImageView previewImageView = new ImageView();
        private final Label nameLabel = new Label();
        private final Label metaLabel = new Label();
        private final Label lastPlayedLabel = new Label();
        private final VBox textContainer = new VBox(4.0, nameLabel, metaLabel, lastPlayedLabel);
        private final Button openDirectoryButton = new Button("Open Dir");
        private final HBox content = new HBox(10.0, previewImageView, textContainer, openDirectoryButton);

        private LocalWorldCell() {
            previewImageView.setFitWidth(96);
            previewImageView.setFitHeight(54);
            previewImageView.setPreserveRatio(true);
            content.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(textContainer, Priority.ALWAYS);
            openDirectoryButton.setText(null);
            openDirectoryButton.setGraphic(createFolderIcon());
            openDirectoryButton.setFocusTraversable(false);
            openDirectoryButton.setOnAction(event -> {
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
            metaLabel.setText("GameMode: " + valueOrUnknown(item.getGameMode()) + "  |  Patch: " + valueOrUnknown(item.getPatchLine()));
            lastPlayedLabel.setText("Last played: " + formatLastPlayed(item.getLastModified()));

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
