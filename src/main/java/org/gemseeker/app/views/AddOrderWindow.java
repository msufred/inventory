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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
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
import org.gemseeker.app.data.Stock;
import org.gemseeker.app.views.frameworks.AbstractWindowController;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author Gem
 */
public class AddOrderWindow extends AbstractWindowController {
    
    @FXML private TextField tfName;
    @FXML private DatePicker datePicker;
    @FXML private Button btnAdd;
    @FXML private Label lblTotal;
    @FXML private TableView<OrderItem> itemsTable;
    @FXML private TableColumn<OrderItem, Product> colName;
    @FXML private TableColumn<OrderItem, Product> colUnit;
    @FXML private TableColumn<OrderItem, Product> colPriceBefore;
    @FXML private TableColumn<OrderItem, Double> colDiscount;
    @FXML private TableColumn<OrderItem, Double> colPriceAfter;
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
        colName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colName.setCellFactory(col -> new ProductNameTableCell<>());
        colUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colPriceBefore.setCellValueFactory(new PropertyValueFactory<>("product"));
        colPriceBefore.setCellFactory(col -> new ProductPriceTableCell<>());
        colDiscount.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colPriceAfter.setCellValueFactory(new PropertyValueFactory<>("discountedPrice"));
        colPriceAfter.setCellFactory(col -> new TableCell<OrderItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) setText(Utils.getMoneyFormat(item));
                else setText("");
            }
        });
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("listPrice"));
        colTotal.setCellFactory(col -> new TableCell<OrderItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) setText(Utils.getMoneyFormat(item));
                else setText("");
            }
        });
        
        itemsTable.setItems(orderItems);
        
        disposables.addAll(
                JavaFxObservable.changesOf(mTotal).subscribe(value -> {
                    if (value.getNewVal() != null) {
                        lblTotal.setText(String.format("P %.2f", value.getNewVal()));
                    }
                }),
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
                    addOrderItemWindow.show();
                }),
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    if (tfName.getText().isEmpty() || datePicker.getValue() == null ||
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
        datePicker.setValue(LocalDate.now());
    }
    
    private void saveAndClose() {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            // Save Order entry
            Order order = new Order();
            order.setName(tfName.getText());
            order.setDate(datePicker.getValue());
            order.setTotal(mTotal.get());
            return EmbeddedDatabase.getInstance().addEntryReturnId(order);
        }).flatMap(id -> Single.fromCallable(() -> {
            // Save Order Item entries
            if (id != -1) {
                for (OrderItem orderItem : orderItems) {
                    orderItem.setOrderId(id);
                    EmbeddedDatabase.getInstance().addEntry(orderItem);
                }
            }
            return id;
        })).flatMap(id -> Single.fromCallable(() -> {
            // Update Inventory
            if (id != -1) {
                for (OrderItem orderItem : orderItems) {
                    Product p = orderItem.getProduct();
                    Stock s = EmbeddedDatabase.getInstance().getStock(p.getId());
                    if (s != null) {
                        int qtyOut = s.getQuantityOut() + orderItem.getQuantity();
                        EmbeddedDatabase.getInstance().updateEntry("stocks", "quantity_out", qtyOut, 
                                "id", s.getId());
                    }
                }
            }
            return id;
        })).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(id -> {
            showProgress(false);
            if (id == -1) {
                
            }
            close();
            ordersPanel.onResume();
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error occurred while saving order data.", err);
        }));
    }
    
    public void addOrderItem(OrderItem orderItem) {
        double newTotal = mTotal.get() + orderItem.getListPrice();
        mTotal.set(newTotal);
        orderItems.add(orderItem);
    }

    private void showProgress(boolean show) {
        progressBar.setVisible(show);
    }
    
    @Override
    public void onClose() {
        tfName.clear();
        datePicker.setValue(null);
        itemsTable.setItems(null);
        orderItems.clear();
        mTotal.set(0);
    }

    @Override
    public void onDispose() {
        disposables.dispose();
        addOrderItemWindow.onDispose();
    }

}
