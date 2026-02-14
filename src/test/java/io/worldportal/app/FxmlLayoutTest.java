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

    @Test
    void viewDefinesNeonThemeStylesheetAndKeyStyleClasses() throws IOException {
        String fxml = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(fxml.contains("stylesheets=\"@main-view.css\""));
        assertTrue(fxml.contains("styleClass=\"app-root\""));
        assertTrue(fxml.contains("styleClass=\"panel-title\""));
        assertTrue(fxml.contains("styleClass=\"worlds-list\""));
        assertTrue(fxml.contains("styleClass=\"action-button\""));
    }

    @Test
    void stylesheetUsesCompactControlSizing() throws IOException {
        String css = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.css").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(css.contains(".app-title {\n    -fx-text-fill: #7de7f5;\n    -fx-font-size: 20px;"));
        assertTrue(css.contains(".panel-title {\n    -fx-text-fill: #8bdeee;\n    -fx-font-size: 14px;"));
        assertTrue(css.contains(".field-label {\n    -fx-text-fill: #85d4e8;\n    -fx-font-size: 13px;"));
        assertTrue(css.contains("-fx-font-size: 13px;"));
        assertTrue(css.contains(".action-button {\n    -fx-border-width: 1;\n    -fx-font-size: 13px;"));
    }

    @Test
    void viewDefinesCustomWindowChromeControls() throws IOException {
        String fxml = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(fxml.contains("fx:id=\"windowDragBar\""));
        assertTrue(fxml.contains("fx:id=\"minimizeButton\""));
        assertTrue(fxml.contains("fx:id=\"closeButton\""));
    }

    @Test
    void closeAndRefreshUseExpectedButtonStyles() throws IOException {
        String fxml = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(fxml.contains("fx:id=\"closeButton\" mnemonicParsing=\"false\" styleClass=\"window-close-square-button\""));
        assertTrue(fxml.contains("fx:id=\"refreshButton\" mnemonicParsing=\"false\" onAction=\"#onRefresh\" styleClass=\"folder-button\""));
    }

    @Test
    void closeAndRefreshStylesUseBlueAndTransparentThemes() throws IOException {
        String css = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.css").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(css.contains(".window-close-square-button {\n    -fx-background-color: linear-gradient(to bottom, #8ecbff, #6faee8);"));
        assertTrue(css.contains("-fx-border-color: #73a8d9;"));
        assertTrue(css.contains(".folder-button {\n    -fx-background-radius: 4;"));
        assertTrue(css.contains(".folder-button {\n    -fx-background-radius: 4;\n    -fx-border-radius: 4;\n    -fx-border-color: #4bd4f2;\n    -fx-border-width: 1;\n    -fx-background-color: rgba(34, 145, 175, 0.32);\n    -fx-text-fill: #8de7f5;\n    -fx-padding: 6 9;\n    -fx-cursor: hand;"));
        assertTrue(css.contains(".details-checkbox .box {\n    -fx-background-color: transparent;"));
        assertTrue(css.contains("-fx-border-color: #4bd4f2;"));
    }

    @Test
    void topBarUsesUnifiedColorAndCompactTopSpacing() throws IOException {
        String fxml = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );
        String css = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.css").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(fxml.contains("<VBox spacing=\"6.0\" styleClass=\"top-section\">"));
        assertTrue(fxml.contains("<Insets top=\"0.0\" right=\"8.0\" bottom=\"2.0\" left=\"8.0\"/>"));
        assertTrue(fxml.contains("<Insets top=\"0.0\" right=\"16.0\" bottom=\"4.0\" left=\"16.0\"/>"));
        assertTrue(css.contains(".window-drag-bar {\n    -fx-background-color: transparent;"));
    }

    @Test
    void worldDetailsViewIsDefinedInSeparateFxml() throws IOException {
        String detailsFxml = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/world-details-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(detailsFxml.contains("fx:id=\"detailsRoot\""));
        assertTrue(detailsFxml.contains("styleClass=\"app-root details-root\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWindowDragBar\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWindowMinimizeButton\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWindowCloseButton\""));
        assertTrue(!detailsFxml.contains("fx:id=\"detailsWindowTitle\""));
        assertTrue(!detailsFxml.contains("fx:id=\"detailsWindowHeaderSeparator\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsNameLabel\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWhitelistList\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsAddUuidButton\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsSaveWhitelistButton\""));
    }
}
