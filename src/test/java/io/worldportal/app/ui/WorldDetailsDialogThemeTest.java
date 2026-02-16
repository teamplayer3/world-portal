package io.worldportal.app.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            Scene scene = new Scene(root, 520, 520);

            MainController.applyWorldDetailsTheme(scene);

            assertTrue(scene.getStylesheets().stream().anyMatch(path -> path.endsWith("/io/worldportal/app/main-view.css")));
            assertEquals(Color.TRANSPARENT, scene.getFill());
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
