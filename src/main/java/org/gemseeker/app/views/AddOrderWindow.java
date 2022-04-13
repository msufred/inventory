package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import java.time.LocalDate;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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
import org.gemseeker.app.data.ShipperStock;
import org.gemseeker.app.data.Stock;
import org.gemseeker.app.views.frameworks.AbstractWindowController;
import org.gemseeker.app.views.tablecells.PriceTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductRetailPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author Gem
 */
public class AddOrderWindow extends AbstractWindowController {
    
    @FXML private ComboBox<Shipper> cbShippers;
    @FXML private DatePicker datePicker;
    @FXML private Button btnAdd;
    @FXML private Label lblTotal;
    @FXML private TableView<OrderItem> itemsTable;
    @FXML private TableColumn<OrderItem, Product> colName;
    @FXML private TableColumn<OrderItem, Product> colUnit;
    @FXML private TableColumn<OrderItem, Product> colRetailPrice;
    @FXML private TableColumn<OrderItem, Integer> colQuantity;
    @FXML private TableColumn<OrderItem, Double> colTotal;
    @FXML private ProgressBar progressBar;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    
    private final OrdersPanel ordersPanel;
    private final CompositeDisposable disposables;
    
    private final ObservableList<OrderItem> orderItems = FXCollections.observableArrayList();
    private final SimpleDoubleProperty mTotal = new SimpleDoubleProperty(0);
    
    private final AddOrderItemWindow addOrderItemWindow;
    
    public AddOrderWindow(OrdersPanel ordersPanel, Stage mainStage) {
        super("Add Order", AddOrderWindow.class.getResource("add_order.fxml"), mainStage);
        this.ordersPanel = ordersPanel;
        disposables = new CompositeDisposable();
        addOrderItemWindow = new AddOrderItemWindow(this);
    }

    @Override
    protected void initWindow(Stage stage) {
        super.initWindow(stage);
        stage.initModality(Modality.APPLICATION_MODAL);
    }

    @Override
    public void onLoad() {
        Utils.setSafeTextField(cbShippers.getEditor());
        colName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colName.setCellFactory(col -> new ProductNameTableCell<>());
        colUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colRetailPrice.setCellValueFactory(new PropertyValueFactory<>("product"));
        colRetailPrice.setCellFactory(col -> new ProductRetailPriceTableCell<>());
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> new PriceTableCell<>());
        
        itemsTable.setItems(orderItems);
        
        disposables.addAll(
                JavaFxObservable.changesOf(mTotal).subscribe(value -> {
                    if (value.getNewVal() != null) {
                        lblTotal.setText("P " + Utils.getMoneyFormat(value.getNewVal().doubleValue()));
                    }
                }),
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
                    addOrderItemWindow.show();
                }),
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    if (cbShippers.getEditor().getText().isEmpty() || datePicker.getValue() == null ||
                            itemsTable.getItems().isEmpty()) {
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

    @Override
    public void show() {
        super.show();
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getShippers();
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(shippers -> {
            showProgress(false);
            cbShippers.setItems(FXCollections.observableArrayList(shippers));
            datePicker.setValue(LocalDate.now());
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error while fetching shippers data", err);
        }));
    }
    
    private void saveAndClose() {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            EmbeddedDatabase database = EmbeddedDatabase.getInstance();
            
            // Save Shipper entry if not yet saved
            String shipperName = cbShippers.getEditor().getText();
            Shipper shipper = database.getShipper(shipperName);
            if (shipper == null) {
                shipper = new Shipper();
                shipper.setName(cbShippers.getEditor().getText());
                int shipperId = database.addEntryReturnId(shipper);
                if (shipperId == -1) return -1; // return if not saved
                shipper.setId(shipperId);
            }
            
            // Save Order entry
            Order order = new Order();
            order.setDate(datePicker.getValue());
            order.setShipperId(shipper.getId());
            order.setShipper(shipper);
            order.setTotal(mTotal.get());
            int orderId = database.addEntryReturnId(order);
            if (orderId == -1) return -1; // return if not saved
            
            // Save order items
            for (OrderItem item : orderItems) {
                item.setOrderId(orderId);
                item.setDate(order.getDate());
                boolean added = database.addEntry(item);
                if(added) {
                    // Check if product already exists in Shipper's inventory
                    ShipperStock shipperStock = database.getShipperStock(shipper.getId(), item.getProductId());
                    boolean success;
                    if (shipperStock == null) {
                        shipperStock = new ShipperStock();
                        shipperStock.setShipperId(shipper.getId());
                        shipperStock.setProductId(item.getProductId());
                        shipperStock.setInStock(item.getQuantity());
                        shipperStock.setDelivered(0);
                        shipperStock.setSales(0);
                        success = database.addEntry(shipperStock);
                    } else {
                        int newInStock = shipperStock.getInStock() + item.getQuantity();
                        shipperStock.setInStock(newInStock);
                        success = database.executeQuery(shipperStock.updateSQL());
                    }
                    
                    // update Stocks
                    if (success) {
                        Stock stock = database.getStock(item.getProductId());
                        if (stock != null) {
                            int qtyOut = stock.getQuantityOut() + item.getQuantity();
                            database.updateEntry("stocks", "quantity_out", qtyOut, "id", stock.getId());
                            
                            int inStock = stock.getInStock() - item.getQuantity();
                            database.updateEntry("stocks", "in_stock", inStock, "id", stock.getId());
                        }
                    }
                }
            }
            return orderId;
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(id -> {
            showProgress(false);
            if (id == -1) {
                showInfoDialog("Failed to add new Order entry.", "");
            }
            close();
            ordersPanel.onResume();
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error occurred while saving order data.", err);
        }));
    }
    
    public void addOrderItem(OrderItem orderItem) {
        double newTotal = mTotal.get() + orderItem.getTotal();
        mTotal.set(newTotal);
        orderItems.add(orderItem);
    }

    private void showProgress(boolean show) {
        progressBar.setVisible(show);
    }
    
    @Override
    public void onClose() {
        cbShippers.setValue(null);      // reset Shipper ComboBox
        datePicker.setValue(null);      // reset DatePicker
        orderItems.clear();             // clear itemsTable items
        mTotal.set(0);                  // clear total
    }

    @Override
    public void onDispose() {
        disposables.dispose();
        addOrderItemWindow.onDispose();
    }

}
