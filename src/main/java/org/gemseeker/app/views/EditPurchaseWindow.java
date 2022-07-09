package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Optional;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
public class EditPurchaseWindow extends AbstractWindowController {
    
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
    private final SimpleDoubleProperty mTotal = new SimpleDoubleProperty(0);
    
    private final AddSaveProductWindow addProductWindow;
    private final EditSavePurchaseItemWindow editItemWindow;
    
    private PurchaseInvoice mInvoice;
    
    public EditPurchaseWindow(PurchasesPanel inventoryPanel, Stage mainStage) {
        super("Edit Purchase", AddPurchaseWindow.class.getResource("edit_purchase.fxml"), mainStage);
        this.inventoryPanel = inventoryPanel;
        disposables = new CompositeDisposable();
        addProductWindow = new AddSaveProductWindow(this);
        editItemWindow = new EditSavePurchaseItemWindow(this);
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
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
                    if (mInvoice != null) addProductWindow.show(mInvoice);
                }),
                JavaFxObservable.actionEventsOf(btnEdit).subscribe(evt -> {
                    PurchaseInvoiceItem selected = itemsTable.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        editItemWindow.show(selected);
                    }
                }),
                JavaFxObservable.actionEventsOf(btnRemove).subscribe(evt -> {
                    PurchaseInvoiceItem item = itemsTable.getSelectionModel().getSelectedItem();
                    if (item != null) {
                        Optional<ButtonType> result = showConfirmDialog("Remove Product?", "This will update/remove existing product. Proceed?",
                                ButtonType.YES, ButtonType.NO);
                        if (result.isPresent() && result.get() == ButtonType.YES) {
                            
                        }
                    }
                }),
                JavaFxObservable.actionEventsOf(btnSave).subscribe(evt -> {
                    if (tfNo.getText().isEmpty() || datePicker.getValue() == null ||
                            cbSuppliers.getEditor().getText().isEmpty() || mItems.isEmpty()) {
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
                        lblTotal.setText("P " + Utils.toMoneyFormat(value.getNewVal().doubleValue()));
                    }
                }),
                JavaFxObservable.changesOf(itemsTable.getSelectionModel().selectedItemProperty()).subscribe(selected -> {
                    boolean isNull = selected.getNewVal() == null;
                    btnEdit.setDisable(isNull);
                    btnRemove.setDisable(isNull);
                })
        );
    }

    public void show(PurchaseInvoice purchaseInvoice) {
        if (purchaseInvoice == null) {
            showInfoDialog("Cannot Edit Purchase Invoice!", "No purchase invoice selected.");
            return;
        }
        mInvoice = purchaseInvoice;
        show();
        
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getSuppliers();
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(suppliers -> {
            showProgress(false);
            cbSuppliers.setItems(FXCollections.observableArrayList(suppliers));
            tfNo.setText(mInvoice.getId());
            boolean hasSupplier = false;
            for (Supplier s : suppliers) {
                if (s.getName().equals(mInvoice.getSupplier())) {
                    cbSuppliers.setValue(s);
                    hasSupplier = true;
                    break;
                }
            }
            if (!hasSupplier) cbSuppliers.getEditor().setText(mInvoice.getSupplier());
            datePicker.setValue(mInvoice.getDate());
            refresh();
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error while retrieving suppliers data.", err);
        }));
    }
    
    public void refresh() {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getPurchaseInvoiceItems(mInvoice.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(items -> {
            showProgress(false);
            mItems.clear();
            addProducts(items);
        }, err -> {
            showProgress(false);
            showErrorDialog("Database Error", "Error while retrieving purchase invoice items.", err);
        }));
    }
    
    private void addProducts(ArrayList<PurchaseInvoiceItem> items) {
        double total = 0;
        for (PurchaseInvoiceItem p : items) {
            mItems.add(p);
            total += p.getTotal();
        }
        mTotal.set(total);
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
            mInvoice.setSupplier(supplier.getName());
            mInvoice.setDate(datePicker.getValue());
            mInvoice.setTotal(mTotal.get());

            boolean success = database.executeQuery(mInvoice.updateSQL());
            
            // Update all PurchaseInvoiceItems' supplier information. This is to
            // really make sure that all items have the same supplier information.
            // (supplier might have changed right after editing or adding a
            // product)
            if (success) {
                for (PurchaseInvoiceItem item : mItems) {
                    Product p = item.getProduct();
                    database.updateEntry("products", "supplier", supplier.getName(), "id", p.getId());
                }
            }
            
            return success;
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            showProgress(false);
            if (!success) showInfoDialog("Failed to save purchase entry.", "");
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
        mInvoice = null;
    }

    @Override
    public void onDispose() {
        disposables.dispose();
        addProductWindow.onDispose();
    }

}
