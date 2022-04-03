package org.gemseeker.app.views.frameworks;

import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import javafx.scene.control.ButtonType;

public abstract class AbstractWindowController {

    protected Stage stage;
    protected Scene scene;
    protected Pane root;
    protected Stage owner;
    protected String windowTitle;
    protected URL fxmlUrl;

    private final EventHandler<WindowEvent> onCloseRequest = evt -> onClose();

    private final Alert infoDialog;
    private final Alert errorDialog;
    private final Alert confirmDialog;
    
    private boolean mStageCreated = false;

    public AbstractWindowController(String windowTitle, URL fxmlUrl, Stage owner) {
        this(windowTitle, fxmlUrl, null, owner);
    }
    
    public AbstractWindowController(String windowTitle, URL fxmlUrl, Stage stage, Stage owner) {
        this.windowTitle = windowTitle;
        this.fxmlUrl = fxmlUrl;
        this.stage = stage;
        this.owner = owner;

        infoDialog = new Alert(Alert.AlertType.INFORMATION);
        infoDialog.setTitle("Information");
        errorDialog = new Alert(Alert.AlertType.ERROR);
        errorDialog.setHeaderText("Error");
        confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation");
    }

    protected Stage getStage() {
        if (stage == null) stage = new Stage();
        if (!mStageCreated) {
            initWindow(stage);
            mStageCreated = true;
        }
        return stage;
    }
    
    protected void initWindow(Stage stage) {
        stage.setTitle(windowTitle);
        if (owner != null) stage.initOwner(owner);
        stage.setOnHidden(onCloseRequest);
        stage.setScene(getScene());
    }

    protected Scene getScene() {
        if (scene == null) {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(fxmlUrl);
            loader.setController(this);
            try {
                root = loader.load();
                onLoad();
                scene = new Scene(root);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load fxml file: " + e);
            }
        }
        return scene;
    }

    public void show() {
        getStage().show();
    }

    public void close() {
        getStage().close();
    }
    
    public Stage getWindow() {
        return stage;
    }

    /**
     * Called right after FXML file is loaded and added to a scene.
     */
    public abstract void onLoad();

    /**
     * Called when window is closed/hidden.
     */
    public abstract void onClose();

    /**
     * Called when the parent window/application is exited.
     */
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
    
    public Optional<ButtonType> showConfirmDialog(String title, String content, ButtonType... buttonTypes) {
        confirmDialog.setTitle(title);
        confirmDialog.setContentText(content);
        confirmDialog.getButtonTypes().setAll(buttonTypes);
        return confirmDialog.showAndWait();
    }
}
