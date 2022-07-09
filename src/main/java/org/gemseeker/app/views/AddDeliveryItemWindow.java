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
import org.gemseeker.app.data.DeliveryInvoiceItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.Shipper;
import org.gemseeker.app.data.ShipperStock;
import org.gemseeker.app.views.frameworks.AbstractWindowController;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductRetailPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSupplierTableCell;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class AddDeliveryItemWindow extends AbstractWindowController {

    @FXML private TableView<ShipperStock> productsTable;
    @FXML private TableColumn<ShipperStock, Product> colName;
    @FXML private TableColumn<ShipperStock, Product> colSupplier;
    @FXML private TableColumn<ShipperStock, Integer> colStock;
    @FXML private TableColumn<ShipperStock, Product> colRetailPrice;
    
    private FilteredList<ShipperStock> filteredList;
    
    @FXML private TextField tfDiscount;
    @FXML private TextField tfDiscountedPrice;
    @FXML private TextField tfQuantity;
    @FXML private TextField tfTotal;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private ProgressBar progressBar;
    
    private final AddDeliveryWindow addInvoiceWindow;
    private final CompositeDisposable disposables;
    
    private final SimpleObjectProperty<ShipperStock> selected = new SimpleObjectProperty<>();
    
    public AddDeliveryItemWindow(AddDeliveryWindow addInvoiceWindow) {
        super("Add Invoice Item", AddDeliveryItemWindow.class.getResource("add_invoice_item.fxml"),
                addInvoiceWindow.getWindow());
        this.addInvoiceWindow = addInvoiceWindow;
        disposables = new CompositeDisposable();
    }

    @Override
    protected void initWindow(Stage stage) {
        super.initWindow(stage);
        stage.initModality(Modality.APPLICATION_MODAL);
    }
    
    @Override
    public void onLoad() {
        Utils.setAsIntegerTextField(tfDiscount);
        Utils.setAsIntegerTextField(tfQuantity);
        
        colName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colName.setCellFactory(col -> new ProductNameTableCell<>());
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("product"));
        colSupplier.setCellFactory(col -> new ProductSupplierTableCell<>());
        colStock.setCellValueFactory(new PropertyValueFactory<>("inStock"));
        colRetailPrice.setCellValueFactory(new PropertyValueFactory<>("product"));
        colRetailPrice.setCellFactory(col -> new ProductRetailPriceTableCell<>());
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    if (selected.get() != null && !tfQuantity.getText().isEmpty()) {
                        saveAndClose();
                    } else {
                        showInfoDialog("Invalid Input", "Please fill-in required fields.");
                    }
                }),
                JavaFxObservable.actionEventsOf(btnCancel).subscribe(evt -> {
                    close();
                }),
                JavaFxObservable.changesOf(productsTable.getSelectionModel().selectedItemProperty()).subscribe(stock -> {
                    selected.set(stock.getNewVal());
                    ShipperStock item = selected.get();
                    if (item != null) recalculate();
                }),
                JavaFxObservable.changesOf(tfDiscount.textProperty()).subscribe(text -> {
                    recalculate();
                }),
                JavaFxObservable.changesOf(tfQuantity.textProperty()).subscribe(text -> {
                    recalculate();
                })
        );
    }

    public void show(Shipper shipper) {
        super.show();
        if (shipper != null) {
            showProgress(true);
            disposables.add(Single.fromCallable(() -> {
                return EmbeddedDatabase.getInstance().getShipperStocks(shipper.getId());
            }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(items -> {
                showProgress(false);
                filteredList = new FilteredList<>(FXCollections.observableArrayList(items), p -> true);
                productsTable.setItems(filteredList);
            }, err -> {
                showProgress(false);
                showErrorDialog("Database Error", "Error while fetching shipper stocks items.", err);
            }));
        }
    }
    
    private void recalculate() {
        ShipperStock item = selected.get();
        if (item != null) {
            double discount = 0;
            if (!tfDiscount.getText().isEmpty()) {
                discount = Double.parseDouble(tfDiscount.getText().trim()) / 100;
            }
            
            double retail = item.getProduct().getRetailPrice();
            double less = retail * discount;
            double discountedPrice = retail - less;
            tfDiscountedPrice.setText(String.format("%.2f", discountedPrice));
            
            int quantity = 0;
            if (!tfQuantity.getText().isEmpty()) {
                quantity = Integer.parseInt(tfQuantity.getText().trim());
            }
            
            double total = discountedPrice * quantity;
            tfTotal.setText(String.format("%.2f", total));
        }
    }
    
    private void saveAndClose() {
        DeliveryInvoiceItem item = new DeliveryInvoiceItem();
        ShipperStock stock = selected.get();
        item.setProductId(stock.getProductId());
        int qty = Integer.parseInt(tfQuantity.getText().trim());
        item.setQuantity(qty);
        double discount = 0;
        if (!tfDiscount.getText().isEmpty()) {
            discount = Double.parseDouble(tfDiscount.getText().trim()) / 100;
        }
        item.setDiscount(discount);
        double retail = stock.getProduct().getRetailPrice();
        double less = retail * discount;
        double discountedPrice = retail - less;
        item.setDiscountedPrice(discountedPrice);
        item.setTotal(qty * discountedPrice);
        item.setProduct(stock.getProduct());
        addInvoiceWindow.addInvoiceItem(item);
        close();
    }

    private void showProgress(boolean show) {
        progressBar.setVisible(show);
    }
    
    @Override
    public void onClose() {
        selected.set(null);
        tfDiscount.setText("0");
        tfDiscountedPrice.setText("0.00");
        tfQuantity.setText("1");
        tfTotal.setText("0.00");
        showProgress(false);
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }
    
}
