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
import org.gemseeker.app.data.InvoiceItem;
import org.gemseeker.app.data.Order;
import org.gemseeker.app.data.OrderItem;
import org.gemseeker.app.views.frameworks.AbstractWindowController;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class AddInvoiceItemWindow extends AbstractWindowController {

    @FXML private ComboBox<OrderItem> cbProducts;
    @FXML private Label lblPrice;
    @FXML private Label lblDiscount;
    @FXML private Label lblPriceAfter;
    @FXML private Label lblStock;
    @FXML private TextField tfQuantity;
    @FXML private TextField tfTotal;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private ProgressBar progressBar;
    
    private final AddInvoiceWindow addInvoiceWindow;
    private final CompositeDisposable disposables;
    
    public AddInvoiceItemWindow(AddInvoiceWindow addInvoiceWindow) {
        super("Add Invoice Item", AddInvoiceItemWindow.class.getResource("add_invoice_item.fxml"),
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
                JavaFxObservable.changesOf(cbProducts.valueProperty()).subscribe(orderItem -> {
                    if (orderItem.getNewVal() != null) {
                        OrderItem item = orderItem.getNewVal();
                        lblPrice.setText(String.format("%.2f", item.getProduct().getUnitPrice()));
                        lblDiscount.setText((int) (item.getDiscount() * 100) + "%");
                        lblPriceAfter.setText(String.format("%.2f", item.getDiscountedPrice()));
                        lblStock.setText((item.getQuantity() - item.getQuantityOut()) + "");
                        recalculate();
                    }
                }),
                JavaFxObservable.changesOf(tfQuantity.textProperty()).subscribe(text -> {
                    recalculate();
                })
        );
    }

    public void show(Order order) {
        super.show();
        if (order != null) {
            showProgress(true);
            disposables.add(Single.fromCallable(() -> {
                return EmbeddedDatabase.getInstance().getOrderItems(order.getId());
            }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(items -> {
                showProgress(false);
                cbProducts.setItems(FXCollections.observableArrayList(items));
            }, err -> {
                showProgress(false);
                showErrorDialog("Database Error", "Error while fetching order items.", err);
            }));
        }
    }
    
    private void recalculate() {
        OrderItem item = cbProducts.getValue();
        if (item != null) {
            int quantity = 0;
            if (!tfQuantity.getText().isEmpty()) {
                quantity = Integer.parseInt(tfQuantity.getText().trim());
            }
            
            double total = item.getDiscountedPrice() * quantity;
            tfTotal.setText(String.format("%.2f", total));
        }
    }
    
    private void saveAndClose() {
        InvoiceItem item = new InvoiceItem();
        OrderItem orderItem = cbProducts.getValue();
        item.setProductId(orderItem.getProductId());
        int qty = Integer.parseInt(tfQuantity.getText().trim());
        item.setQuantity(qty);
        double discount = orderItem.getDiscount();
        item.setDiscount(discount);
        item.setDiscountedPrice(orderItem.getDiscountedPrice());
        item.setListPrice(qty * item.getDiscountedPrice());
        item.setProduct(orderItem.getProduct());
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
        lblDiscount.setText("0");
        lblPriceAfter.setText("0.00");
        lblStock.setText("0");
        tfQuantity.setText("1");
        tfTotal.setText("0.00");
        showProgress(false);
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }
    
}
