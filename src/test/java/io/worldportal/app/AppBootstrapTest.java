package io.worldportal.app;

import io.worldportal.app.ui.MainController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppBootstrapTest {

    @Test
    void applicationClassIsInstantiable() {
        WorldPortalApplication app = new WorldPortalApplication();
        assertNotNull(app);
    }

    @Test
    void controllerIsInstantiableWithDefaultServices() {
        MainController controller = new MainController();
        assertNotNull(controller);
        assertNotNull(controller.getWorldService());
        assertNotNull(controller.getTransferService());
    }
}
