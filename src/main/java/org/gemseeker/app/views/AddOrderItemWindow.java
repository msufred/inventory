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
import org.gemseeker.app.data.OrderItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.Stock;
import org.gemseeker.app.views.frameworks.AbstractWindowController;

/**
 *
 * @author Gem
 */
public class AddOrderItemWindow extends AbstractWindowController {
    
    @FXML private ComboBox<Stock> cbProducts;
    @FXML private Label lblDate;
    @FXML private Label lblSupplier;
    @FXML private Label lblStock;
    @FXML private Label lblPrice;
    @FXML private TextField tfDiscount;
    @FXML private TextField tfPriceAfter;
    @FXML private TextField tfQuantity;
    @FXML private TextField tfTotal;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private ProgressBar progressBar;
    
    private final AddOrderWindow addOrderWindow;
    private final CompositeDisposable disposables;
    
    public AddOrderItemWindow(AddOrderWindow addOrderWindow) {
        super("Add Order Item", AddOrderItemWindow.class.getResource("add_order_item.fxml"), addOrderWindow.getWindow());
        this.addOrderWindow = addOrderWindow;
        disposables = new CompositeDisposable();
    }

    @Override
    protected void initWindow(Stage stage) {
        super.initWindow(stage);
        stage.initModality(Modality.WINDOW_MODAL);
    }

    @Override
    public void onLoad() {
        Utils.setAsIntegerTextField(tfDiscount);
        Utils.setAsIntegerTextField(tfQuantity);
        
        disposables.addAll(
                JavaFxObservable.changesOf(cbProducts.valueProperty()).subscribe(product -> {
                    recalculate();
                }),
                JavaFxObservable.changesOf(tfDiscount.textProperty()).subscribe(text -> {
                    recalculate();
                }),
                JavaFxObservable.changesOf(tfQuantity.textProperty()).subscribe(text -> {
                    recalculate();
                }),
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    if (cbProducts.getValue() == null || tfQuantity.getText().isEmpty() ||
                            tfDiscount.getText().isEmpty()) {
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
    
    private void recalculate() {
        Stock s = cbProducts.getValue();
        if (s != null) {
            Product p = s.getProduct();
            
            // display product details
            lblDate.setText(Utils.dateTimeFormat.format(p.getDate()));
            lblSupplier.setText(p.getSupplier());
            lblStock.setText(Utils.getMoneyFormat((int) s.getQuantity() - s.getQuantityOut()));
            lblPrice.setText("P " + Utils.getMoneyFormat(p.getUnitPrice()));
            
            // calculate
            double price = p.getUnitPrice();
            double discount = 0;
            if (!tfDiscount.getText().isEmpty()) {
                discount = Double.parseDouble(tfDiscount.getText().trim()) / 100;
            }
            double priceAfter = price - (price * discount);
            tfPriceAfter.setText(String.format("%.2f", priceAfter));
            
            int quantity = 0;
            if (!tfQuantity.getText().isEmpty()) {
                quantity = Integer.parseInt(tfQuantity.getText().trim());
            }
            
            double total = priceAfter * quantity;
            tfTotal.setText(String.format("%.2f", total));
        }
    }
    
    private void saveAndClose() {
        Product product = cbProducts.getValue().getProduct();
        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(product.getId());
        double discount = Integer.parseInt(tfDiscount.getText().trim()) / 100;
        orderItem.setDiscount(discount);
        double discountedPrice = product.getUnitPrice() - (product.getUnitPrice() * discount);
        orderItem.setDiscountedPrice(discountedPrice);
        int qty = Integer.parseInt(tfQuantity.getText().trim());
        orderItem.setQuantity(qty);
        orderItem.setListPrice(discountedPrice * qty);
        orderItem.setQuantityOut(0);
        orderItem.setTotalOut(0);
        orderItem.setProduct(product);
        
        addOrderWindow.addOrderItem(orderItem);
        close();
    }

    @Override
    public void show() {
        super.show();
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getStocks();
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(stocks -> {
            showProgress(false);
            cbProducts.setItems(FXCollections.observableArrayList(stocks));
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Failed to fetch products.", err);
        }));
    }

    private void showProgress(boolean show) {
        progressBar.setVisible(show);
    }
    
    @Override
    public void onClose() {
        cbProducts.getItems().clear();
        lblDate.setText("No Product Selected");
        lblSupplier.setText("No Product Selected");
        lblStock.setText("0");
        lblPrice.setText("0");
        tfDiscount.setText("0");
        tfPriceAfter.clear();
        tfQuantity.setText("1");
        tfTotal.clear();
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }

}
