package org.gemseeker.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.sql.SQLException;
import javafx.scene.control.Alert;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.views.MainWindow;

public class Main extends Application {

    private final Service<Void> startup = new Service<Void>() {
        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    // create app folder (if not created yet)
                    File appFolder = new File(Utils.getAppFolder());
                    if (!appFolder.exists()) appFolder.mkdirs();

                    // create logs folder (if not created yet)
                    File logFolder = new File(Utils.getLogFolder());
                    if (!logFolder.exists()) logFolder.mkdirs();

                    // create data folder (if not created yet)
                    File dataFolder = new File(Utils.getDataFolder());
                    if (!dataFolder.exists()) dataFolder.mkdirs();

                    // create images folder (if not created yet)
                    File imagesFolder = new File(Utils.getImagesFolder());
                    if (!imagesFolder.exists()) imagesFolder.mkdirs();

                    // create temp folder (if not created yet)
                    File tempFolder = new File(Utils.getTempFolder());
                    if (!tempFolder.exists()) tempFolder.mkdirs();

                    // settings.xml
                    File settingsFile = new File(Utils.getAppFolder() + Utils.getSeparator() + "settings.xml");
                    if (!settingsFile.exists()) {
                        File origFile = new File("settings.xml");
                        FileUtils.copyFile(origFile, settingsFile);
                    }

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        System.err.println(e);
                    }
                    return null;
                }
            };
        }
    };
    
    private MainWindow mainWindow;
    private Alert errorDialog;

    @Override
    public void start(Stage primaryStage) throws Exception {
        mainWindow = new MainWindow(primaryStage);
        errorDialog = new Alert(Alert.AlertType.ERROR);
        
        doStart();
    }

    private void doStart() {
        startup.restart();

        startup.setOnRunning(evt -> {
            System.out.print("Startup running...");
        });

        startup.setOnFailed(evt -> {
            System.out.println("FAILED");
            Platform.exit();
            System.exit(1);
        });

        startup.setOnSucceeded(evt -> {
            System.out.println("SUCCESS");
            mainWindow.show();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
