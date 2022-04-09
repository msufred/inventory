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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.PurchaseInvoice;
import org.gemseeker.app.data.PurchaseProduct;
import org.gemseeker.app.data.Stock;
import org.gemseeker.app.views.frameworks.AbstractWindowController;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSkuTableCell;
import org.gemseeker.app.views.tablecells.ProductTotalTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author Gem
 */
public class AddPurchaseWindow extends AbstractWindowController {
    
    @FXML private DatePicker datePicker;
    @FXML private TextField tfNo;
    @FXML private TextField tfSupplier;
    @FXML private Button btnAdd;
    @FXML private Label lblTotal;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private ProgressBar progressBar;
    @FXML private TableView<Stock> itemsTable;
    @FXML private TableColumn<Stock, Product> colName;
    @FXML private TableColumn<Stock, Product> colSku;
    @FXML private TableColumn<Stock, Product> colUnit;
    @FXML private TableColumn<Stock, Product> colPrice;
    @FXML private TableColumn<Stock, Integer> colQuantity;
    @FXML private TableColumn<Stock, Product> colTotal;
    
    private final InventoryPanel inventoryPanel;
    private final CompositeDisposable disposables;
    
    private final ObservableList<Stock> mItems = FXCollections.observableArrayList();
    private final SimpleDoubleProperty mTotal = new SimpleDoubleProperty(0);
    
    private final AddProductWindow addProductWindow;
    
    public AddPurchaseWindow(InventoryPanel inventoryPanel, Stage mainStage) {
        super("Add Purchase", AddPurchaseWindow.class.getResource("add_purchase.fxml"), mainStage);
        this.inventoryPanel = inventoryPanel;
        disposables = new CompositeDisposable();
        addProductWindow = new AddProductWindow(this);
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
        colSku.setCellValueFactory(new PropertyValueFactory<>("product"));
        colSku.setCellFactory(col -> new ProductSkuTableCell<>());
        colUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colPrice.setCellValueFactory(new PropertyValueFactory<>("product"));
        colPrice.setCellFactory(col -> new ProductPriceTableCell<>());
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("product"));
        colTotal.setCellFactory(col -> new ProductTotalTableCell<>());
        
        itemsTable.setItems(mItems);
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
                    addProductWindow.show();
                }),
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    if (tfNo.getText().isEmpty() || datePicker.getValue() == null ||
                            tfSupplier.getText().isEmpty() || mItems.isEmpty()) {
                        showInfoDialog("Invalid Input", "Please fill-in required fields.");
                    } else {
                        saveAndClose();
                    }
                }),
                JavaFxObservable.actionEventsOf(btnCancel).subscribe(evt -> {
                    close();
                }),
                JavaFxObservable.changesOf(mTotal).subscribe(value -> {
                    if (value.getNewVal() != null) {
                        lblTotal.setText("P " + Utils.getMoneyFormat(value.getNewVal().doubleValue()));
                    }
                })
        );
    }
    
    public void addProduct(Stock stock) {
        mItems.add(stock);
        // recalculate
        double total = 0;
        for (Stock s : mItems) {
            total += s.getProduct().getTotal();
        }
        mTotal.set(total);
    }

    @Override
    public void show() {
        super.show();
        datePicker.setValue(LocalDate.now());
    }
    
    private void saveAndClose() {
        PurchaseInvoice invoice = new PurchaseInvoice();
        invoice.setId(tfNo.getText());
        invoice.setDate(datePicker.getValue());
        invoice.setSupplier(tfSupplier.getText());
        invoice.setTotal(mTotal.get());
        
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().addEntry(invoice);
        }).flatMap(success -> Single.fromCallable(() -> {
            if (success) {
                EmbeddedDatabase db = EmbeddedDatabase.getInstance();
                for (Stock s : mItems) {
                    Product p = s.getProduct();
                    p.setDate(invoice.getDate());
                    p.setSupplier(invoice.getSupplier());
                    int id = db.addEntryReturnId(p);
                    if (id != -1) {
                        PurchaseProduct pp = new PurchaseProduct();
                        pp.setProductId(id);
                        pp.setInvoiceId(invoice.getId());
                        db.addEntry(pp);
                        
                        s.setProductId(id);
                        db.addEntry(s);
                    }
                }
            }
            return success;
        })).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            showProgress(false);
            if (!success) {
                showInfoDialog("Failed to save purchase entry.", "");
            }
            close();
            inventoryPanel.onResume();
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error occurred while saving purchase entry.", err);
        }));
    }
    
    private void showProgress(boolean show) {
        progressBar.setVisible(show);
    }

    @Override
    public void onClose() {
        datePicker.setValue(null);
        tfNo.clear();
        tfSupplier.clear();
        mItems.clear();
        mTotal.set(0);
        showProgress(false);
    }

    @Override
    public void onDispose() {
        disposables.dispose();
        addProductWindow.onDispose();
    }

}
