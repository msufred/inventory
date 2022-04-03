package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.Stock;
import org.gemseeker.app.views.frameworks.AbstractWindowController;

/**
 *
 * @author Gem
 */
public class AddProductWindow extends AbstractWindowController {
    
    @FXML private TextField tfName;
    @FXML private TextField tfSku;
    @FXML private TextField tfSupplier;
    @FXML private TextField tfPrice;
    @FXML private TextField tfQuantity;
    @FXML private ComboBox<String> cbUnits;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private ProgressBar progressBar;
    
    private final InventoryPanel inventoryPanel;
    private final CompositeDisposable disposables;
    
    public AddProductWindow(InventoryPanel inventoryPanel, Stage mainStage) {
        super("Add Product", AddProductWindow.class.getResource("add_product.fxml"), mainStage);
        this.inventoryPanel = inventoryPanel;
        disposables = new CompositeDisposable();
    }

    @Override
    protected void initWindow(Stage stage) {
        super.initWindow(stage);
        stage.initModality(Modality.APPLICATION_MODAL);
    }

    @Override
    public void onLoad() {
        Utils.setAsNumericalTextField(tfPrice);
        Utils.setAsIntegerTextField(tfQuantity);
        cbUnits.setItems(FXCollections.observableArrayList(
                "Piece", "Box", "Pack", "Tray", "Sack", "Kilogram", "Gram",  "Litre"
        ));
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    if (tfName.getText().isEmpty() || cbUnits.getValue() == null ||
                            tfPrice.getText().isEmpty() || tfQuantity.getText().isEmpty()) {
                        showInfoDialog("Invalid Input", "Please fill-in required fields: Product Name, "
                                + "Unit, Unit Price (PHP), Stock Quantity");
                    } else {
                        saveAndClose();
                    }
                }),
                JavaFxObservable.actionEventsOf(btnCancel).subscribe(evt -> {
                    close();
                })
        );
    }
    
    private void saveAndClose() {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            Product product = new Product();
            product.setName(tfName.getText());
            product.setSku(tfSku.getText());
            product.setSupplier(tfSupplier.getText());
            product.setUnit(cbUnits.getValue());
            product.setUnitPrice(Double.parseDouble(tfPrice.getText().trim()));
            return EmbeddedDatabase.getInstance().addEntryReturnId(product);
        }).flatMap(id -> Single.fromCallable(() -> {
            if (id != -1) {
                Stock stock = new Stock();
                stock.setProductId(id);
                stock.setQuantity(Integer.parseInt(tfQuantity.getText().trim()));
                boolean success = EmbeddedDatabase.getInstance().addEntry(stock);
                if (success) return id;
                else return -2;
            }
            return id;
        })).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(id -> {
            showProgress(false);
            if (id == -2) {
                showInfoDialog("Error while saving...", "Product entry was saved successfully but "
                        + "related data wasn't saved properly.");
            } else if (id == -1) {
                showInfoDialog("Failed to save product entry.", "");
            }
            close();
            inventoryPanel.onResume();
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error occurred while saving product data.", err);
        }));
    }
    
    private void showProgress(boolean show) {
        progressBar.setVisible(show);
    }

    @Override
    public void onClose() {
        tfName.clear();
        tfSku.clear();
        tfSupplier.clear();
        tfPrice.clear();
        cbUnits.setValue(null);
        showProgress(false);
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }

}
