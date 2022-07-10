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
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Order;
import org.gemseeker.app.data.OrderItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.ShipperStock;
import org.gemseeker.app.views.frameworks.AbstractWindowController;

/**
 *
 * @author Gem
 */
public class EditOrderItemWindow extends AbstractWindowController {
    
    @FXML private TextField tfName;
    @FXML private TextField tfSupplier;
    @FXML private TextField tfInStock;
    @FXML private TextField tfRetailPrice;
    @FXML private TextField tfQuantity;
    @FXML private TextField tfTotal;
    @FXML private ProgressBar progressBar;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    
    private final EditOrderWindow editOrderWindow;
    private final CompositeDisposable disposables;
    
    private Order mOrder;
    private OrderItem mOrderItem;
    private ShipperStock mStock;
    
    public EditOrderItemWindow(EditOrderWindow editOrderWindow) {
        super("Edit Order Item", EditOrderItemWindow.class.getResource("edit_order_item.fxml"),
                editOrderWindow.getWindow());
        this.editOrderWindow = editOrderWindow;
        disposables = new CompositeDisposable();
    }

    @Override
    public void onLoad() {
        Utils.setAsIntegerTextField(tfQuantity);
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    saveAndClose();
                }),
                JavaFxObservable.actionEventsOf(btnCancel).subscribe(evt -> {
                    onClose();
                }),
                JavaFxObservable.changesOf(tfQuantity.textProperty()).subscribe(text -> {
                    recalculate();
                })
        );
    }

    public void show(Order order, OrderItem orderItem) {
        if (order == null && orderItem == null) {
            showInfoDialog("Invalid", "No selected order item.");
            return;
        }
        super.show();
        mOrder = order;
        mOrderItem = orderItem;
        
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getShipperStock(mOrder.getShipperId(), mOrderItem.getProductId());
        }).flatMap(stock -> Single.fromCallable(() -> {
            if (stock != null) {
                stock.setProduct(EmbeddedDatabase.getInstance().getProductById(stock.getProductId()));
            }
            return stock;
        })).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(stock -> {
            if (stock == null) {
                showInfoDialog("Invalid", "No shipper stock data.");
                close();
                return;
            }
            
            Product product = stock.getProduct();
            if (product == null) {
                showInfoDialog("Invalid", "No product data.");
                close();
                return;
            }
            
            tfName.setText(product.getName());
            tfSupplier.setText(product.getSupplier());
            tfRetailPrice.setText(Utils.toMoneyFormat(product.getRetailPrice()));
            tfInStock.setText(stock.getInStock() + "");
            tfQuantity.setText(mOrderItem.getQuantity() + "");
            recalculate();
            
            mStock = stock;
            showProgress(false);
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error while retrieving shipper stock data.", err);
        }));
    }
    
    private void recalculate() {
        Product p = mOrderItem.getProduct();
        if (p != null) {
            // calculate
            double price = p.getRetailPrice();
            int quantity = 0;
            if (!tfQuantity.getText().isEmpty()) {
                quantity = Integer.parseInt(tfQuantity.getText().trim());
            }
            double total = price * quantity;
            tfTotal.setText(String.format("%.2f", total));
        }
    }
    
    private void saveAndClose() {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            EmbeddedDatabase database = EmbeddedDatabase.getInstance();
            
            int qty = Integer.parseInt(tfQuantity.getText().trim());
            double total = mStock.getProduct().getRetailPrice() * qty;
            // difference between previous and current qty
            int stockDiff = mOrderItem.getQuantity() - qty;
            
            mOrderItem.setQuantity(qty);
            mOrderItem.setTotal(total);
            
            boolean success = database.executeQuery(mOrderItem.updateSQL());
            if (success) {
                int newStock = mStock.getInStock() + stockDiff;
                mStock.setInStock(newStock);
                database.executeQuery(mStock.updateSQL());
            }
            return success;
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            showProgress(false);
            if (!success) showInfoDialog("Updating Order Item Failed!", "");
            close();
            editOrderWindow.refresh();
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error while updating order item entry.", err);
        }));
    }
    
    private void showProgress(boolean show) {
        progressBar.setVisible(show);
    }
    
    @Override
    public void onClose() {
        tfName.clear();
        tfSupplier.clear();
        tfRetailPrice.clear();
        tfInStock.clear();
        tfQuantity.clear();
        tfTotal.clear();
        mOrderItem = null;
        mOrder = null;
        mStock = null;
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }

}
