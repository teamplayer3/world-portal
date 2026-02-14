package io.worldportal.app.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldDetailsDialogThemeTest {
    private static volatile boolean javaFxAvailable = true;

    @BeforeAll
    static void startJavaFxRuntime() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        } catch (UnsupportedOperationException headlessEnvironment) {
            javaFxAvailable = false;
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void detailsDialogUsesSameThemeClassesAndStylesheet() throws InterruptedException {
        Assumptions.assumeTrue(javaFxAvailable, "JavaFX runtime is not available in this environment");

        runOnFxThreadAndWait(() -> {
            VBox root = new VBox();
            Label nameLabel = new Label("Name");
            Label folderLabel = new Label("Folder");
            Label gameModeLabel = new Label("Mode");
            Label patchLabel = new Label("Patch");
            Label lastPlayedLabel = new Label("Last");
            Label whitelistTitle = new Label("Whitelist");
            HBox appBar = new HBox();
            Button minimizeButton = new Button("—");
            Button closeButton = new Button("✕");
            CheckBox enabledCheckBox = new CheckBox("Whitelist enabled");
            ListView<String> playerUuids = new ListView<>();
            TextField uuidInput = new TextField();
            Button addPlayerButton = new Button("Add UUID");
            Button removePlayerButton = new Button("Remove Selected");
            Button saveWhitelistButton = new Button("Save Whitelist");
            Label whitelistStatus = new Label();
            Scene scene = new Scene(root, 520, 520);

            MainController.applyWorldDetailsTheme(
                    scene,
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

            assertTrue(scene.getStylesheets().stream().anyMatch(path -> path.endsWith("/io/worldportal/app/main-view.css")));
            assertTrue(root.getStyleClass().contains("app-root"));
            assertTrue(root.getStyleClass().contains("details-root"));
            assertTrue(appBar.getStyleClass().contains("window-drag-bar"));
            assertTrue(minimizeButton.getStyleClass().contains("window-button"));
            assertTrue(closeButton.getStyleClass().contains("window-close-square-button"));
            assertTrue(nameLabel.getStyleClass().contains("panel-title"));
            assertTrue(folderLabel.getStyleClass().contains("field-label"));
            assertTrue(playerUuids.getStyleClass().contains("worlds-list"));
            assertTrue(uuidInput.getStyleClass().contains("neon-input"));
            assertTrue(addPlayerButton.getStyleClass().contains("action-button"));
            assertTrue(removePlayerButton.getStyleClass().contains("action-button"));
            assertTrue(saveWhitelistButton.getStyleClass().contains("action-button"));
            assertTrue(whitelistStatus.getStyleClass().contains("status-label"));
        });
    }

    private static void runOnFxThreadAndWait(Runnable runnable) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                runnable.run();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
