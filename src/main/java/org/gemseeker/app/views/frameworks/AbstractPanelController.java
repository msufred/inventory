package org.gemseeker.app.views.frameworks;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public abstract class AbstractPanelController {

    protected URL fxmlUrl;
    protected Pane root;

    private final Alert infoDialog;
    private final Alert errorDialog;
    private final Alert confirmDialog;

    public AbstractPanelController(URL fxmlUrl) {
        this.fxmlUrl = fxmlUrl;

        infoDialog = new Alert(Alert.AlertType.INFORMATION);
        infoDialog.setTitle("Information");
        errorDialog = new Alert(Alert.AlertType.ERROR);
        errorDialog.setHeaderText("Error");
        confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Action");
    }

    public Pane getContent() {
        if (root == null) {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(fxmlUrl);
            loader.setController(this);
            try {
                root = loader.load();
                onLoad();
            } catch (IOException e) {
                throw new RuntimeException("Failed to load fxml file: " + e);
            }
        }
        return root;
    }

    public abstract void onLoad();
    public abstract void onPause();
    public abstract void onResume();
    public abstract void onDispose();

    public void showInfoDialog(String title, String content) {
        infoDialog.setHeaderText(title);
        infoDialog.setContentText(content);
        infoDialog.showAndWait();
    }

    public void showErrorDialog(String title, String content, Throwable t) {
        errorDialog.setHeaderText(title);
        errorDialog.setContentText(content + "\n" + t);
        errorDialog.showAndWait();
    }

    public Optional<ButtonType> showConfirmDialog(String title, String content) {
        confirmDialog.setHeaderText(title);
        confirmDialog.setContentText(content);
        return confirmDialog.showAndWait();
    }
}
