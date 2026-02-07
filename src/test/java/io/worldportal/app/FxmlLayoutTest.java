package io.worldportal.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FxmlLayoutTest {

    @Test
    void topSectionUsesLeftRightLocalAndRemoteColumnsWithExpectedOrder() throws IOException {
        String fxml = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(fxml.contains("text=\"Local\""));
        assertTrue(fxml.contains("text=\"Remote Connection\""));
        assertTrue(fxml.contains("fx:id=\"publicKeyContainer\""));
        assertTrue(fxml.contains("fx:id=\"connectButton\""));
        assertTrue(fxml.contains("fx:id=\"connectLoadingIndicator\""));

        int hostIndex = fxml.indexOf("fx:id=\"hostField\"");
        int portIndex = fxml.indexOf("fx:id=\"portField\"");
        int usernameIndex = fxml.indexOf("fx:id=\"usernameField\"");
        int authTypeIndex = fxml.indexOf("fx:id=\"authTypeCombo\"");
        int passwordIndex = fxml.indexOf("fx:id=\"passwordField\"");
        int publicKeyIndex = fxml.indexOf("fx:id=\"publicKeyFileCombo\"");
        int remotePathIndex = fxml.indexOf("fx:id=\"remotePathField\"");
        int connectButtonIndex = fxml.indexOf("onAction=\"#onConnect\"");

        assertTrue(hostIndex >= 0 && portIndex > hostIndex);
        assertTrue(usernameIndex > portIndex);
        assertTrue(authTypeIndex > usernameIndex);
        assertTrue(passwordIndex > authTypeIndex || publicKeyIndex > authTypeIndex);
        assertTrue(remotePathIndex > authTypeIndex);
        assertTrue(connectButtonIndex > remotePathIndex);
    }

    @Test
    void bottomSectionContainsTransferProgressAndStatusElements() throws IOException {
        String fxml = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(fxml.contains("fx:id=\"transferProgressIndicator\""));
        assertTrue(fxml.contains("fx:id=\"transferStatusLabel\""));
        assertTrue(fxml.contains("fx:id=\"uploadButton\""));
        assertTrue(fxml.contains("fx:id=\"downloadButton\""));
        assertTrue(fxml.contains("fx:id=\"refreshButton\""));
    }
}
