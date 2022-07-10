package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import java.util.Optional;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Order;
import org.gemseeker.app.data.OrderItem;
import org.gemseeker.app.data.Product;
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
public class EditOrderWindow extends AbstractWindowController {
    
    @FXML private TextField tfShipper;
    @FXML private DatePicker datePicker;
    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnRemove;
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
    private final SimpleIntegerProperty selectedIndex = new SimpleIntegerProperty();
    private final SimpleDoubleProperty mTotal = new SimpleDoubleProperty(0);
    
    private final AddSaveOrderItemWindow addOrderItemWindow;
    private final EditOrderItemWindow editOrderItemWindow;
    
    private Order mOrder;
    
    public EditOrderWindow(OrdersPanel ordersPanel, Stage mainStage) {
        super("Edit Order", AddOrderWindow.class.getResource("edit_order.fxml"), mainStage);
        this.ordersPanel = ordersPanel;
        disposables = new CompositeDisposable();
        addOrderItemWindow = new AddSaveOrderItemWindow(this);
        editOrderItemWindow = new EditOrderItemWindow(this);
    }

    @Override
    protected void initWindow(Stage stage) {
        super.initWindow(stage);
        stage.initModality(Modality.APPLICATION_MODAL);
    }

    @Override
    public void onLoad() {
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
        selectedIndex.bind(itemsTable.getSelectionModel().selectedIndexProperty());
        
        disposables.addAll(
                JavaFxObservable.changesOf(mTotal).subscribe(value -> {
                    if (value.getNewVal() != null) {
                        lblTotal.setText("P " + Utils.toMoneyFormat(value.getNewVal().doubleValue()));
                    }
                }),
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
                    addOrderItemWindow.show(mOrder);
                }),
                JavaFxObservable.actionEventsOf(btnEdit).subscribe(evt -> {
                    OrderItem selected = itemsTable.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        editOrderItemWindow.show(mOrder, selected);
                    }
                }),
                JavaFxObservable.actionEventsOf(btnRemove).subscribe(evt -> {
                    OrderItem selected = itemsTable.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        Optional<ButtonType> result = showConfirmDialog("Delete Order Item", "Removing this item "
                                + "will update current product stocks and truck inventory. Proceed?", 
                                ButtonType.YES, ButtonType.CANCEL);
                        if (result.isPresent() && result.get() == ButtonType.YES) {
                            removeItem(selected);
                        }
                    }
                }),
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    saveAndClose();
                }),
                JavaFxObservable.actionEventsOf(btnCancel).subscribe(evt -> {
                    close();
                }),
                JavaFxObservable.changesOf(selectedIndex).subscribe(index -> {
                    btnEdit.setDisable(index.getNewVal().intValue() == -1);
                    btnRemove.setDisable(index.getNewVal().intValue() == -1);
                })
        );
    }

    public void show(Order order) {
        if (order == null) {
            showInfoDialog("Invalid", "No selected order.");
            return;
        }
        mOrder = order;
        super.show();
        tfShipper.setText(mOrder.getShipper().getName());
        datePicker.setValue(mOrder.getDate());
        refresh();
    }
    
    public void refresh() {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getOrderItems(mOrder.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(items -> {
            showProgress(false);
            orderItems.setAll(items);
            
            // calculate
            double total = 0;
            for (OrderItem item : items) {
                total += item.getTotal();
            }
            mTotal.set(total);
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error while retrieving order items", err);
        }));
    }
    
    private void saveAndClose() {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            EmbeddedDatabase database = EmbeddedDatabase.getInstance();
            // Save Order entry
            mOrder.setDate(datePicker.getValue());
            mOrder.setTotal(mTotal.get());
            return database.executeQuery(mOrder.updateSQL());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            showProgress(false);
            if (!success) showInfoDialog("Failed to add new Order entry.", "");
            close();
            ordersPanel.onResume();
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error occurred while saving order data.", err);
        }));
    }
    
    private void removeItem(OrderItem item) {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            EmbeddedDatabase database = EmbeddedDatabase.getInstance();
            boolean removed = database.deleteEntry("order_items", "id", item.getId());
            if (removed) {
                // update product stocks
                Product p = database.getProductById(item.getProductId());
                if (p != null) {
                    Stock s = p.getStock();
                    int inStock = s.getInStock() + item.getQuantity();
                    s.setInStock(inStock);
                    int out = s.getQuantityOut() - item.getQuantity();
                    s.setQuantityOut(out);
                    database.executeQuery(s.updateSQL());
                    
                    // update shipper stocks
                    ShipperStock ss = database.getShipperStock(mOrder.getShipperId(), p.getId());
                    if (ss != null) {
                        int ssInStock = ss.getInStock() - item.getQuantity();
                        ss.setInStock(ssInStock);
                        database.executeQuery(ss.updateSQL());
                    }
                }
            }
            
            return removed;
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            showProgress(false);
            if (!success) showInfoDialog("Failed to remove order item completely.", "");
            refresh();
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error while removing order item.", err);
        }));
    }
    
    private void showProgress(boolean show) {
        progressBar.setVisible(show);
    }
    
    @Override
    public void onClose() {
        datePicker.setValue(null);      // reset DatePicker
        orderItems.clear();             // clear itemsTable items
        mTotal.set(0);                  // clear total
        mOrder = null;
    }

    @Override
    public void onDispose() {
        disposables.dispose();
        addOrderItemWindow.onDispose();
    }

}
