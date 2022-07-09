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
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Order;
import org.gemseeker.app.data.OrderItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.Shipper;
import org.gemseeker.app.views.frameworks.AbstractWindowController;
import org.gemseeker.app.views.tablecells.DateTableCell;
import org.gemseeker.app.views.tablecells.PriceTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductRetailPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSupplierTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author Gem
 */
public class ShowOrdersWindow extends AbstractWindowController {
    
    @FXML private Label lblProduct;
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, LocalDate> colDate;
    @FXML private TableColumn<Order, Shipper> colShipper;
    @FXML private TableView<OrderItem> orderItemsTable;
    @FXML private TableColumn<OrderItem, Product> colName;
    @FXML private TableColumn<OrderItem, Product> colSupplier;
    @FXML private TableColumn<OrderItem, Product> colUnit; 
    @FXML private TableColumn<OrderItem, Product> colRetailPrice; 
    @FXML private TableColumn<OrderItem, Integer> colQuantity; 
    @FXML private TableColumn<OrderItem, Double> colTotal;
    @FXML private Label lblTotal;
    
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
        colShipper.setCellValueFactory(new PropertyValueFactory<>("shipper"));
        colShipper.setCellFactory(col -> new TableCell<Order, Shipper>() {
            @Override
            protected void updateItem(Shipper item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item.getName());
                } else setText("");
            }
        });
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> new PriceTableCell<>());
        
        // OrderItems Table Columns
        colName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colName.setCellFactory(col -> new ProductNameTableCell<>());
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("product"));
        colSupplier.setCellFactory(col -> new ProductSupplierTableCell<>());
        colUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colRetailPrice.setCellValueFactory(new PropertyValueFactory<>("product"));
        colRetailPrice.setCellFactory(col -> new ProductRetailPriceTableCell<>());
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("listPrice"));
        colTotal.setCellFactory(col -> new PriceTableCell<>());

        orderItemsTable.setItems(mItems);
        
        disposables.addAll(
                JavaFxObservable.changesOf(ordersTable.getSelectionModel().selectedItemProperty()).subscribe(order -> {
                    if (order.getNewVal() != null) {
                        getOrderItems(order.getNewVal());
                    }
                })
        );
    }
    
    public void show(Product product) {
        super.show();
        lblProduct.setText(product.getName() + "-" + product.getSupplier());
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getOrders(product.getId());
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
            lblTotal.setText("P " + Utils.toMoneyFormat(order.getTotal()));
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
