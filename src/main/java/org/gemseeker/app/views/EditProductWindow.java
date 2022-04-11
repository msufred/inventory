package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.Supplier;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.frameworks.AbstractWindowController;

/**
 * Edit Product will only edit product's name, sku and supplier details.
 *
 * @author Gem
 */
public class EditProductWindow extends AbstractWindowController {
    
    @FXML private TextField tfName;
    @FXML private TextField tfSku;
    @FXML private TextField tfSupplier;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private ProgressBar progressBar;
    
    private final AbstractPanelController panelController;
    private final CompositeDisposable disposables;
    
    private Product mProduct;
    
    public EditProductWindow(AbstractPanelController panelController, Stage mainStage) {
        super("Add Product", EditProductWindow.class.getResource("edit_product.fxml"), mainStage);
        this.panelController = panelController;
        disposables = new CompositeDisposable();
    }

    @Override
    protected void initWindow(Stage stage) {
        super.initWindow(stage);
        stage.initModality(Modality.APPLICATION_MODAL);
    }

    @Override
    public void onLoad() {
        Utils.setSafeTextField(tfName);
        Utils.setSafeTextField(tfSku);
        Utils.setSafeTextField(tfSupplier);
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    if (mProduct == null || tfName.getText().isEmpty() || tfSupplier.getText().isEmpty()) {
                        showInfoDialog("Invalid Input", "Please fill-in required fields.");
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
            mProduct = product;
        } else {
            showInfoDialog("No Product Selected", "Please select a product and try again.");
        }
    }
    
    private void saveAndClose() {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            EmbeddedDatabase database = EmbeddedDatabase.getInstance();
            
            String supplierName = tfSupplier.getText();
            Supplier supplier = database.getSupplier(supplierName);
            if (supplier == null) {
                supplier = new Supplier();
                supplier.setName(supplierName);
                supplier.setId(database.addEntryReturnId(supplier));
            }
            
            mProduct.setName(tfName.getText());
            mProduct.setSku(tfSku.getText());
            mProduct.setSupplier(supplier.getName());
            return database.executeQuery(mProduct.updateSQL());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            showProgress(false);
            if (!success) {
                showInfoDialog("Failed to update product entry.", "");
            }
            close();
            panelController.onResume();
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
        showProgress(false);
        mProduct = null;
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }

}
