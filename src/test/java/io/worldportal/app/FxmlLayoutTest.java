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
    void bottomSectionContainsTransferProgressStatusAndRefreshOnly() throws IOException {
        String fxml = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(fxml.contains("fx:id=\"transferProgressIndicator\""));
        assertTrue(fxml.contains("fx:id=\"transferStatusLabel\""));
        assertTrue(fxml.contains("fx:id=\"refreshButton\""));
        assertTrue(!fxml.contains("fx:id=\"uploadButton\""));
        assertTrue(!fxml.contains("fx:id=\"downloadButton\""));
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
        String css = normalizeLineEndings(new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.css").readAllBytes(),
                StandardCharsets.UTF_8
        ));

        assertTrue(css.contains(".app-title {\n    -fx-text-fill: #7de7f5;\n    -fx-font-size: 20px;"));
        assertTrue(css.contains(".panel-title {\n    -fx-text-fill: #8bdeee;\n    -fx-font-size: 14px;"));
        assertTrue(css.contains(".field-label {\n    -fx-text-fill: #85d4e8;\n    -fx-font-size: 13px;"));
        assertTrue(css.contains("-fx-font-size: 13px;"));
        assertTrue(css.contains(".action-button {\n    -fx-border-width: 1;\n    -fx-font-size: 13px;"));
    }

    @Test
    void multilineStyleAssertionsNeedLineEndingNormalizationForWindows() throws IOException {
        String css = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.css").readAllBytes(),
                StandardCharsets.UTF_8
        );
        String windowsCss = css.replace("\n", "\r\n");

        assertTrue(normalizeLineEndings(windowsCss).contains(".app-title {\n    -fx-text-fill: #7de7f5;\n    -fx-font-size: 20px;"));
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
        String css = normalizeLineEndings(new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.css").readAllBytes(),
                StandardCharsets.UTF_8
        ));

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
        String css = normalizeLineEndings(new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.css").readAllBytes(),
                StandardCharsets.UTF_8
        ));

        assertTrue(fxml.contains("<VBox spacing=\"6.0\">"));
        assertTrue(fxml.contains("<Insets top=\"4.0\" right=\"8.0\" bottom=\"2.0\" left=\"8.0\"/>"));
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
        assertTrue(detailsFxml.contains("styleClass=\"app-root\""));
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

    @Test
    void worldDetailsUsesBorderPaneWithWindowControlsInTopAndContentInCenter() throws IOException {
        String detailsFxml = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/world-details-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(detailsFxml.contains("<BorderPane"));
        assertTrue(detailsFxml.contains("<top>"));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWindowMinimizeButton\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWindowCloseButton\""));
        assertTrue(detailsFxml.contains("<center>"));
        assertTrue(detailsFxml.contains("fx:id=\"detailsNameLabel\""));

        int topIndex = detailsFxml.indexOf("<top>");
        int centerIndex = detailsFxml.indexOf("<center>");
        int minButtonIndex = detailsFxml.indexOf("fx:id=\"detailsWindowMinimizeButton\"");
        int closeButtonIndex = detailsFxml.indexOf("fx:id=\"detailsWindowCloseButton\"");
        int nameLabelIndex = detailsFxml.indexOf("fx:id=\"detailsNameLabel\"");

        assertTrue(topIndex >= 0 && centerIndex > topIndex);
        assertTrue(minButtonIndex > topIndex && minButtonIndex < centerIndex);
        assertTrue(closeButtonIndex > topIndex && closeButtonIndex < centerIndex);
        assertTrue(nameLabelIndex > centerIndex);
    }

    @Test
    void worldDetailsContainsBottomPaneWithRightAlignedDeleteButton() throws IOException {
        String detailsFxml = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/world-details-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(detailsFxml.contains("<bottom>"));
        assertTrue(detailsFxml.contains("alignment=\"CENTER_RIGHT\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsDeleteWorldButton\""));
        assertTrue(detailsFxml.contains("text=\"Delete World\""));
        assertTrue(detailsFxml.contains("styleClass=\"danger-action-button\""));
    }

    @Test
    void worldDetailsDefinesThemeStyleClassesDirectlyInFxml() throws IOException {
        String detailsFxml = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/world-details-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(detailsFxml.contains("fx:id=\"detailsWindowDragBar\" alignment=\"CENTER_RIGHT\" spacing=\"6.0\" styleClass=\"window-drag-bar\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWindowMinimizeButton\" mnemonicParsing=\"false\" styleClass=\"window-button\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWindowCloseButton\" mnemonicParsing=\"false\" styleClass=\"window-close-square-button\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsNameLabel\" styleClass=\"panel-title\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsFolderLabel\" styleClass=\"field-label\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsGameModeLabel\" styleClass=\"field-label\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsPatchLabel\" styleClass=\"field-label\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsLastPlayedLabel\" styleClass=\"field-label\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWhitelistTitle\" styleClass=\"panel-title\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWhitelistEnabledCheckBox\" mnemonicParsing=\"false\" styleClass=\"details-checkbox\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWhitelistList\" prefHeight=\"180.0\" styleClass=\"worlds-list\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsUuidInput\" promptText=\"Player UUID\" styleClass=\"neon-input\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsAddUuidButton\" mnemonicParsing=\"false\" styleClass=\"action-button\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsRemoveUuidButton\" mnemonicParsing=\"false\" styleClass=\"action-button\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsSaveWhitelistButton\" mnemonicParsing=\"false\" styleClass=\"action-button\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWhitelistStatusLabel\" styleClass=\"status-label\""));
    }

    @Test
    void worldDetailsRootDefinesOnlyAppRootInInlineStyleClass() throws IOException {
        String detailsFxml = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/world-details-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(detailsFxml.contains("styleClass=\"app-root\""));
        assertTrue(!detailsFxml.contains("details-root"));
    }

    @Test
    void worldDetailsUsesFxmlSpacingAndMainViewLikeContentPadding() throws IOException {
        String detailsFxml = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/world-details-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );
        String css = normalizeLineEndings(new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.css").readAllBytes(),
                StandardCharsets.UTF_8
        ));

        assertTrue(detailsFxml.contains("<VBox fillWidth=\"true\" spacing=\"6.0\">"));
        assertTrue(detailsFxml.contains("<Insets top=\"12.0\" right=\"16.0\" bottom=\"12.0\" left=\"16.0\"/>"));
        assertTrue(!css.contains(".details-root {\n    -fx-spacing:"));
    }

    @Test
    void appRootUsesSmallCornerRadiusForWindowEdges() throws IOException {
        String css = normalizeLineEndings(new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.css").readAllBytes(),
                StandardCharsets.UTF_8
        ));

        assertTrue(css.contains(".app-root {\n    -fx-font-family: \"Segoe UI\", \"Noto Sans\", sans-serif;"));
        assertTrue(css.contains("-fx-background-radius: 8;"));
        assertTrue(css.contains("-fx-border-radius: 8;"));
    }

    @Test
    void worldDeleteConfirmationViewDefinesNameInputAndConfirmButtons() throws IOException {
        String deleteConfirmationFxml = new String(
                FxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/world-delete-confirmation-view.fxml")
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(deleteConfirmationFxml.contains("fx:id=\"deleteConfirmationRoot\""));
        assertTrue(deleteConfirmationFxml.contains("fx:id=\"deleteWorldPromptLabel\""));
        assertTrue(deleteConfirmationFxml.contains("fx:id=\"deleteWorldNameInput\""));
        assertTrue(deleteConfirmationFxml.contains("fx:id=\"confirmDeleteWorldButton\""));
        assertTrue(deleteConfirmationFxml.contains("fx:id=\"cancelDeleteWorldButton\""));
        assertTrue(deleteConfirmationFxml.contains("styleClass=\"danger-action-button\""));
    }

    private static String normalizeLineEndings(String value) {
        return value.replace("\r\n", "\n");
    }
}
