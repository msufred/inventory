package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import java.time.LocalDate;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.PurchaseInvoice;
import org.gemseeker.app.data.PurchaseInvoiceItem;
import org.gemseeker.app.data.Stock;
import org.gemseeker.app.data.Supplier;
import org.gemseeker.app.views.frameworks.AbstractWindowController;
import org.gemseeker.app.views.tablecells.PriceTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductRetailPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSkuTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author Gem
 */
public class AddPurchaseWindow extends AbstractWindowController {
    
    @FXML private DatePicker datePicker;
    @FXML private TextField tfNo;
    @FXML private ComboBox<Supplier> cbSuppliers;
    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnRemove;
    @FXML private Label lblTotal;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private ProgressBar progressBar;
    @FXML private TableView<PurchaseInvoiceItem> itemsTable;
    @FXML private TableColumn<PurchaseInvoiceItem, Product> colName;
    @FXML private TableColumn<PurchaseInvoiceItem, Product> colSku;
    @FXML private TableColumn<PurchaseInvoiceItem, Product> colUnit;
    @FXML private TableColumn<PurchaseInvoiceItem, Product> colPrice;
    @FXML private TableColumn<PurchaseInvoiceItem, Product> colRetailPrice;
    @FXML private TableColumn<PurchaseInvoiceItem, Integer> colQuantity;
    @FXML private TableColumn<PurchaseInvoiceItem, Double> colTotal;
    
    private final PurchasesPanel inventoryPanel;
    private final CompositeDisposable disposables;
    
    private final ObservableList<PurchaseInvoiceItem> mItems = FXCollections.observableArrayList();
    private final SimpleIntegerProperty selectedIndex = new SimpleIntegerProperty(-1);
    private final SimpleDoubleProperty mTotal = new SimpleDoubleProperty(0);
    
    // TODO rename to AddPurchaseItemWindow
    private final AddProductWindow addProductWindow;
    private final EditPurchaseItemWindow editPurchaseItemWindow;
    
    public AddPurchaseWindow(PurchasesPanel inventoryPanel, Stage mainStage) {
        super("Add Purchase", AddPurchaseWindow.class.getResource("add_purchase.fxml"), mainStage);
        this.inventoryPanel = inventoryPanel;
        disposables = new CompositeDisposable();
        addProductWindow = new AddProductWindow(this);
        editPurchaseItemWindow = new EditPurchaseItemWindow(this);
    }

    @Override
    protected void initWindow(Stage stage) {
        super.initWindow(stage);
        stage.initModality(Modality.APPLICATION_MODAL);
    }

    @Override
    public void onLoad() {
        Utils.setSafeTextField(tfNo);
        Utils.setSafeTextField(cbSuppliers.getEditor());
        
        colName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colName.setCellFactory(col -> new ProductNameTableCell<>());
        colSku.setCellValueFactory(new PropertyValueFactory<>("product"));
        colSku.setCellFactory(col -> new ProductSkuTableCell<>());
        colUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colPrice.setCellValueFactory(new PropertyValueFactory<>("product"));
        colPrice.setCellFactory(col -> new ProductUnitPriceTableCell<>());
        colRetailPrice.setCellValueFactory(new PropertyValueFactory<>("product"));
        colRetailPrice.setCellFactory(col -> new ProductRetailPriceTableCell<>());
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> new PriceTableCell<>());
        
