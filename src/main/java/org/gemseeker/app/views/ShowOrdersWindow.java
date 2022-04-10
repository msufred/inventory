package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Order;
import org.gemseeker.app.data.OrderItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.views.frameworks.AbstractWindowController;
import org.gemseeker.app.views.tablecells.DateTableCell;
import org.gemseeker.app.views.tablecells.DiscountTableCell;
import org.gemseeker.app.views.tablecells.PriceTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSupplierTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author Gem
 */
public class ShowOrdersWindow extends AbstractWindowController {
    
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, LocalDate> colDate;
    @FXML private TableColumn<Order, String> colName;
    @FXML private TableView<OrderItem> orderItemsTable;
    @FXML private TableColumn<OrderItem, Product> colItemName;
    @FXML private TableColumn<OrderItem, Product> colItemSupplier;
    @FXML private TableColumn<OrderItem, Product> colItemUnit; 
    @FXML private TableColumn<OrderItem, Product> colItemPriceBefore; 
    @FXML private TableColumn<OrderItem, Double> colItemDiscount; 
    @FXML private TableColumn<OrderItem, Double> colItemPriceAfter; 
    @FXML private TableColumn<OrderItem, Integer> colItemQuantity; 
    @FXML private TableColumn<OrderItem, Double> colItemTotal; 
    @FXML private TableColumn<OrderItem, Integer> colItemQuantityOut; 
    @FXML private TableColumn<OrderItem, Double> colItemTotalOut; 
    
    @FXML private ProgressBar progressBar;
    
    private final CompositeDisposable disposables;
    private FilteredList<Order> filteredList;
    private final ObservableList<OrderItem> mItems = FXCollections.observableArrayList();
    
    public ShowOrdersWindow(Stage mainStage) {
        super("Orders", ShowOrdersWindow.class.getResource("show_orders.fxml"), mainStage);
        disposables = new CompositeDisposable();
    }

    @Override
    protected void initWindow(Stage stage) {
        super.initWindow(stage);
        stage.initModality(Modality.APPLICATION_MODAL);
    }

    @Override
    public void onLoad() {
         // Order Table Columns
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDate.setCellFactory(col -> new DateTableCell<>());
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        // OrderItems Table Columns
        colItemName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemName.setCellFactory(col -> new ProductNameTableCell<>());
        colItemSupplier.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemSupplier.setCellFactory(col -> new ProductSupplierTableCell<>());
        colItemUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colItemPriceBefore.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemPriceBefore.setCellFactory(col -> new ProductUnitPriceTableCell<>());
        colItemDiscount.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colItemDiscount.setCellFactory(col -> new DiscountTableCell<>());
        colItemPriceAfter.setCellValueFactory(new PropertyValueFactory<>("discountedPrice"));
        colItemPriceAfter.setCellFactory(col -> new PriceTableCell<>());
        colItemQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colItemTotal.setCellValueFactory(new PropertyValueFactory<>("listPrice"));
        colItemTotal.setCellFactory(col -> new PriceTableCell<>());
        colItemQuantityOut.setCellValueFactory(new PropertyValueFactory<>("quantityOut"));
        colItemTotalOut.setCellValueFactory(new PropertyValueFactory<>("totalOut"));
        colItemTotalOut.setCellFactory(col -> new PriceTableCell<>());
        
        orderItemsTable.setItems(mItems);
        
        disposables.addAll(
                JavaFxObservable.changesOf(ordersTable.getSelectionModel().selectedItemProperty()).subscribe(order -> {
                    if (order.getNewVal() != null) {
                        getOrderItems(order.getNewVal());
                    }
                })
        );
    }
    
    public void show(int productId) {
        super.show();
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getOrders(productId);
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(orders -> {
            showProgress(false);
            filteredList = new FilteredList<>(FXCollections.observableArrayList(orders));
            ordersTable.setItems(filteredList);
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching orders", err);
        }));
    }
    
    private void getOrderItems(Order order) {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getOrderItems(order.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(items -> {
            showProgress(false);
            mItems.setAll(items);
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching order items.", err);
        }));
    }
    
    private void showProgress(boolean show) {
        progressBar.setVisible(show);
    }
    
    @Override
    public void onClose() {
        mItems.clear();
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }

}
