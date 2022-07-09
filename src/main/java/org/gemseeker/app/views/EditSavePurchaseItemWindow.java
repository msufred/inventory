package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.beans.property.SimpleDoubleProperty;
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
import org.gemseeker.app.data.PurchaseInvoiceItem;
import org.gemseeker.app.data.Stock;
import org.gemseeker.app.views.frameworks.AbstractWindowController;

/**
 *
 * @author Gem
 */
public class EditSavePurchaseItemWindow extends AbstractWindowController {
    
    @FXML private TextField tfName;
    @FXML private TextField tfSku;
    @FXML private TextField tfPrice;
    @FXML private TextField tfRetailPrice;
    @FXML private TextField tfQuantity;
    @FXML private TextField tfTotal;
    @FXML private ComboBox<String> cbUnits;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private ProgressBar progressBar;
    
    private final EditPurchaseWindow editPurchaseWindow;
    private final CompositeDisposable disposables;
    
    private final SimpleDoubleProperty mTotal = new SimpleDoubleProperty(0);
    
    private PurchaseInvoiceItem mItem;
    
    public EditSavePurchaseItemWindow(EditPurchaseWindow editPurchaseWindow) {
        super("Edit Product", AddProductWindow.class.getResource("edit_save_purchase_item.fxml"), editPurchaseWindow.getWindow());
        this.editPurchaseWindow = editPurchaseWindow;
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
        Utils.setAsNumericalTextField(tfPrice);
        Utils.setAsNumericalTextField(tfRetailPrice);
        Utils.setAsIntegerTextField(tfQuantity);
        
        cbUnits.setItems(FXCollections.observableArrayList(
                "Piece", "Box", "Pack", "Tray", "Sack", "Kilogram", "Gram",  "Litre"
        ));
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    saveAndClose();
                }),
                JavaFxObservable.actionEventsOf(btnCancel).subscribe(evt -> {
                    close();
                }),
                JavaFxObservable.changesOf(mTotal).subscribe(value -> {
                    if (value.getNewVal() != null) {
                        tfTotal.setText("P " + Utils.toMoneyFormat(value.getNewVal().doubleValue()));
                    }
                }),
                JavaFxObservable.changesOf(tfPrice.textProperty()).subscribe(text -> {
                    recalculate();
                }),
                JavaFxObservable.changesOf(tfQuantity.textProperty()).subscribe(text -> {
                    recalculate();
                })
        );
    }
    
    public void show(PurchaseInvoiceItem item) {
        if (item == null) {
            showInfoDialog("Cannot Edit Purchase Item", "No selected purchase invoice item.");
            return;
        }
        mItem = item;
        show();
        
        Product product = mItem.getProduct();
        tfName.setText(product.getName());
        tfSku.setText(product.getSku());
        cbUnits.getEditor().setText(product.getUnit());
        tfPrice.setText(product.getUnitPrice() + "");
        tfRetailPrice.setText(product.getRetailPrice() + "");
        tfQuantity.setText(mItem.getQuantity() + "");
        mTotal.set(mItem.getTotal());
    }

    private void recalculate() {
        double price = 0;
        if (!tfPrice.getText().isEmpty()) price = Double.parseDouble(tfPrice.getText().trim());
        
        int qty = 0;
        if (!tfQuantity.getText().isEmpty()) qty = Integer.parseInt(tfQuantity.getText().trim());
        
        double total = price * qty;
        mTotal.set(total);
    }

    private void saveAndClose() {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            mItem.setUnitPrice(Double.parseDouble(tfPrice.getText().trim()));
            mItem.setQuantity(Integer.parseInt(tfQuantity.getText().trim()));
            mItem.setTotal(mTotal.get());
            
            EmbeddedDatabase db = EmbeddedDatabase.getInstance();
            boolean success = db.executeQuery(mItem.updateSQL());
            
            if (success) {
                Product product = mItem.getProduct();
                product.setName(tfName.getText());
                product.setSku(tfSku.getText());
                product.setUnit(cbUnits.getEditor().getText());
                product.setUnitPrice(Double.parseDouble(tfPrice.getText().trim()));
                product.setRetailPrice(Double.parseDouble(tfRetailPrice.getText().trim()));
                boolean productUpdated = db.executeQuery(product.updateSQL());
                if (productUpdated) {
                    Stock stock = product.getStock();
                    int diff = stock.getQuantity() - mItem.getQuantity();
                    stock.setQuantity(stock.getQuantity() - diff);
                    stock.setInStock(stock.getInStock() - diff);
                    db.executeQuery(stock.updateSQL());
                }
            }
            return success;
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            showProgress(false);
            if (success) System.out.println("Success");
            else System.out.println("Failed");
            close();
            editPurchaseWindow.refresh();
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error while updating invoice item.", err);
        }));
    }
    
    private void showProgress(boolean show) {
        progressBar.setVisible(show);
    }

    @Override
    public void onClose() {
        tfName.clear();
        tfSku.clear();
        tfPrice.setText("0");
        tfRetailPrice.setText("0");
        tfQuantity.setText("0");
        tfTotal.setText("0");
        cbUnits.setValue(null);
        showProgress(false);
        mItem = null;
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }

}
