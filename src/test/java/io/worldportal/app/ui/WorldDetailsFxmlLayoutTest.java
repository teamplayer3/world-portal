package io.worldportal.app.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldDetailsFxmlLayoutTest {

    @Test
    void worldDetailsViewBindsDedicatedController() throws IOException {
        String detailsFxml = new String(
                WorldDetailsFxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/world-details-view.fxml")
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(detailsFxml.contains("fx:controller=\"io.worldportal.app.ui.WorldDetailsController\""));
    }

    @Test
    void worldDetailsViewIsDefinedInSeparateFxml() throws IOException {
        String detailsFxml = new String(
                WorldDetailsFxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/world-details-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(detailsFxml.contains("fx:id=\"detailsRoot\""));
        assertTrue(detailsFxml.contains("styleClass=\"app-root\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWindowDragBar\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWindowMinimizeButton\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWindowCloseButton\""));
        assertTrue(!detailsFxml.contains("fx:id=\"detailsWindowTitle\""));
        assertTrue(!detailsFxml.contains("fx:id=\"detailsWindowHeaderSeparator\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsNameInput\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsFolderInput\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWhitelistList\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsAddUuidButton\""));
        assertTrue(!detailsFxml.contains("fx:id=\"detailsSaveWhitelistButton\""));
    }

    @Test
    void worldDetailsUsesBorderPaneWithWindowControlsInTopAndContentInCenter() throws IOException {
        String detailsFxml = new String(
                WorldDetailsFxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/world-details-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(detailsFxml.contains("<BorderPane"));
        assertTrue(detailsFxml.contains("<top>"));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWindowMinimizeButton\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWindowCloseButton\""));
        assertTrue(detailsFxml.contains("<center>"));
        assertTrue(detailsFxml.contains("fx:id=\"detailsNameInput\""));

        int topIndex = detailsFxml.indexOf("<top>");
        int centerIndex = detailsFxml.indexOf("<center>");
        int minButtonIndex = detailsFxml.indexOf("fx:id=\"detailsWindowMinimizeButton\"");
        int closeButtonIndex = detailsFxml.indexOf("fx:id=\"detailsWindowCloseButton\"");
        int nameInputIndex = detailsFxml.indexOf("fx:id=\"detailsNameInput\"");

        assertTrue(topIndex >= 0 && centerIndex > topIndex);
        assertTrue(minButtonIndex > topIndex && minButtonIndex < centerIndex);
        assertTrue(closeButtonIndex > topIndex && closeButtonIndex < centerIndex);
        assertTrue(nameInputIndex > centerIndex);
    }

    @Test
    void worldDetailsContainsBottomPaneWithRightAlignedDeleteButton() throws IOException {
        String detailsFxml = new String(
                WorldDetailsFxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/world-details-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(detailsFxml.contains("<bottom>"));
        assertTrue(detailsFxml.contains("alignment=\"CENTER_RIGHT\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsDeleteWorldButton\""));
        assertTrue(detailsFxml.contains("text=\"Delete World\""));
        assertTrue(detailsFxml.contains("styleClass=\"danger-action-button\""));
    }

    @Test
    void worldDetailsProvidesEditableNameAndFolderInputsWithSaveButton() throws IOException {
        String detailsFxml = new String(
                WorldDetailsFxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/world-details-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(detailsFxml.contains("fx:id=\"detailsNameInput\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsFolderInput\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsSaveIdentityButton\""));
        assertTrue(detailsFxml.contains("<HBox alignment=\"CENTER_LEFT\" spacing=\"8.0\">"));
        assertTrue(detailsFxml.contains("<Label styleClass=\"field-label\" text=\"Name\"/>"));
        assertTrue(detailsFxml.contains("<TextField fx:id=\"detailsNameInput\" promptText=\"World name\" styleClass=\"neon-input\" HBox.hgrow=\"ALWAYS\"/>"));
        assertTrue(detailsFxml.contains("<Label styleClass=\"field-label\" text=\"Folder\"/>"));
        assertTrue(detailsFxml.contains("<TextField fx:id=\"detailsFolderInput\" promptText=\"Folder name\" styleClass=\"neon-input\" HBox.hgrow=\"ALWAYS\"/>"));
    }

    @Test
    void worldDetailsDefinesThemeStyleClassesDirectlyInFxml() throws IOException {
        String detailsFxml = new String(
                WorldDetailsFxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/world-details-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(detailsFxml.contains("fx:id=\"detailsWindowDragBar\" alignment=\"CENTER_RIGHT\" spacing=\"6.0\" styleClass=\"window-drag-bar\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWindowMinimizeButton\" mnemonicParsing=\"false\" styleClass=\"window-button\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWindowCloseButton\" mnemonicParsing=\"false\" styleClass=\"window-close-square-button\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsNameInput\" promptText=\"World name\" styleClass=\"neon-input\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsFolderInput\" promptText=\"Folder name\" styleClass=\"neon-input\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsGameModeLabel\" styleClass=\"field-label\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsPatchLabel\" styleClass=\"field-label\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsLastPlayedLabel\" styleClass=\"field-label\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWhitelistTitle\" styleClass=\"panel-title\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWhitelistEnabledCheckBox\" mnemonicParsing=\"false\" styleClass=\"details-checkbox\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWhitelistList\" prefHeight=\"180.0\" styleClass=\"worlds-list\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsUuidInput\" promptText=\"Player UUID\" styleClass=\"neon-input\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsAddUuidButton\" mnemonicParsing=\"false\" styleClass=\"action-button\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsRemoveUuidButton\" mnemonicParsing=\"false\" styleClass=\"action-button\""));
        assertTrue(!detailsFxml.contains("fx:id=\"detailsSaveWhitelistButton\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsWhitelistStatusLabel\" styleClass=\"status-label\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsIdentityStatusLabel\" styleClass=\"status-label\""));
        assertTrue(detailsFxml.contains("fx:id=\"detailsSaveIdentityButton\" managed=\"false\" mnemonicParsing=\"false\" styleClass=\"action-button\" text=\"Save Changes\" visible=\"false\""));
    }

    @Test
    void worldDetailsRootDefinesOnlyAppRootInInlineStyleClass() throws IOException {
        String detailsFxml = new String(
                WorldDetailsFxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/world-details-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(detailsFxml.contains("styleClass=\"app-root\""));
        assertTrue(!detailsFxml.contains("details-root"));
    }

    @Test
    void worldDetailsUsesFxmlSpacingAndMainViewLikeContentPadding() throws IOException {
        String detailsFxml = new String(
                WorldDetailsFxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/world-details-view.fxml").readAllBytes(),
                StandardCharsets.UTF_8
        );
        String css = normalizeLineEndings(new String(
                WorldDetailsFxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/main-view.css").readAllBytes(),
                StandardCharsets.UTF_8
        ));

        assertTrue(detailsFxml.contains("<VBox fillWidth=\"true\" spacing=\"6.0\">"));
        assertTrue(detailsFxml.contains("<Insets top=\"12.0\" right=\"16.0\" bottom=\"12.0\" left=\"16.0\"/>"));
        assertTrue(!css.contains(".details-root {\n    -fx-spacing:"));
    }

    @Test
    void worldDeleteConfirmationViewDefinesNameInputAndConfirmButtons() throws IOException {
        String deleteConfirmationFxml = new String(
                WorldDetailsFxmlLayoutTest.class.getResourceAsStream("/io/worldportal/app/world-delete-confirmation-view.fxml")
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
        return value.replace("\r\n", "\n").replace("\r", "\n");
    }
}
