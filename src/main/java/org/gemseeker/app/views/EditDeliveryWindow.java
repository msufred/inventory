package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.DeliveryInvoice;
import org.gemseeker.app.data.DeliveryInvoiceItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.Shipper;
import org.gemseeker.app.data.ShipperStock;
import org.gemseeker.app.views.frameworks.AbstractWindowController;
import org.gemseeker.app.views.tablecells.DiscountTableCell;
import org.gemseeker.app.views.tablecells.PriceTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductRetailPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class EditDeliveryWindow extends AbstractWindowController {
    
    @FXML private TextField tfInvoiceId;
    @FXML private TextField tfShipper;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> cbPaymentTypes;
    @FXML private TextField tfCustomer;
    @FXML private TextField tfAddress;
    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnRemove;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label lblTotal;
    
    @FXML private TableView<DeliveryInvoiceItem> itemsTable;
    @FXML private TableColumn<DeliveryInvoiceItem, Product> colName;
    @FXML private TableColumn<DeliveryInvoiceItem, Product> colUnit;
    @FXML private TableColumn<DeliveryInvoiceItem, Product> colPriceBefore;
    @FXML private TableColumn<DeliveryInvoiceItem, Double> colDiscount;
    @FXML private TableColumn<DeliveryInvoiceItem, Double> colPriceAfter;
    @FXML private TableColumn<DeliveryInvoiceItem, Integer> colQuantity;
    @FXML private TableColumn<DeliveryInvoiceItem, Double> colTotal;
    @FXML private ProgressBar progressBar;
    
    private final DeliveriesPanel invoicesPanel;
    private final CompositeDisposable disposables;
    
    private final ObservableList<DeliveryInvoiceItem> items = FXCollections.observableArrayList();
    private final SimpleDoubleProperty mTotal = new SimpleDoubleProperty(0);
    
//    private final AddDeliveryItemWindow addInvoiceItemWindow;
    
    private DeliveryInvoice mInvoice;
    
    public EditDeliveryWindow(DeliveriesPanel invoicesPanel, Stage mainStage) {
        super("Add Invoice", AddDeliveryWindow.class.getResource("edit_delivery.fxml"), mainStage);
        this.invoicesPanel = invoicesPanel;
        disposables = new CompositeDisposable();
        
//        addInvoiceItemWindow = new AddDeliveryItemWindow(this);
    }

    @Override
    protected void initWindow(Stage stage) {
        super.initWindow(stage);
        stage.initModality(Modality.APPLICATION_MODAL);
    }

    @Override
    public void onLoad() {
        Utils.setSafeTextField(tfInvoiceId);
        Utils.setSafeTextField(tfCustomer);
        Utils.setSafeTextField(tfAddress);
        
        cbPaymentTypes.setItems(FXCollections.observableArrayList("Cash", "Cheque", "Receivable"));
        cbPaymentTypes.setValue("Cash"); // default
        
        colName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colName.setCellFactory(col -> new ProductNameTableCell<>());
        colUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colPriceBefore.setCellValueFactory(new PropertyValueFactory<>("product"));
        colPriceBefore.setCellFactory(col -> new ProductRetailPriceTableCell<>());
        colDiscount.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colDiscount.setCellFactory(col -> new DiscountTableCell<>());
        colPriceAfter.setCellValueFactory(new PropertyValueFactory<>("discountedPrice"));
        colPriceAfter.setCellFactory(col -> new PriceTableCell<>());
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> new PriceTableCell<>());
        
        itemsTable.setItems(items);
        
        disposables.addAll(
                JavaFxObservable.changesOf(mTotal).subscribe(value -> {
                    if (value.getNewVal() != null) {
                        lblTotal.setText("P " + Utils.toMoneyFormat(value.getNewVal().doubleValue()));
                    }
                }), 
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
//                    Shipper shipper = cbShippers.getValue();
//                    if (shipper != null) addInvoiceItemWindow.show(shipper);
                }),
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    if (datePicker.getValue() == null || cbPaymentTypes.getValue() == null ||
                            tfCustomer.getText().isEmpty() || tfAddress.getText().isEmpty() ||
                            items.isEmpty()) {
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

    public void show(DeliveryInvoice invoice) {
        if (invoice == null) {
            showInfoDialog("Invalid", "No selected delivery invoice.");
            return;
        }
        mInvoice = invoice;
        super.show();
        
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getShipperById(mInvoice.getShipperId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(shipper -> {
            showProgress(false);
            tfShipper.setText(shipper.getName());
            tfInvoiceId.setText(mInvoice.getId());
            datePicker.setValue(mInvoice.getDate());
            tfCustomer.setText(mInvoice.getCustomer());
            tfAddress.setText(mInvoice.getAddress());
            cbPaymentTypes.setValue(mInvoice.getPaymentType());
            refresh();
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error while retrieving shipper info.", err);
        }));
    }
    
    public void refresh() {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getDeliveryInvoiceItems(mInvoice.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(deliveryItems -> {
            showProgress(false);
            items.setAll(deliveryItems);
            double total = 0;
            for (DeliveryInvoiceItem item : deliveryItems) {
                total += item.getTotal();
            }
            mTotal.set(total);
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error while retrieving delivery items.", err);
        }));
    }

    private void saveAndClose() {
        showProgress(true);
        mInvoice.setDate(datePicker.getValue());
        mInvoice.setCustomer(tfCustomer.getText());
        mInvoice.setAddress(tfAddress.getText());
        mInvoice.setPaymentType(cbPaymentTypes.getValue());
        mInvoice.setTotal(mTotal.get());
        
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().executeQuery(mInvoice.updateSQL());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            showProgress(false);
            if (!success) showInfoDialog("Failed to add Invoice", "");
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
        tfShipper.clear();
        cbPaymentTypes.setValue(null);
        tfCustomer.clear();
        tfAddress.clear();
        items.clear();
        mTotal.set(0);
        showProgress(false);
        mInvoice = null;
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }
    
}
