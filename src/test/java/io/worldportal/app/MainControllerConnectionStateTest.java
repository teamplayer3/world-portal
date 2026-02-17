package io.worldportal.app;

import io.worldportal.app.ui.MainController;
import io.worldportal.app.model.WorldEntry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainControllerConnectionStateTest {
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
    void busyStateDisablesRemoteInputsAndShowsLoadingIndicator() throws Exception {
        Assumptions.assumeTrue(javaFxAvailable, "JavaFX runtime is not available in this environment");
        MainController controller = new MainController();

        TextField hostField = new TextField();
        TextField portField = new TextField();
        TextField usernameField = new TextField();
        TextField remotePathField = new TextField();
        ComboBox<String> authTypeCombo = new ComboBox<>(FXCollections.observableArrayList("Password", "Public Key"));
        PasswordField passwordField = new PasswordField();
        ComboBox<String> publicKeyFileCombo = new ComboBox<>();
        Label passwordLabel = new Label();
        Label publicKeyFileLabel = new Label();
        Button refreshKeysButton = new Button();
        HBox publicKeyContainer = new HBox();
        Button connectButton = new Button();
        ProgressIndicator connectLoadingIndicator = new ProgressIndicator();

        setField(controller, "hostField", hostField);
        setField(controller, "portField", portField);
        setField(controller, "usernameField", usernameField);
        setField(controller, "remotePathField", remotePathField);
        setField(controller, "authTypeCombo", authTypeCombo);
        setField(controller, "passwordField", passwordField);
        setField(controller, "publicKeyFileCombo", publicKeyFileCombo);
        setField(controller, "passwordLabel", passwordLabel);
        setField(controller, "publicKeyFileLabel", publicKeyFileLabel);
        setField(controller, "refreshKeysButton", refreshKeysButton);
        setField(controller, "publicKeyContainer", publicKeyContainer);
        setField(controller, "connectButton", connectButton);
        setField(controller, "connectLoadingIndicator", connectLoadingIndicator);

        runOnFxThreadAndWait(() -> {
            authTypeCombo.getSelectionModel().select("Password");
            invokeBusyState(controller, false);

            assertFalse(connectButton.isDisable());
            assertFalse(hostField.isDisable());
            assertFalse(passwordField.isDisable());
            assertFalse(connectLoadingIndicator.isVisible());

            invokeBusyState(controller, true);

            assertTrue(connectButton.isDisable());
            assertTrue(hostField.isDisable());
            assertTrue(portField.isDisable());
            assertTrue(usernameField.isDisable());
            assertTrue(remotePathField.isDisable());
            assertTrue(authTypeCombo.isDisable());
            assertTrue(passwordField.isDisable());
            assertTrue(connectLoadingIndicator.isVisible());
        });
    }

    @Test
    void transferBusyStateDisablesTransferButtonsAndShowsProgressIndicator() throws Exception {
        Assumptions.assumeTrue(javaFxAvailable, "JavaFX runtime is not available in this environment");
        MainController controller = new MainController();

        Button uploadButton = new Button();
        Button downloadButton = new Button();
        Button refreshButton = new Button();
        ProgressIndicator transferProgressIndicator = new ProgressIndicator();
        Label transferStatusLabel = new Label();

        setField(controller, "uploadButton", uploadButton);
        setField(controller, "downloadButton", downloadButton);
        setField(controller, "refreshButton", refreshButton);
        setField(controller, "transferProgressIndicator", transferProgressIndicator);
        setField(controller, "transferStatusLabel", transferStatusLabel);

        runOnFxThreadAndWait(() -> {
            invokeTransferBusyState(controller, false);

            assertTrue(uploadButton.isDisable());
            assertTrue(downloadButton.isDisable());
            assertFalse(refreshButton.isDisable());
            assertFalse(transferProgressIndicator.isVisible());

            invokeTransferBusyState(controller, true);

            assertTrue(uploadButton.isDisable());
            assertTrue(downloadButton.isDisable());
            assertTrue(refreshButton.isDisable());
            assertTrue(transferProgressIndicator.isVisible());
        });
    }

    @Test
    void transferButtonsRequireMatchingWorldSelection() throws Exception {
        Assumptions.assumeTrue(javaFxAvailable, "JavaFX runtime is not available in this environment");
        MainController controller = new MainController() {
            @Override
            protected boolean isRemoteConnected() {
                return true;
            }
        };

        Button uploadButton = new Button();
        Button downloadButton = new Button();
        Button refreshButton = new Button();
        ProgressIndicator transferProgressIndicator = new ProgressIndicator();
        Label transferStatusLabel = new Label();
        ListView<Object> localWorldsList = new ListView<>();
        ListView<Object> remoteWorldsList = new ListView<>();

        setField(controller, "uploadButton", uploadButton);
        setField(controller, "downloadButton", downloadButton);
        setField(controller, "refreshButton", refreshButton);
        setField(controller, "transferProgressIndicator", transferProgressIndicator);
        setField(controller, "transferStatusLabel", transferStatusLabel);
        setField(controller, "localWorldsList", localWorldsList);
        setField(controller, "remoteWorldsList", remoteWorldsList);

        runOnFxThreadAndWait(() -> {
            localWorldsList.getItems().add(new Object());
            remoteWorldsList.getItems().add(new Object());

            invokeTransferBusyState(controller, false);
            assertTrue(uploadButton.isDisable());
            assertTrue(downloadButton.isDisable());

            localWorldsList.getSelectionModel().select(0);
            invokeTransferBusyState(controller, false);
            assertFalse(uploadButton.isDisable());
            assertTrue(downloadButton.isDisable());

            remoteWorldsList.getSelectionModel().select(0);
            invokeTransferBusyState(controller, false);
            assertFalse(uploadButton.isDisable());
            assertFalse(downloadButton.isDisable());
        });
    }

    @Test
    void worldCellExposesDirectionalTransferButtonPerItem() throws Exception {
        Assumptions.assumeTrue(javaFxAvailable, "JavaFX runtime is not available in this environment");
        runOnFxThreadAndWait(() -> {
            assertTrue(worldCellContainsTransferButton(">>"));
            assertTrue(worldCellContainsTransferButton("<<"));
        });
    }

    @Test
    void worldCellMetadataDoesNotShowPatchInfo() throws Exception {
        Assumptions.assumeTrue(javaFxAvailable, "JavaFX runtime is not available in this environment");
        runOnFxThreadAndWait(() -> {
            String metadataText = worldCellMetaLabelText();
            assertTrue(metadataText.contains("GameMode: Survival"));
            assertFalse(metadataText.contains("Patch:"));
        });
    }

    @Test
    void worldCellTransferButtonUsesExpectedTooltipTextAndFastShowDelay() throws Exception {
        Assumptions.assumeTrue(javaFxAvailable, "JavaFX runtime is not available in this environment");
        runOnFxThreadAndWait(() -> {
            Tooltip uploadTooltip = worldCellTransferButtonTooltip(">>", true);
            Tooltip downloadTooltip = worldCellTransferButtonTooltip("<<", true);

            assertEquals("Upload", uploadTooltip.getText());
            assertEquals("Download", downloadTooltip.getText());
            assertTrue(uploadTooltip.getShowDelay().toMillis() <= 250.0);
            assertTrue(downloadTooltip.getShowDelay().toMillis() <= 250.0);
        });
    }

    private static void invokeBusyState(MainController controller, boolean busy) {
        try {
            Method method = MainController.class.getDeclaredMethod("setRemoteConnectionBusy", boolean.class);
            method.setAccessible(true);
            method.invoke(controller, busy);
        } catch (ReflectiveOperationException reflectionFailure) {
            throw new AssertionError(reflectionFailure);
        }
    }

    private static void invokeTransferBusyState(MainController controller, boolean busy) {
        try {
            Method method = MainController.class.getDeclaredMethod("setTransferBusy", boolean.class, String.class);
            method.setAccessible(true);
            method.invoke(controller, busy, "");
        } catch (ReflectiveOperationException reflectionFailure) {
            throw new AssertionError(reflectionFailure);
        }
    }

    private static boolean worldCellContainsTransferButton(String expectedText) {
        try {
            Class<?> worldCellClass = findWorldCellClass();
            Constructor<?> constructor = worldCellClass.getDeclaredConstructor(
                    boolean.class,
                    String.class,
                    String.class,
                    Consumer.class,
                    BooleanSupplier.class);
            constructor.setAccessible(true);

            Object cell = constructor.newInstance(
                    true,
                    expectedText,
                    "tooltip",
                    (Consumer<WorldEntry>) world -> {
                    },
                    (BooleanSupplier) () -> true);

            Method updateItem = worldCellClass.getDeclaredMethod("updateItem", WorldEntry.class, boolean.class);
            updateItem.setAccessible(true);
            updateItem.invoke(cell,
                    new WorldEntry("folder", "Name", "/tmp/world", null, "Survival", "1.0", Instant.now()), false);

            Method getGraphic = cell.getClass().getSuperclass().getMethod("getGraphic");
            Object graphic = getGraphic.invoke(cell);
            if (!(graphic instanceof HBox hBox)) {
                return false;
            }

            for (Node child : hBox.getChildren()) {
                if (child instanceof Button button && expectedText.equals(button.getText())) {
                    return true;
                }
            }
            return false;
        } catch (ReflectiveOperationException reflectionFailure) {
            throw new AssertionError(reflectionFailure);
        }
    }

    private static Class<?> findWorldCellClass() {
        for (Class<?> nestedClass : MainController.class.getDeclaredClasses()) {
            if ("WorldCell".equals(nestedClass.getSimpleName())) {
                return nestedClass;
            }
        }
        throw new AssertionError("WorldCell nested class not found");
    }

    private static String worldCellMetaLabelText() {
        try {
            Class<?> worldCellClass = findWorldCellClass();
            Constructor<?> constructor = worldCellClass.getDeclaredConstructor(
                    boolean.class,
                    String.class,
                    String.class,
                    Consumer.class,
                    BooleanSupplier.class);
            constructor.setAccessible(true);

            Object cell = constructor.newInstance(
                    true,
                    ">>",
                    "Upload",
                    (Consumer<WorldEntry>) world -> {
                    },
                    (BooleanSupplier) () -> true);

            Method updateItem = worldCellClass.getDeclaredMethod("updateItem", WorldEntry.class, boolean.class);
            updateItem.setAccessible(true);
            updateItem.invoke(cell,
                    new WorldEntry("folder", "Name", "/tmp/world", null, "Survival", "1.0", Instant.now()), false);

            Field metaLabelField = worldCellClass.getDeclaredField("metaLabel");
            metaLabelField.setAccessible(true);
            Label metaLabel = (Label) metaLabelField.get(cell);
            return metaLabel.getText();
        } catch (ReflectiveOperationException reflectionFailure) {
            throw new AssertionError(reflectionFailure);
        }
    }

    private static Tooltip worldCellTransferButtonTooltip(String transferButtonText, boolean enabled) {
        try {
            Class<?> worldCellClass = findWorldCellClass();
            Constructor<?> constructor = worldCellClass.getDeclaredConstructor(
                    boolean.class,
                    String.class,
                    String.class,
                    Consumer.class,
                    BooleanSupplier.class);
            constructor.setAccessible(true);

            Object cell = constructor.newInstance(
                    true,
                    transferButtonText,
                    ">>".equals(transferButtonText) ? "Upload" : "Download",
                    (Consumer<WorldEntry>) world -> {
                    },
                    (BooleanSupplier) () -> enabled);

            Method updateItem = worldCellClass.getDeclaredMethod("updateItem", WorldEntry.class, boolean.class);
            updateItem.setAccessible(true);
            updateItem.invoke(cell,
                    new WorldEntry("folder", "Name", "/tmp/world", null, "Survival", "1.0", Instant.now()), false);

            Field transferButtonField = worldCellClass.getDeclaredField("transferButton");
            transferButtonField.setAccessible(true);
            Button transferButton = (Button) transferButtonField.get(cell);
            return transferButton.getTooltip();
        } catch (ReflectiveOperationException reflectionFailure) {
            throw new AssertionError(reflectionFailure);
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static void runOnFxThreadAndWait(Runnable runnable) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }
}
