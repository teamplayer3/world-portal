package io.worldportal.app;

import io.worldportal.app.service.TransferService;
import io.worldportal.app.service.WorldService;
import io.worldportal.app.service.impl.StubTransferService;
import io.worldportal.app.service.impl.StubWorldService;
import io.worldportal.app.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class WorldPortalApplication extends Application {

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
        stage.setTitle("world-portal");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
