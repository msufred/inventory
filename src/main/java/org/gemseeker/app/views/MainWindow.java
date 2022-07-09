package org.gemseeker.app.views;

import io.reactivex.Completable;
import org.gemseeker.app.views.frameworks.AbstractWindowController;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.gemseeker.app.data.EmbeddedDatabase;

/**
 *
 * @author Gem
 */
public class MainWindow extends AbstractWindowController {
    
    @FXML private ToggleButton toggleInventory;
    @FXML private ToggleButton togglePurchases;
    @FXML private ToggleButton toggleOrders;
    @FXML private ToggleButton toggleShippers;
    @FXML private ToggleButton toggleDeliveries;
    @FXML private StackPane contentView;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    
     @FXML private MenuItem mReset; // TEMPORARY RESET FUNCTION - will drop the database and re-create database at the same time
    
    private final CompositeDisposable disposables;
    
    private final InventoryPanel inventoryPanel;
    private final PurchasesPanel purchasesPanel;
    private final OrdersPanel ordersPanel;
    private final ShippersStocksPanel shipperStocksPanel;
    private final DeliveriesPanel deliveriesPanel;
    private AbstractPanelController mPanelController;
    
    public MainWindow(Stage stage) {
        super("Inventory", MainWindow.class.getResource("main.fxml"), stage, null);
        disposables = new CompositeDisposable();
        
        inventoryPanel = new InventoryPanel(this);
        purchasesPanel = new PurchasesPanel(this);
        ordersPanel = new OrdersPanel(this);
        shipperStocksPanel = new ShippersStocksPanel(this);
        deliveriesPanel = new DeliveriesPanel(this);
    }

    @Override
    protected void initWindow(Stage stage) {
        super.initWindow(stage);
        stage.setMaximized(true);
    }
    
    @Override
    public void onLoad() {
        addToggleEventFilter(toggleInventory);
        addToggleEventFilter(togglePurchases);
        addToggleEventFilter(toggleOrders);
        addToggleEventFilter(toggleShippers);
        addToggleEventFilter(toggleDeliveries);
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(toggleInventory).subscribe(evt -> {
                    changeContent(inventoryPanel);
                }),
                JavaFxObservable.actionEventsOf(togglePurchases).subscribe(evt -> {
                    changeContent(purchasesPanel);
                }),
                JavaFxObservable.actionEventsOf(toggleOrders).subscribe(evt -> {
                    changeContent(ordersPanel);
                }),
                JavaFxObservable.actionEventsOf(toggleShippers).subscribe(evt -> {
                    changeContent(shipperStocksPanel);
                }),
                JavaFxObservable.actionEventsOf(toggleDeliveries).subscribe(evt -> {
                    changeContent(deliveriesPanel);
                })
                ,
                JavaFxObservable.actionEventsOf(mReset).subscribe(evt -> {
                    Optional<ButtonType> result = showConfirmDialog("Reset Database?", "This will erase all the data of the database. Continue?");
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        showProgress(true, "Resetting database...");
                        disposables.add(Completable.fromAction(() -> {
                            EmbeddedDatabase.getInstance().reset();
                            EmbeddedDatabase.getInstance();
                        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(() -> {
                            showProgress(false);
                            mPanelController.onResume();
                        }, err -> {
                            showProgress(false);
                            showErrorDialog("Database Error", "Error while resetting database.", err);
                        }));
                    }
                })
        );
    }
    
    private void changeContent(AbstractPanelController panelController) {
        if (panelController == null) {
            showInfoDialog("Invalid Content View", "Couldn't load content view");
            return;
        }
        if (mPanelController != null) mPanelController.onPause();
        contentView.getChildren().clear();
        contentView.getChildren().add(panelController.getContent());
        panelController.onResume();
        mPanelController = panelController;
    }

    public void showProgress(boolean show) {
        showProgress(show, "");
    }
    
    public void showProgress(boolean show, String text) {
        progressBar.setVisible(show);
        progressLabel.setVisible(show);
        progressLabel.setText(text);
    }
    
    private void addToggleEventFilter(ToggleButton toggleButton) {
        toggleButton.addEventFilter(MouseEvent.MOUSE_PRESSED, evt -> {
            if (toggleButton.isSelected()) evt.consume();
        });
    }

    public Stage getMainStage() {
        return stage;
    }
    
    @Override
    public void show() {
        super.show();
        changeContent(inventoryPanel);
    }
    
    @Override
    public void onClose() {
        onDispose();
    }

    @Override
    public void onDispose() {
        disposables.dispose();
        inventoryPanel.onDispose();
        purchasesPanel.onDispose();
        ordersPanel.onDispose();
        shipperStocksPanel.onDispose();
        deliveriesPanel.onDispose();
    }

}
