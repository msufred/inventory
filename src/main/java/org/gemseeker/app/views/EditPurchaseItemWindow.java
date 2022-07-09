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
public class EditPurchaseItemWindow extends AbstractWindowController {
    
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
    
    private final AddPurchaseWindow addPurchaseWindow;
    private final CompositeDisposable disposables;
    
    private final SimpleDoubleProperty mTotal = new SimpleDoubleProperty(0);
    
    private PurchaseInvoiceItem mItem;
    
    public EditPurchaseItemWindow(AddPurchaseWindow addPurchaseWindow) {
        super("Edit Product", AddProductWindow.class.getResource("edit_purchase_item.fxml"), addPurchaseWindow.getWindow());
        this.addPurchaseWindow = addPurchaseWindow;
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
        PurchaseInvoiceItem item = new PurchaseInvoiceItem();                           // create PurchaseInvoiceItem object
        item.setUnitPrice(Double.parseDouble(tfPrice.getText().trim()));                // set Unit Price
        item.setQuantity(Integer.parseInt(tfQuantity.getText().trim()));                // set Quantity
        item.setTotal(mTotal.get());                                                    // set Total
        
        Product product = new Product();                                                // create new Product object
        product.setName(tfName.getText());                                              // set Product name
        product.setSku(tfSku.getText());                                                // set Product SKU
        product.setUnit(cbUnits.getEditor().getText());                                 // set Product Unit
        product.setUnitPrice(Double.parseDouble(tfPrice.getText().trim()));             // set Product Unit Price
        product.setRetailPrice(Double.parseDouble(tfRetailPrice.getText().trim()));     // set Product Retail Price
        
        item.setProduct(product);                                                       // set PurchaseInvoiceItem Product
        addPurchaseWindow.replaceProduct(item);
        close();
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
