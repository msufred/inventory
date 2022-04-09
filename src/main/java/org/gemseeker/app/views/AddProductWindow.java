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
import javafx.scene.control.DatePicker;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.PurchaseInvoice;
import org.gemseeker.app.data.Stock;
import org.gemseeker.app.views.frameworks.AbstractWindowController;

/**
 *
 * @author Gem
 */
public class AddProductWindow extends AbstractWindowController {
    
    @FXML private TextField tfName;
    @FXML private TextField tfSku;
    @FXML private TextField tfPrice;
    @FXML private TextField tfQuantity;
    @FXML private TextField tfTotal;
    @FXML private ComboBox<String> cbUnits;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private ProgressBar progressBar;
    
    private final InventoryPanel inventoryPanel;
    private final AddPurchaseWindow addPurchaseWindow;
    private final CompositeDisposable disposables;
    
    private final SimpleDoubleProperty mTotal = new SimpleDoubleProperty(0);
    
    private PurchaseInvoice mInvoice;
    
    public AddProductWindow(AddPurchaseWindow addPurchaseWindow) {
        super("Add Product", AddProductWindow.class.getResource("add_product.fxml"), addPurchaseWindow.getWindow());
        this.inventoryPanel = null;
        this.addPurchaseWindow = addPurchaseWindow;
        disposables = new CompositeDisposable();
    }
    
    public AddProductWindow(InventoryPanel inventoryPanel, Stage mainStage) {
        super("Add Product", AddProductWindow.class.getResource("add_product.fxml"), mainStage);
        this.inventoryPanel = inventoryPanel;
        this.addPurchaseWindow = null;
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
                }),
                JavaFxObservable.changesOf(mTotal).subscribe(value -> {
                    if (value.getNewVal() != null) {
                        tfTotal.setText("P " + Utils.getMoneyFormat(value.getNewVal().doubleValue()));
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
        super.show();
        mInvoice = invoice;
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
        Product product = new Product();
        product.setName(tfName.getText());
        product.setSku(tfSku.getText());
        product.setUnit(cbUnits.getValue());
        product.setUnitPrice(Double.parseDouble(tfPrice.getText().trim()));
        product.setTotal(mTotal.get());
        
        if (mInvoice != null) {
            product.setDate(mInvoice.getDate());
            product.setSupplier(mInvoice.getSupplier());
        }
        
        if (inventoryPanel != null) {
            showProgress(true);
            disposables.add(Single.fromCallable(() -> {
                return EmbeddedDatabase.getInstance().addEntryReturnId(product);
            }).flatMap(id -> Single.fromCallable(() -> {
                if (id != -1) {
                    Stock stock = new Stock();
                    stock.setProductId(id);
                    stock.setQuantity(Integer.parseInt(tfQuantity.getText().trim()));
                    stock.setQuantityOut(0);
                    EmbeddedDatabase.getInstance().addEntry(stock);
                }
                return id;
            })).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(id -> {
                showProgress(false);
                if (id != -1) showInfoDialog("Failed to add Product entry", "");
                close();
                inventoryPanel.refreshSelectedInvoice();
            }));
        }
        
        if (addPurchaseWindow != null) {
            Stock stock = new Stock();
            stock.setQuantity(Integer.parseInt(tfQuantity.getText().trim()));
            stock.setQuantityOut(0);
            stock.setProduct(product);
            addPurchaseWindow.addProduct(stock);
            close();
        }
    }
    
    private void showProgress(boolean show) {
        progressBar.setVisible(show);
    }

    @Override
    public void onClose() {
        tfName.clear();
        tfSku.clear();
        tfPrice.setText("0");
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
