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
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.DeliveryInvoiceItem;
import org.gemseeker.app.data.Shipper;
import org.gemseeker.app.data.ShipperStock;
import org.gemseeker.app.views.frameworks.AbstractWindowController;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class AddDeliveryItemWindow extends AbstractWindowController {

    @FXML private ComboBox<ShipperStock> cbProducts;
    @FXML private Label lblPrice;
    @FXML private Label lblStock;
    @FXML private TextField tfDiscount;
    @FXML private TextField tfDiscountedPrice;
    @FXML private TextField tfQuantity;
    @FXML private TextField tfTotal;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private ProgressBar progressBar;
    
    private final AddDeliveryWindow addInvoiceWindow;
    private final CompositeDisposable disposables;
    
    public AddDeliveryItemWindow(AddDeliveryWindow addInvoiceWindow) {
        super("Add Invoice Item", AddDeliveryItemWindow.class.getResource("add_invoice_item.fxml"),
                addInvoiceWindow.getWindow());
        this.addInvoiceWindow = addInvoiceWindow;
        disposables = new CompositeDisposable();
    }

    @Override
    protected void initWindow(Stage stage) {
        super.initWindow(stage);
        stage.initModality(Modality.APPLICATION_MODAL);
    }
    
    @Override
    public void onLoad() {
        Utils.setAsNumericalTextField(tfDiscount);
        Utils.setAsIntegerTextField(tfQuantity);
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    if (cbProducts.getValue() != null && !tfQuantity.getText().isEmpty()) {
                        saveAndClose();
                    } else {
                        showInfoDialog("Invalid Input", "Please fill-in required fields.");
                    }
                }),
                JavaFxObservable.actionEventsOf(btnCancel).subscribe(evt -> {
                    close();
                }),
                JavaFxObservable.changesOf(cbProducts.valueProperty()).subscribe(stock -> {
                    if (stock.getNewVal() != null) {
                        ShipperStock item = stock.getNewVal();
                        lblPrice.setText(String.format("%.2f", item.getProduct().getRetailPrice()));
                        lblStock.setText(item.getInStock() + "");
                        recalculate();
                    }
                }),
                JavaFxObservable.changesOf(tfDiscount.textProperty()).subscribe(text -> {
                    recalculate();
                }),
                JavaFxObservable.changesOf(tfQuantity.textProperty()).subscribe(text -> {
                    recalculate();
                })
        );
    }

    public void show(Shipper shipper) {
        super.show();
        if (shipper != null) {
            showProgress(true);
            disposables.add(Single.fromCallable(() -> {
                return EmbeddedDatabase.getInstance().getShipperStocks(shipper.getId());
            }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(items -> {
                showProgress(false);
                cbProducts.setItems(FXCollections.observableArrayList(items));
            }, err -> {
                showProgress(false);
                showErrorDialog("Database Error", "Error while fetching shipper stocks items.", err);
            }));
        }
    }
    
    private void recalculate() {
        ShipperStock item = cbProducts.getValue();
        if (item != null) {
            double discount = 0;
            if (!tfDiscount.getText().isEmpty()) {
                discount = Double.parseDouble(tfDiscount.getText().trim());
            }
            
            double retail = item.getProduct().getRetailPrice();
            double less = retail * discount;
            double discountedPrice = retail - less;
            tfDiscountedPrice.setText(String.format("%.2f", discountedPrice));
            
            int quantity = 0;
            if (!tfQuantity.getText().isEmpty()) {
                quantity = Integer.parseInt(tfQuantity.getText().trim());
            }
            
            double total = discountedPrice * quantity;
            tfTotal.setText(String.format("%.2f", total));
        }
    }
    
    private void saveAndClose() {
        DeliveryInvoiceItem item = new DeliveryInvoiceItem();
        ShipperStock stock = cbProducts.getValue();
        item.setProductId(stock.getProductId());
        int qty = Integer.parseInt(tfQuantity.getText().trim());
        item.setQuantity(qty);
        double discount = Double.parseDouble(tfDiscount.getText().trim());
        item.setDiscount(discount);
        double retail = stock.getProduct().getRetailPrice();
        double less = retail * discount;
        double discountedPrice = retail - less;
        item.setDiscountedPrice(discountedPrice);
        item.setListPrice(qty * discountedPrice);
        item.setProduct(stock.getProduct());
        addInvoiceWindow.addInvoiceItem(item);
        close();
    }

    private void showProgress(boolean show) {
        progressBar.setVisible(show);
    }
    
    @Override
    public void onClose() {
        cbProducts.setItems(null);
        lblPrice.setText("0.00");
        lblStock.setText("0");
        tfDiscount.setText("0");
        tfDiscountedPrice.setText("0.00");
        tfQuantity.setText("1");
        tfTotal.setText("0.00");
        showProgress(false);
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }
    
}
