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
import org.gemseeker.app.views.frameworks.AbstractWindowController;

/**
 *
 * @author Gem
 */
public class EditProductWindow extends AbstractWindowController {
    
    @FXML private TextField tfName;
    @FXML private TextField tfSku;
    @FXML private TextField tfSupplier;
    @FXML private TextField tfPrice;
    @FXML private TextField tfRetailPrice;
    @FXML private TextField tfQuantity;
    @FXML private ComboBox<String> cbUnits;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private ProgressBar progressBar;
    
    private final PurchasesPanel inventoryPanel;
    private final CompositeDisposable disposables;
    
    private Product mProduct;
    
    public EditProductWindow(PurchasesPanel inventoryPanel, Stage mainStage) {
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
        Utils.setAsNumericalTextField(tfRetailPrice);
        Utils.setAsIntegerTextField(tfQuantity);
        cbUnits.setItems(FXCollections.observableArrayList(
                "Piece", "Box", "Pack", "Tray", "Sack", "Kilogram", "Gram",  "Litre"
        ));
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    if (mProduct == null || tfName.getText().isEmpty() ||
                            cbUnits.getValue() == null || tfPrice.getText().isEmpty() ||
                            tfRetailPrice.getText().isEmpty() || tfQuantity.getText().isEmpty()) {
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

    public void show(Product product) {
        super.show();
        if (product != null) {
            tfName.setText(product.getName());
            tfSku.setText(product.getSku());
            tfSupplier.setText(product.getSupplier());
            cbUnits.setValue(product.getUnit());
            tfPrice.setText(String.format("%.2f", product.getUnitPrice()));
            tfRetailPrice.setText(String.format("%.2f", product.getRetailPrice()));
            tfQuantity.setText(product.getStock().getQuantity() + "");
            mProduct = product;
        } else {
            showInfoDialog("No Product Selected", "Please select a product and try again.");
        }
    }
    
    private void saveAndClose() {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            mProduct.setName(tfName.getText());
            mProduct.setSku(tfSku.getText());
            mProduct.setSupplier(tfSupplier.getText());
            mProduct.setUnit(cbUnits.getValue());
            mProduct.setUnitPrice(Double.parseDouble(tfPrice.getText().trim()));
            mProduct.setRetailPrice(Double.parseDouble(tfRetailPrice.getText().trim()));
            return EmbeddedDatabase.getInstance().executeQuery(mProduct.updateSQL());
        }).flatMap(success -> Single.fromCallable(() -> {
            if (success) {
                int qty = Integer.parseInt(tfQuantity.getText().trim());
                return EmbeddedDatabase.getInstance().updateEntry("stocks", "quantity", qty, "id", mProduct.getStock().getId());
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
        tfName.clear();
        tfSku.clear();
        tfSupplier.clear();
        tfPrice.setText("0");
        tfQuantity.setText("0");
        cbUnits.setValue(null);
        showProgress(false);
        mProduct = null;
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }

}
