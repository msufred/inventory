package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import java.util.Optional;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.PurchaseInvoice;
import org.gemseeker.app.data.PurchaseInvoiceItem;
import org.gemseeker.app.data.Stock;
import org.gemseeker.app.views.frameworks.AbstractWindowController;

/**
 * NOTE: Almost the same as AddProductWindow. The only difference is that
 * it will save the Product to the database directly when Add Product is pressed.
 * 
 * @author Gem
 */
public class AddSaveProductWindow extends AbstractWindowController {
    
    @FXML private CheckBox checkExisting;
    @FXML private ComboBox<Product> cbProducts;
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
    private PurchaseInvoice mInvoice;
    
    public AddSaveProductWindow(EditPurchaseWindow editPurchaseWindow) {
        super("Add Product", AddProductWindow.class.getResource("add_save_product.fxml"), editPurchaseWindow.getWindow());
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
                JavaFxObservable.changesOf(checkExisting.selectedProperty()).subscribe(select -> {
                    disableInputs(select.getNewVal());
                }),
                JavaFxObservable.changesOf(cbProducts.valueProperty()).subscribe(product -> {
                    Product p = product.getNewVal();
                    if (p != null) fillInFields(p);
                }),
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    if (tfName.getText().isEmpty() || cbUnits.getValue() == null ||
                            tfPrice.getText().isEmpty() || tfRetailPrice.getText().isEmpty() ||
                            tfQuantity.getText().isEmpty()) {
                        showInfoDialog("Invalid Input", "Please fill-in required fields: Product Name, "
                                + "Unit, Unit Price (PHP), Retail Price (PHP), Stock Quantity");
                    } else {
                        if (checkExisting.isSelected()) {
                            Optional<ButtonType> result = showConfirmDialog("Purchase Product", "Selected product's Retail Price might be changed "
                                + "afte this operation. Confirm?");
                            if (result.isPresent() && result.get() == ButtonType.OK) saveAndClose();
                        } else {
                            saveAndClose();
                        }
                    }
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

    public void show(PurchaseInvoice invoice) {
        if (invoice == null) {
            showInfoDialog("Invalid", "Please select invoice.");
            return;
        }
        mInvoice = invoice;
        super.show();
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getProducts();
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(products -> {
            showProgress(false);
            cbProducts.setItems(FXCollections.observableArrayList(products));
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching products.", err);
        }));
    }
    
    private void disableInputs(boolean disable) {
        // Disable fields except the Unit Price, Retail Price and SKU
        tfName.setDisable(disable);
        cbUnits.setDisable(disable);
        cbProducts.setDisable(!disable);
    }
    
    private void fillInFields(Product product) {
        tfName.setText(product.getName());
        tfSku.setText(product.getSku());
        cbUnits.setValue(product.getUnit());
        tfPrice.setText(String.format("%.2f", product.getUnitPrice()));
        tfRetailPrice.setText(String.format("%.2f", product.getRetailPrice()));
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
            EmbeddedDatabase database = EmbeddedDatabase.getInstance();
            
            PurchaseInvoiceItem item = new PurchaseInvoiceItem();                           // create PurchaseInvoiceItem object
            item.setInvoiceId(mInvoice.getId());
            item.setUnitPrice(Double.parseDouble(tfPrice.getText().trim()));                // set Unit Price
            item.setQuantity(Integer.parseInt(tfQuantity.getText().trim()));                // set Quantity
            item.setTotal(mTotal.get());                                                    // set Total
            item.setDate(mInvoice.getDate());

            Product product;
            if (checkExisting.isSelected() && cbProducts.getValue() != null) {              // if checkbox is selected
                product = cbProducts.getValue();                                            // get selected Product
                item.setProductId(product.getId());                                         // set PurchaseInvoiceItem productId
            } else {
                product = new Product();                                                    // else, create new Product object
                product.setName(tfName.getText());                                          // set Product name
                product.setSku(tfSku.getText());                                                // set Product SKU
                product.setUnit(cbUnits.getValue());                                            // set Product Unit
                product.setUnitPrice(Double.parseDouble(tfPrice.getText().trim()));             // set Product Unit Price
                product.setRetailPrice(Double.parseDouble(tfRetailPrice.getText().trim()));     // set Product Retail Price
                int id = database.addEntryReturnId(product);
                if (id != -1) {
                    item.setProductId(id);
                    // create Stock
                    Stock stock = new Stock();
                    stock.setProductId(id);
                    stock.setQuantity(item.getQuantity());
                    stock.setInStock(item.getQuantity());
                    stock.setQuantityOut(0);
                    database.addEntry(stock);
                }
            }
            item.setProduct(product);
            return database.addEntry(item);
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            showProgress(false);
            if (!success) showInfoDialog("Can't save purchase invoice item.", "");
            close();
            editPurchaseWindow.refresh();
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error while adding purchase invoice item.", err);
        }));
    }
    
    private void showProgress(boolean show) {
        progressBar.setVisible(show);
    }

    @Override
    public void onClose() {
        checkExisting.setSelected(false);
        cbProducts.setValue(null);
        tfName.clear();
        tfSku.clear();
        tfPrice.setText("0");
        tfRetailPrice.setText("0");
        tfQuantity.setText("0");
        tfTotal.setText("0");
        cbUnits.setValue(null);
        showProgress(false);
        mInvoice = null;
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }

}
