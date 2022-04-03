package org.gemseeker.app.views;

import org.gemseeker.app.views.frameworks.AbstractWindowController;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 *
 * @author Gem
 */
public class MainWindow extends AbstractWindowController {
    
    @FXML private ToggleGroup toggleGroup;
    @FXML private ToggleButton toggleWarehouse;
    @FXML private ToggleButton toggleOrders;
    @FXML private ToggleButton toggleInvoices;
    @FXML private StackPane contentView;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    
    private final CompositeDisposable disposables;
    
    private final InventoryPanel inventoryPanel;
    private final OrdersPanel ordersPanel;
    private final InvoicesPanel invoicesPanel;
    private AbstractPanelController mPanelController;
    
    public MainWindow(Stage stage) {
        super("Inventory", MainWindow.class.getResource("main.fxml"), stage, null);
        disposables = new CompositeDisposable();
        
        inventoryPanel = new InventoryPanel(this);
        ordersPanel = new OrdersPanel(this);
        invoicesPanel = new InvoicesPanel(this);
    }

    @Override
    protected void initWindow(Stage stage) {
        super.initWindow(stage);
        stage.setMaximized(true);
    }
    
    @Override
    public void onLoad() {
        addToggleEventFilter(toggleWarehouse);
        addToggleEventFilter(toggleOrders);
        addToggleEventFilter(toggleInvoices);
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(toggleWarehouse).subscribe(evt -> {
                    changeContent(inventoryPanel);
                }),
                JavaFxObservable.actionEventsOf(toggleOrders).subscribe(evt -> {
                    changeContent(ordersPanel);
                }),
                JavaFxObservable.actionEventsOf(toggleInvoices).subscribe(evt -> {
                    changeContent(invoicesPanel);
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

    public Stage getWindow() {
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
        ordersPanel.onDispose();
        invoicesPanel.onDispose();
    }

}
