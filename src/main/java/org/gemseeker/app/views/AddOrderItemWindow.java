package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
    
    @FXML private ListView<Product> productsList;
    @FXML private Label lblSupplier;
    @FXML private Label lblStock;
    @FXML private Label lblPrice; // retail price
    @FXML private TextField tfQuantity;
    @FXML private TextField tfTotal;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private ProgressBar progressBar;
    
    private final AddOrderWindow addOrderWindow;
    private final CompositeDisposable disposables;
    
    private final SimpleObjectProperty<Product> selected = new SimpleObjectProperty<>();
    
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
        Utils.setAsIntegerTextField(tfQuantity);
        
        disposables.addAll(
                JavaFxObservable.changesOf(productsList.getSelectionModel().selectedItemProperty()).subscribe(product -> {
                    selected.set(product.getNewVal());
                    btnSave.setDisable(selected.get() == null && tfQuantity.getText().isEmpty());
                    recalculate();
                }),
                JavaFxObservable.changesOf(tfQuantity.textProperty()).subscribe(text -> {
                    btnSave.setDisable(selected.get() == null && text.getNewVal().isEmpty());
                    recalculate();
                }),
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    if (selected.get() == null || tfQuantity.getText().isEmpty()) {
                        showInfoDialog("Invalid Input", "Please select Product & fill-in required fields.");
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
        Product p = productsList.getSelectionModel().getSelectedItem();
        if (p != null) {
            Stock s = p.getStock();
            
            // display product details
            lblSupplier.setText(p.getSupplier());
            lblStock.setText(s.getInStock() + "");
            lblPrice.setText("P " + Utils.getMoneyFormat(p.getRetailPrice()));
            
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
        Product product = selected.get();
        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(product.getId());
        int qty = Integer.parseInt(tfQuantity.getText().trim());
        orderItem.setQuantity(qty);
        orderItem.setTotal(product.getRetailPrice() * qty);
        orderItem.setProduct(product);
        
        addOrderWindow.addOrderItem(orderItem);
        close();
    }

    @Override
    public void show() {
        super.show();
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getProducts();
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(products -> {
            showProgress(false);
            productsList.setItems(FXCollections.observableArrayList(products));
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
        selected.set(null);
        lblSupplier.setText("No Product Selected");
        lblStock.setText("0");
        lblPrice.setText("0");
        tfQuantity.setText("1");
        tfTotal.clear();
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }

}
