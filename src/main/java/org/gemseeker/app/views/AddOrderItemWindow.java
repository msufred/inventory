package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.OrderItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.Stock;
import org.gemseeker.app.views.frameworks.AbstractWindowController;
import org.gemseeker.app.views.tablecells.PriceTableCell;
import org.gemseeker.app.views.tablecells.StockInStockTableCell;

/**
 *
 * @author Gem
 */
public class AddOrderItemWindow extends AbstractWindowController {
    
    @FXML private TextField tfSearch;
    
    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colSupplier;
    @FXML private TableColumn<Product, Stock> colStock;
    @FXML private TableColumn<Product, Double> colRetailPrice;
    
    private FilteredList<Product> filteredList;
    
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
        
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStock.setCellFactory(col -> new StockInStockTableCell<>());
        colRetailPrice.setCellValueFactory(new PropertyValueFactory<>("retailPrice"));
        colRetailPrice.setCellFactory(col -> new PriceTableCell<>());
        
        disposables.addAll(
                JavaFxObservable.changesOf(tfSearch.textProperty()).subscribe(text -> {
                    String searchText = text.getNewVal();
                    if (searchText != null && filteredList != null) {
                        if (searchText.isEmpty()) filteredList.setPredicate(p -> true);
                        else filteredList.setPredicate(p -> p.getName().toLowerCase().contains(searchText.toLowerCase()));
                    }
                }),
                JavaFxObservable.changesOf(productsTable.getSelectionModel().selectedItemProperty()).subscribe(product -> {
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
        Product p = selected.get();
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
            filteredList = new FilteredList<>(FXCollections.observableArrayList(products), p -> true);
            productsTable.setItems(filteredList);
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
        tfQuantity.setText("1");
        tfTotal.clear();
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }

}
