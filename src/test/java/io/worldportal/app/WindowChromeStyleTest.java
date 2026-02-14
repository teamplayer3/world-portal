package io.worldportal.app;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowChromeStyleTest {
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
    void appWindowUsesUndecoratedStageStyle() throws InterruptedException {
        Assumptions.assumeTrue(javaFxAvailable, "JavaFX runtime is not available in this environment");

        AtomicReference<StageStyle> stageStyle = new AtomicReference<>();
        runOnFxThreadAndWait(() -> {
            Stage stage = new Stage();
            WorldPortalApplication.applyWindowStyle(stage);
            stageStyle.set(stage.getStyle());
            stage.hide();
        });

        assertEquals(StageStyle.UNDECORATED, stageStyle.get());
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
