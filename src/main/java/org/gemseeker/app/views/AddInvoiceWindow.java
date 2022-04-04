package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import java.time.LocalDate;
import java.util.ArrayList;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
import org.gemseeker.app.data.Invoice;
import org.gemseeker.app.data.InvoiceItem;
import org.gemseeker.app.data.Order;
import org.gemseeker.app.data.OrderItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.views.frameworks.AbstractWindowController;
import org.gemseeker.app.views.tablecells.DiscountTableCell;
import org.gemseeker.app.views.tablecells.PriceTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class AddInvoiceWindow extends AbstractWindowController {
    
    @FXML private TextField tfInvoiceId;
    @FXML private ComboBox<Order> cbOrders;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> cbPaymentTypes;
    @FXML private TextField tfCustomer;
    @FXML private TextField tfAddress;
    @FXML private Button btnAdd;
    @FXML private Button btnSave;
    @FXML private Button btnPrint;
    @FXML private Button btnCancel;
    @FXML private Label lblTotal;
    @FXML private TableView<InvoiceItem> itemsTable;
    @FXML private TableColumn<InvoiceItem, Product> colName;
    @FXML private TableColumn<InvoiceItem, Product> colUnit;
    @FXML private TableColumn<InvoiceItem, Product> colPriceBefore;
    @FXML private TableColumn<InvoiceItem, Double> colDiscount;
    @FXML private TableColumn<InvoiceItem, Double> colPriceAfter;
    @FXML private TableColumn<InvoiceItem, Integer> colQuantity;
    @FXML private TableColumn<InvoiceItem, Double> colTotal;
    @FXML private ProgressBar progressBar;
    
    private final InvoicesPanel invoicesPanel;
    private final CompositeDisposable disposables;
    
    private final ObservableList<InvoiceItem> items = FXCollections.observableArrayList();
    private final SimpleDoubleProperty mTotal = new SimpleDoubleProperty(0);
    
    private final AddInvoiceItemWindow addInvoiceItemWindow;
    
    public AddInvoiceWindow(InvoicesPanel invoicesPanel, Stage mainStage) {
        super("Add Invoice", AddInvoiceWindow.class.getResource("add_invoice.fxml"), mainStage);
        this.invoicesPanel = invoicesPanel;
        disposables = new CompositeDisposable();
        
        addInvoiceItemWindow = new AddInvoiceItemWindow(this);
    }

    @Override
    protected void initWindow(Stage stage) {
        super.initWindow(stage);
        stage.initModality(Modality.APPLICATION_MODAL);
    }

    @Override
    public void onLoad() {
        cbPaymentTypes.setItems(FXCollections.observableArrayList("Cash", "Cheque", "Receivable"));
        
        colName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colName.setCellFactory(col -> new ProductNameTableCell<>());
        colUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colPriceBefore.setCellValueFactory(new PropertyValueFactory<>("product"));
        colPriceBefore.setCellFactory(col -> new ProductPriceTableCell<>());
        colDiscount.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colDiscount.setCellFactory(col -> new DiscountTableCell<>());
        colPriceAfter.setCellValueFactory(new PropertyValueFactory<>("discountedPrice"));
        colPriceAfter.setCellFactory(col -> new PriceTableCell<>());
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("listPrice"));
        colTotal.setCellFactory(col -> new PriceTableCell<>());
        
        itemsTable.setItems(items);
        
        disposables.addAll(
                JavaFxObservable.changesOf(mTotal).subscribe(value -> {
                    if (value.getNewVal() != null) lblTotal.setText(String.format("P %.2f", value.getNewVal()));
                }), 
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
                    Order order = cbOrders.getValue();
                    if (order != null) addInvoiceItemWindow.show(order);
                }),
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    if (tfInvoiceId.getText().isEmpty() || cbOrders.getValue() == null ||
                            datePicker.getValue() == null || cbPaymentTypes.getValue() == null ||
                            tfCustomer.getText().isEmpty() || tfAddress.getText().isEmpty() ||
                            items.isEmpty()) {
                        showInfoDialog("Invalid Input", "Please fill-in required fields.");
                    } else {
                        saveAndClose();
                    }
                }),
                JavaFxObservable.actionEventsOf(btnPrint).subscribe(evt -> {
                    
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
            return EmbeddedDatabase.getInstance().getOrders();
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(orders -> {
            showProgress(false);
            cbOrders.setItems(FXCollections.observableArrayList(orders));
            datePicker.setValue(LocalDate.now());
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching orders.", err);
        }));
        
    }
    
    public void addInvoiceItem(InvoiceItem item) {
        if (item != null) {
            double newTotal = mTotal.get() + item.getListPrice();
            mTotal.set(newTotal);
            items.add(item);
        }
    }
    
    private void saveAndClose() {
        showProgress(true);
        
        Invoice invoice = new Invoice();
        invoice.setId(tfInvoiceId.getText().trim());
        invoice.setDate(datePicker.getValue());
        invoice.setOrderId(cbOrders.getValue().getId());
        invoice.setCustomer(tfCustomer.getText());
        invoice.setAddress(tfAddress.getText());
        invoice.setPaymentType(cbPaymentTypes.getValue());
        invoice.setTotal(mTotal.get());
        
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().addEntry(invoice);
        }).flatMap(success -> Single.fromCallable(() -> {
            if (success) {
                EmbeddedDatabase database = EmbeddedDatabase.getInstance();
                
                ArrayList<OrderItem> orderItems = database.getOrderItems(cbOrders.getValue().getId());
                
                for (InvoiceItem item : items) {
                    // Add Invoice entry
                    item.setInvoiceId(invoice.getId());
                    boolean added = database.addEntry(item);
                    
                    // Update OrderItem of Order
                    if (added) {
                        OrderItem orderItem = orderItems.stream()
                                .filter(oi -> oi.getProduct().getId() == item.getProductId())
                                .findAny()
                                .orElse(null);
                        if (orderItem != null) {
                            // update Quantity Out
                            int qtyOut = orderItem.getQuantityOut() + item.getQuantity();
                            database.updateEntry("order_items", "quantity_out", qtyOut, "id", orderItem.getId());
                            // update Total Out
                            double totalOut = orderItem.getTotalOut() + item.getListPrice();
                            database.updateEntry("order_items", "total_out", totalOut, "id", orderItem.getId());
                        }
                    }
                }
            }
            return success;
        })).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            showProgress(false);
            if (!success) {
                showInfoDialog("Failed to add Invoice", "");
            }
            close();
            invoicesPanel.onResume();
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error occurred while adding invoice entry.", err);
        }));
    }
    
    private void showProgress(boolean show) {
        progressBar.setVisible(show);
    }

    @Override
    public void onClose() {
        cbOrders.setValue(null);
        cbPaymentTypes.setValue(null);
        tfCustomer.clear();
        tfAddress.clear();
        items.clear();
        mTotal.set(0);
        showProgress(false);
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }
    
}
