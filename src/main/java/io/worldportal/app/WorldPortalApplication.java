package io.worldportal.app;

import io.worldportal.app.service.TransferService;
import io.worldportal.app.service.WorldService;
import io.worldportal.app.service.impl.StubTransferService;
import io.worldportal.app.service.impl.StubWorldService;
import io.worldportal.app.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WorldPortalApplication extends Application {
    private static final double RESIZE_MARGIN = 6.0;
    private static final double MIN_WINDOW_WIDTH = 930.0;
    private static final double MIN_WINDOW_HEIGHT = 520.0;

    @Override
    public void start(Stage stage) throws Exception {
        WorldService worldService = new StubWorldService();
        TransferService transferService = new StubTransferService();

        FXMLLoader loader = new FXMLLoader(
                WorldPortalApplication.class.getResource("/io/worldportal/app/main-view.fxml")
        );
        loader.setControllerFactory(type -> {
            if (type == MainController.class) {
                return new MainController(worldService, transferService);
            }
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception exception) {
                throw new IllegalStateException("Unable to create controller: " + type.getName(), exception);
            }
        });

        Scene scene = new Scene(loader.load(), 980, 620);
        applyWindowStyle(stage);
        configureWindowFrame(stage, scene);
        stage.setTitle("world-portal");
        stage.setScene(scene);
        stage.show();
    }

    static void applyWindowStyle(Stage stage) {
        stage.initStyle(StageStyle.TRANSPARENT);
    }

    static void configureWindowFrame(Stage stage, Scene scene) {
        scene.setFill(Color.TRANSPARENT);
        stage.setMinWidth(MIN_WINDOW_WIDTH);
        stage.setMinHeight(MIN_WINDOW_HEIGHT);

        Parent root = scene.getRoot();
        bindWindowControls(root, stage);
        bindWindowDrag(root, stage);
        bindWindowResize(stage, scene);
    }

    private static void bindWindowControls(Parent root, Stage stage) {
        Node minimizeNode = root.lookup("#minimizeButton");
        if (minimizeNode instanceof Button minimizeButton) {
            minimizeButton.setOnAction(event -> stage.setIconified(true));
        }

        Node closeNode = root.lookup("#closeButton");
        if (closeNode instanceof Button closeButton) {
            closeButton.setOnAction(event -> stage.close());
        }
    }

    private static void bindWindowDrag(Parent root, Stage stage) {
        Node dragBar = root.lookup("#windowDragBar");
        if (dragBar == null) {
            return;
        }

        final Delta dragDelta = new Delta();
        dragBar.setOnMousePressed(event -> {
            if (event.isPrimaryButtonDown()) {
                dragDelta.x = stage.getX() - event.getScreenX();
                dragDelta.y = stage.getY() - event.getScreenY();
            }
        });
        dragBar.setOnMouseDragged(event -> {
            if (event.isPrimaryButtonDown() && sceneCursorIsDefault(stage.getScene())) {
                stage.setX(event.getScreenX() + dragDelta.x);
                stage.setY(event.getScreenY() + dragDelta.y);
            }
        });
    }

    private static boolean sceneCursorIsDefault(Scene scene) {
        return scene == null || scene.getCursor() == null || scene.getCursor() == Cursor.DEFAULT;
    }

    private static void bindWindowResize(Stage stage, Scene scene) {
        final Delta start = new Delta();
        final ResizeState resizeState = new ResizeState();

        scene.setOnMouseMoved(event -> {
            Cursor cursor = computeCursor(scene, event.getSceneX(), event.getSceneY());
            scene.setCursor(cursor);
        });

        scene.setOnMousePressed(event -> {
            Cursor cursor = scene.getCursor();
            resizeState.cursor = cursor;
            resizeState.active = cursor != null && cursor != Cursor.DEFAULT;
            start.x = event.getScreenX();
            start.y = event.getScreenY();
            resizeState.stageX = stage.getX();
            resizeState.stageY = stage.getY();
            resizeState.width = stage.getWidth();
            resizeState.height = stage.getHeight();
        });

        scene.setOnMouseDragged(event -> {
            if (!resizeState.active || resizeState.cursor == null) {
                return;
            }
            double dx = event.getScreenX() - start.x;
            double dy = event.getScreenY() - start.y;
            applyResize(stage, resizeState, dx, dy);
        });

        scene.setOnMouseReleased(event -> resizeState.active = false);
    }

    private static Cursor computeCursor(Scene scene, double x, double y) {
        double width = scene.getWidth();
        double height = scene.getHeight();
        boolean left = x <= RESIZE_MARGIN;
        boolean right = x >= width - RESIZE_MARGIN;
        boolean top = y <= RESIZE_MARGIN;
        boolean bottom = y >= height - RESIZE_MARGIN;

        if (left && top) {
            return Cursor.NW_RESIZE;
        }
        if (right && top) {
            return Cursor.NE_RESIZE;
        }
        if (left && bottom) {
            return Cursor.SW_RESIZE;
        }
        if (right && bottom) {
            return Cursor.SE_RESIZE;
        }
        if (left) {
            return Cursor.W_RESIZE;
        }
        if (right) {
            return Cursor.E_RESIZE;
        }
        if (top) {
            return Cursor.N_RESIZE;
        }
        if (bottom) {
            return Cursor.S_RESIZE;
        }
        return Cursor.DEFAULT;
    }

    private static void applyResize(Stage stage, ResizeState resizeState, double dx, double dy) {
        Cursor cursor = resizeState.cursor;
        if (cursor == Cursor.E_RESIZE || cursor == Cursor.NE_RESIZE || cursor == Cursor.SE_RESIZE) {
            stage.setWidth(Math.max(stage.getMinWidth(), resizeState.width + dx));
        }
        if (cursor == Cursor.S_RESIZE || cursor == Cursor.SE_RESIZE || cursor == Cursor.SW_RESIZE) {
            stage.setHeight(Math.max(stage.getMinHeight(), resizeState.height + dy));
        }
        if (cursor == Cursor.W_RESIZE || cursor == Cursor.NW_RESIZE || cursor == Cursor.SW_RESIZE) {
            double width = Math.max(stage.getMinWidth(), resizeState.width - dx);
            stage.setWidth(width);
            stage.setX(resizeState.stageX + (resizeState.width - width));
        }
        if (cursor == Cursor.N_RESIZE || cursor == Cursor.NE_RESIZE || cursor == Cursor.NW_RESIZE) {
            double height = Math.max(stage.getMinHeight(), resizeState.height - dy);
            stage.setHeight(height);
            stage.setY(resizeState.stageY + (resizeState.height - height));
        }
    }

    private static class Delta {
        double x;
        double y;
    }

    private static class ResizeState {
        boolean active;
        Cursor cursor;
        double stageX;
        double stageY;
        double width;
        double height;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
