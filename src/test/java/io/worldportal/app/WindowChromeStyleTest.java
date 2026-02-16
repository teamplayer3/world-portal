package io.worldportal.app;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
    void appWindowUsesTransparentStageStyle() throws InterruptedException {
        Assumptions.assumeTrue(javaFxAvailable, "JavaFX runtime is not available in this environment");

        AtomicReference<StageStyle> stageStyle = new AtomicReference<>();
        runOnFxThreadAndWait(() -> {
            Stage stage = new Stage();
            WorldPortalApplication.applyWindowStyle(stage);
            stageStyle.set(stage.getStyle());
            stage.hide();
        });

        assertEquals(StageStyle.TRANSPARENT, stageStyle.get());
    }

    @Test
    void windowFrameUsesTransparentSceneFillForRoundedCorners() throws InterruptedException {
        Assumptions.assumeTrue(javaFxAvailable, "JavaFX runtime is not available in this environment");

        AtomicReference<Color> sceneFill = new AtomicReference<>();
        runOnFxThreadAndWait(() -> {
            Stage stage = new Stage();
            Scene scene = new Scene(new VBox(), 980, 620);
            WorldPortalApplication.configureWindowFrame(stage, scene);
            sceneFill.set((Color) scene.getFill());
            stage.hide();
        });

        assertEquals(Color.TRANSPARENT, sceneFill.get());
    }

    @Test
    void windowFrameSetsMainWindowMinimumSizeTo930By520() throws InterruptedException {
        Assumptions.assumeTrue(javaFxAvailable, "JavaFX runtime is not available in this environment");

        AtomicReference<Double> minWidth = new AtomicReference<>();
        AtomicReference<Double> minHeight = new AtomicReference<>();
        runOnFxThreadAndWait(() -> {
            Stage stage = new Stage();
            Scene scene = new Scene(new VBox(), 980, 620);
            WorldPortalApplication.configureWindowFrame(stage, scene);
            minWidth.set(stage.getMinWidth());
            minHeight.set(stage.getMinHeight());
            stage.hide();
        });

        assertEquals(930.0, minWidth.get());
        assertEquals(520.0, minHeight.get());
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
