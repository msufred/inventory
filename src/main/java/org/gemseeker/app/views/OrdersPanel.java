package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import java.time.LocalDate;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Order;
import org.gemseeker.app.data.OrderItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.frameworks.SplitController;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSupplierTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author Gem
 */
public class OrdersPanel extends AbstractPanelController {
    
    @FXML private Button btnAdd;
    @FXML private Button btnPrint;
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, LocalDate> colOrderDate;
    @FXML private TableColumn<Order, String> colOrderName;
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
    @FXML private Label lblOrderTotal;
    @FXML private SplitPane splitPane;
    private SplitController splitController;
    
    private final MainWindow mainWindow;
    private final CompositeDisposable disposables;
    
    private final SimpleDoubleProperty mOrderTotal = new SimpleDoubleProperty(0);
    
    private FilteredList<Order> filteredList;
    
    private final AddOrderWindow addOrderWindow;
    
    public OrdersPanel(MainWindow mainWindow) {
        super(InventoryPanel.class.getResource("orders.fxml"));
        this.mainWindow = mainWindow;
        disposables = new CompositeDisposable();
        
        addOrderWindow = new AddOrderWindow(this, mainWindow.getWindow());
    }

    @Override
    public void onLoad() {
        // Order Table Columns
        colOrderDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colOrderName.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        // OrderItems Table Columns
        colItemName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemName.setCellFactory(col -> new ProductNameTableCell<>());
        colItemSupplier.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemSupplier.setCellFactory(col -> new ProductSupplierTableCell<>());
        colItemUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colItemPriceBefore.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemPriceBefore.setCellFactory(col -> new ProductPriceTableCell<>());
        colItemDiscount.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colItemPriceAfter.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colItemPriceAfter.setCellFactory(col -> new TableCell<OrderItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) setText(Utils.getMoneyFormat(item));
                else setText("");
            }
        });
        colItemQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colItemTotal.setCellValueFactory(new PropertyValueFactory<>("listPrice"));
        colItemTotal.setCellFactory(col -> new TableCell<OrderItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) setText(Utils.getMoneyFormat(item));
                else setText("");
            }
        });
        colItemQuantityOut.setCellValueFactory(new PropertyValueFactory<>("quantityOut"));
        colItemTotalOut.setCellValueFactory(new PropertyValueFactory<>("totalOut"));
        colItemTotalOut.setCellFactory(col -> new TableCell<OrderItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) setText(Utils.getMoneyFormat(item));
                else setText("");
            }
        });
        
        disposables.addAll(
                JavaFxObservable.changesOf(mOrderTotal).subscribe(value -> {
                    if (value.getNewVal() != null) {
                        lblOrderTotal.setText(String.format("P %.2f", value.getNewVal()));
                    }
                }),
                JavaFxObservable.changesOf(ordersTable.getSelectionModel().selectedItemProperty()).subscribe(order -> {
                    if (order.getNewVal() != null) {
                        if (!splitController.isTargetVisible()) {
                            splitController.showTarget();
                        }
                        getOrderItems(order.getNewVal());
                    } else {
                        splitController.hideTarget();
                    }
                }),
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
                    addOrderWindow.show();
                }),
                JavaFxObservable.actionEventsOf(btnPrint).subscribe(evt -> {
                    
                })
        );
        
        splitController = new SplitController(splitPane, SplitController.Target.LAST);
        splitController.hideTarget();
    }

    @Override
    public void onPause() {
        ordersTable.getSelectionModel().clearSelection();
        splitController.hideTarget();
    }

    @Override
    public void onResume() {
        mainWindow.showProgress(true, "Fetching orders...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getOrders();
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(orders -> {
            mainWindow.showProgress(false);
            filteredList = new FilteredList<>(FXCollections.observableArrayList(orders), o -> true);
            ordersTable.setItems(filteredList);
            ordersTable.getSelectionModel().clearSelection();
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching orders data.", err);
        }));
    }

    private void getOrderItems(Order order) {
        mainWindow.showProgress(true, "Fetching order items..");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getOrderItems(order.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(orderItems -> {
            mainWindow.showProgress(false);
            orderItemsTable.setItems(FXCollections.observableArrayList(orderItems));
            mOrderTotal.set(order.getTotal());
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Datbase Error", "Error occurred while fetching order items.", err);
        }));
    }
    
    @Override
    public void onDispose() {
        disposables.dispose();
        addOrderWindow.onDispose();
    }

}
