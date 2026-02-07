package io.worldportal.app;

import io.worldportal.app.ui.MainController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

            assertFalse(uploadButton.isDisable());
            assertFalse(downloadButton.isDisable());
            assertFalse(refreshButton.isDisable());
            assertFalse(transferProgressIndicator.isVisible());

            invokeTransferBusyState(controller, true);

            assertTrue(uploadButton.isDisable());
            assertTrue(downloadButton.isDisable());
            assertTrue(refreshButton.isDisable());
            assertTrue(transferProgressIndicator.isVisible());
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

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
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