        itemsTable.setItems(mItems);
        // bind table's selected index to selectedIndex
        selectedIndex.bind(itemsTable.getSelectionModel().selectedIndexProperty());
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
                    addProductWindow.show();
                }),
                JavaFxObservable.actionEventsOf(btnEdit).subscribe(evt -> {
                    if (selectedIndex.get() > -1) {
                        PurchaseInvoiceItem item = mItems.get(selectedIndex.get());
                        editPurchaseItemWindow.show(item);
                    }
                }),
                JavaFxObservable.actionEventsOf(btnRemove).subscribe(evt -> {
                    int index = itemsTable.getSelectionModel().getSelectedIndex();
                    if (index > -1) mItems.remove(index);
                }),
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    if (tfNo.getText().isEmpty() || datePicker.getValue() == null ||
                            cbSuppliers.getEditor().getText().isEmpty() || mItems.isEmpty()) {
                        showInfoDialog("Invalid Input", "Please fill-in required fields.");
                    } else {
                        checkIdAndSave();
                    }
                }),
                JavaFxObservable.actionEventsOf(btnCancel).subscribe(evt -> {
                    close();
                }),
                JavaFxObservable.changesOf(mTotal).subscribe(value -> {
                    if (value.getNewVal() != null) {
                        lblTotal.setText("P " + Utils.toMoneyFormat(value.getNewVal().doubleValue()));
                    }
                }),
                JavaFxObservable.changesOf(selectedIndex).subscribe(index -> {
                    System.out.println("Selected:" + index.getNewVal().intValue());
                    btnEdit.setDisable(index.getNewVal().intValue() == -1);
                    btnRemove.setDisable(index.getNewVal().intValue() == -1);
                })
        );
    }
    
    public void addProduct(PurchaseInvoiceItem item) {
        mItems.add(item);
        // recalculate
        double total = 0;
        for (PurchaseInvoiceItem p : mItems) {
            total += p.getTotal();
        }
        mTotal.set(total);
    }

    public void replaceProduct(int index, PurchaseInvoiceItem item) {
        if (index > -1) mItems.remove(index);
        mItems.add(index, item);
    }
    
    public void replaceProduct(PurchaseInvoiceItem item) {
        if (selectedIndex.get() > -1) replaceProduct(selectedIndex.get(), item);
    }
    
    @Override
    public void show() {
        super.show();
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getSuppliers();
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(suppliers -> {
            cbSuppliers.setItems(FXCollections.observableArrayList(suppliers));
            datePicker.setValue(LocalDate.now());
        }));
    }
    
    private void checkIdAndSave() {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().purchaseInvoiceExists(tfNo.getText().trim());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(exists -> {
            showProgress(false);
            if (exists) showInfoDialog("Invoice Exists", "Purchase Invoice ID already in used. Try again.");
            else saveAndClose();
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error while doing database query.", err);
        }));
    }
    
    private void saveAndClose() {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            EmbeddedDatabase database = EmbeddedDatabase.getInstance();
            
            // save Supplier if necessary
            String supplierName = cbSuppliers.getEditor().getText();
            Supplier supplier = database.getSupplier(supplierName);
            if (supplier == null) {
                supplier = new Supplier();
                supplier.setName(supplierName);
                supplier.setId(database.addEntryReturnId(supplier));
            }
            
            // save PurchaseInvoice
            PurchaseInvoice invoice = new PurchaseInvoice();
            invoice.setId(tfNo.getText());
            invoice.setDate(datePicker.getValue());
            invoice.setSupplier(supplier.getName());
            invoice.setTotal(mTotal.get());
            
            boolean success = database.addEntry(invoice);
            
            // save purchase items
            if (success) {
                for (PurchaseInvoiceItem p: mItems) {
                    p.setInvoiceId(invoice.getId());
                    p.setDate(invoice.getDate());
                    
                    // if Product is not in the database yet, add
                    if (p.getProductId() == -1) {
                        // save product return id
                        Product product = p.getProduct();
                        product.setSupplier(supplier.getName());
                        int productId = database.addEntryReturnId(product);
                        
                        // save 
                        if (productId != -1) {
                            p.setProductId(productId);
                            // save stock entry
                            Stock stock = new Stock();
                            stock.setProductId(productId);
                            stock.setQuantity(p.getQuantity());
                            stock.setQuantityOut(0);
                            stock.setInStock(p.getQuantity());
                            database.addEntry(stock);
                        }
                    } 
                    
                    // if Product is already saved in the database, update
                    else {
                        boolean updated = database.executeQuery(p.getProduct().updateSQL());
                        if (updated) {
                            // update product stock
                            Stock stock = p.getProduct().getStock();
                            if (stock != null) {
                                int newQty = stock.getQuantity() + p.getQuantity();
                                stock.setQuantity(newQty);
                                int inStock = stock.getInStock() + p.getQuantity();
                                stock.setInStock(inStock);
                                database.executeQuery(stock.updateSQL());
                            }
                        }
                    }
                    
                    // save purchase invoice item
                    database.addEntryReturnId(p);
                }
            }
            return success;
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
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
        cbSuppliers.setValue(null);
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
