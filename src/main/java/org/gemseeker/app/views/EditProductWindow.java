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
import javafx.scene.control.DatePicker;
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
public class EditProductWindow extends AbstractWindowController {
    
    @FXML private DatePicker datePicker;
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
    
    private Stock mStock;
    
    public EditProductWindow(InventoryPanel inventoryPanel, Stage mainStage) {
        super("Add Product", AddProductWindow.class.getResource("edit_product.fxml"), mainStage);
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
                    if (mStock == null || datePicker.getValue() == null || tfName.getText().isEmpty() ||
                            cbUnits.getValue() == null || tfPrice.getText().isEmpty() ||
                            tfQuantity.getText().isEmpty()) {
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

    public void show(Stock stock) {
        super.show();
        if (stock != null) {
            Product p = stock.getProduct();
            datePicker.setValue(p.getDate());
            tfName.setText(p.getName());
            tfSku.setText(p.getSku());
            tfSupplier.setText(p.getSupplier());
            cbUnits.setValue(p.getUnit());
            tfPrice.setText(String.format("%.2f", p.getUnitPrice()));
            tfQuantity.setText(stock.getQuantity() + "");
            mStock = stock;
        } else {
            showInfoDialog("No Product Selected", "Please select a product and try again.");
        }
    }
    
    private void saveAndClose() {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            Product product = mStock.getProduct();
            product.setDate(datePicker.getValue());
            product.setName(tfName.getText());
            product.setSku(tfSku.getText());
            product.setSupplier(tfSupplier.getText());
            product.setUnit(cbUnits.getValue());
            product.setUnitPrice(Double.parseDouble(tfPrice.getText().trim()));
            return EmbeddedDatabase.getInstance().executeQuery(product.updateSQL());
        }).flatMap(success -> Single.fromCallable(() -> {
            if (success) {
                int qty = Integer.parseInt(tfQuantity.getText().trim());
                return EmbeddedDatabase.getInstance().updateEntry("stocks", "quantity", qty, "id", mStock.getId());
            }
            return success;
        })).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            showProgress(false);
            if (!success) {
                showInfoDialog("Failed to update product entry.", "");
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
        datePicker.setValue(null);
        tfName.clear();
        tfSku.clear();
        tfSupplier.clear();
        tfPrice.setText("0");
        tfQuantity.setText("0");
        cbUnits.setValue(null);
        showProgress(false);
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }

}
