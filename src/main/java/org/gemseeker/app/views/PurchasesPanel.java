package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Optional;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.PurchaseInvoice;
import org.gemseeker.app.data.PurchaseInvoiceItem;
import org.gemseeker.app.data.Supplier;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.frameworks.SplitController;
import org.gemseeker.app.views.icons.PrintIcon;
import org.gemseeker.app.views.icons.RefreshIcon;
import org.gemseeker.app.views.prints.PrintPurchaseInvoice;
import org.gemseeker.app.views.prints.PrintPurchaseInvoiceList;
import org.gemseeker.app.views.prints.PrintWindow;
import org.gemseeker.app.views.tablecells.DateTableCell;
import org.gemseeker.app.views.tablecells.PriceTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSkuTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author Gem
 */
public class PurchasesPanel extends AbstractPanelController {
    
    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnRefresh;
    @FXML private Button btnPrint;
    
    @FXML private Label lblMonthYear;
    @FXML private Label lblMonthlyTotal;
    @FXML private ToggleButton toggleShowAll;
    @FXML private ToggleButton toggleFilterDate;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private ComboBox<Supplier> cbSuppliers;
    
    // Purchase Invoices Table
    @FXML private TableView<PurchaseInvoice> purchaseTable;
    @FXML private TableColumn<PurchaseInvoice, LocalDate> colPurchaseDate;
    @FXML private TableColumn<PurchaseInvoice, String> colPurchaseNo;
    @FXML private TableColumn<PurchaseInvoice, String> colPurchaseSupplier;
    @FXML private TableColumn<PurchaseInvoice, Double> colPurchaseTotal;
    
    // Purchase Invoice Items Table
    @FXML private TableView<PurchaseInvoiceItem> itemsTable;
    @FXML private TableColumn<PurchaseInvoiceItem, Product> colName;
    @FXML private TableColumn<PurchaseInvoiceItem, Product> colSku;
    @FXML private TableColumn<PurchaseInvoiceItem, Product> colUnit;
    @FXML private TableColumn<PurchaseInvoiceItem, Product> colUnitPrice;
    @FXML private TableColumn<PurchaseInvoiceItem, Integer> colQuantity;
    @FXML private TableColumn<PurchaseInvoiceItem, Double> colTotal;
    @FXML private Label lblTotal;
    
    @FXML private SplitPane splitPane;
    private SplitController splitController;
    
    private final MainWindow mainWindow;
    private final CompositeDisposable disposables;
    
    private FilteredList<PurchaseInvoice> filteredList;
    private final ObservableList<PurchaseInvoiceItem> productItems = FXCollections.observableArrayList();
    private final SimpleIntegerProperty selectedInvoiceIndex = new SimpleIntegerProperty(-1);
    private final SimpleIntegerProperty selectedInvoiceItemIndex = new SimpleIntegerProperty(-1);
    
    private final AddPurchaseWindow addPurchaseWindow;
    private final EditPurchaseWindow editPurchaseWindow;
    private final PrintWindow printWindow;
    
    // Date filter range
    private LocalDate mDateFrom;
    private LocalDate mDateTo;
    
    public PurchasesPanel(MainWindow mainWindow) {
        super(PurchasesPanel.class.getResource("purchases.fxml"));
        this.mainWindow = mainWindow;
        disposables = new CompositeDisposable();
        
        addPurchaseWindow = new AddPurchaseWindow(this, mainWindow.getWindow());
        editPurchaseWindow = new EditPurchaseWindow(this, mainWindow.getWindow());
        printWindow = new PrintWindow(mainWindow.getWindow());
    }

    @Override
    public void onLoad() {
        // setup icons
        btnRefresh.setGraphic(new RefreshIcon(14));
        btnPrint.setGraphic(new PrintIcon(14));
        
        // setup table
        colPurchaseDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colPurchaseDate.setCellFactory(col -> new DateTableCell<>());
        colPurchaseNo.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPurchaseSupplier.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        colPurchaseTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colPurchaseTotal.setCellFactory(col -> new PriceTableCell<>());
        
        colName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colName.setCellFactory(col -> new ProductNameTableCell<>());
        colSku.setCellValueFactory(new PropertyValueFactory<>("product"));
        colSku.setCellFactory(col -> new ProductSkuTableCell<>());
        colUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colUnitPrice.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnitPrice.setCellFactory(col -> new ProductUnitPriceTableCell<>());
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> new PriceTableCell<>());
        
        itemsTable.setItems(productItems);
        selectedInvoiceIndex.bind(purchaseTable.getSelectionModel().selectedIndexProperty());
        selectedInvoiceItemIndex.bind(itemsTable.getSelectionModel().selectedIndexProperty());
        
        // Purchase Invoice Context Menu
        CheckMenuItem mShowDetails = new CheckMenuItem("Show Details");
        mShowDetails.setSelected(true);
        MenuItem mPrint = new MenuItem("Print");
        MenuItem mEditInvoice = new MenuItem("Edit");
        MenuItem mDeleteInvoice = new MenuItem("Delete");
        ContextMenu cm = new ContextMenu();
        cm.getItems().addAll(mShowDetails, mPrint, mEditInvoice, mDeleteInvoice);
        purchaseTable.setContextMenu(cm);
        
        // Disable typing in DatePickers' textfield
        dpFrom.getEditor().addEventFilter(KeyEvent.KEY_TYPED, evt -> evt.consume());
        dpTo.getEditor().addEventFilter(KeyEvent.KEY_TYPED, evt -> evt.consume());
        
        disposables.addAll(
                // Add
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
                    addPurchaseWindow.show();
                }),
                // Edit
                JavaFxObservable.actionEventsOf(btnEdit).subscribe(evt -> {
                    PurchaseInvoice selected = purchaseTable.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        editPurchaseWindow.show(selected);
                    }
                }),
                // Refresh
                JavaFxObservable.actionEventsOf(btnRefresh).subscribe(evt -> {
                    onResume();
                }),
                // Print
                JavaFxObservable.actionEventsOf(btnPrint).subscribe(evt -> {
                    printList();
                }),
                JavaFxObservable.changesOf(toggleShowAll.selectedProperty()).subscribe(selected -> {
                    if (selected.getNewVal()) {
                        lblMonthYear.setText("All");
                        // show all purchase invoices
                    }
                }),
                JavaFxObservable.changesOf(toggleFilterDate.selectedProperty()).subscribe(selected -> {
                    boolean isSelected = selected.getNewVal();
                    dpFrom.setDisable(!isSelected);
                    dpTo.setDisable(!isSelected);
                    if (isSelected) {
                        updateMonthLabel();
                        filterByDate();
                    }
                }),
                // Purchase Invoice -> Print
                JavaFxObservable.actionEventsOf(mPrint).subscribe(evt -> {
                    PurchaseInvoice inv = purchaseTable.getSelectionModel().getSelectedItem();
                    if (inv != null) printPurchaseProducts(inv);
                }),
                // Purchase Invoice -> Edit
                JavaFxObservable.actionEventsOf(mEditInvoice).subscribe(evt -> {
                    PurchaseInvoice selected = purchaseTable.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        editPurchaseWindow.show(selected);
                    }
                }),
                // Purchase Invoice -> Show Details
                JavaFxObservable.actionEventsOf(mShowDetails).subscribe(evt -> {
                    if (mShowDetails.isSelected() && !splitController.isTargetVisible()) {
                        splitController.showTarget();
                    } else {
                        splitController.hideTarget();
                    }
                }),
                // Purchase Invoice -> Delete
                JavaFxObservable.actionEventsOf(mDeleteInvoice).subscribe(evt -> {
                    PurchaseInvoice inv = purchaseTable.getSelectionModel().getSelectedItem();
                    if (inv != null) {
                        Optional<ButtonType> result = showConfirmDialog("Delete Purchase Invoice?", "Deleting this entry will also "
                                + "delete any related data. Continue?");
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            deleteInvoice(inv);
                        }
                    }
                }),
                JavaFxObservable.changesOf(purchaseTable.getSelectionModel().selectedItemProperty()).subscribe(inv -> {
                    if (inv.getNewVal() != null) {
                        getPurchaseInvoiceItems(inv.getNewVal());
                    }
                }),
                JavaFxObservable.changesOf(dpFrom.valueProperty()).subscribe(value -> {
                    mDateFrom = value.getNewVal();
                    if (mDateFrom.isAfter(mDateTo)) {
                        YearMonth ym = YearMonth.from(mDateFrom);
                        mDateTo = ym.atEndOfMonth();
                        dpTo.setValue(mDateTo);
                    }
                    updateMonthLabel();
                    filterByDate();
                }),
                JavaFxObservable.changesOf(dpTo.valueProperty()).subscribe(value -> {
                    mDateTo = value.getNewVal();
                    updateMonthLabel();
                    filterByDate();
                }),
                JavaFxObservable.changesOf(cbSuppliers.valueProperty()).subscribe(value -> {
                    Supplier supplier = value.getNewVal();
                    if (supplier != null && filteredList != null) {
                        productItems.clear();
                        if (supplier.getName().equals("All")) {
                            filteredList.setPredicate(p -> true);
                        } else {
                            filteredList.setPredicate(p -> p.getSupplier().equals(supplier.getName()));
                        }
                    }
                }),
                JavaFxObservable.changesOf(selectedInvoiceIndex).subscribe(index -> {
                    btnEdit.setDisable(index.getNewVal().intValue() == -1);
                })
        );
        
        splitController = new SplitController(splitPane, SplitController.Target.LAST);
//        splitController.hideTarget();
    }

    @Override
    public void onPause() {
        purchaseTable.getSelectionModel().clearSelection();
        productItems.clear();
        lblTotal.setText("P 0.00");
    }

    @Override
    public void onResume() {
        LocalDate now = LocalDate.now();
        YearMonth ym = YearMonth.from(now);
        mDateFrom = ym.atDay(1);
        mDateTo = ym.atEndOfMonth();
        dpFrom.setValue(mDateFrom);
        dpTo.setValue(mDateTo);
        updateMonthLabel();
        
        mainWindow.showProgress(true, "Fetching suppliers...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getSuppliers();
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(suppliers -> {
            mainWindow.showProgress(false);
            Supplier all = new Supplier();
            all.setName("All");
            suppliers.add(0, all);
            cbSuppliers.setItems(FXCollections.observableArrayList(suppliers));
            filterByDate();
            cbSuppliers.setValue(all);
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error while retrieving suppliers list.", err);
        }));
    }
    
    private void filterByDate() {
        if (mDateFrom != null && mDateTo != null) {
            mainWindow.showProgress(true, "Fetching purchase invoices...");
            disposables.add(Single.fromCallable(() -> {
                return EmbeddedDatabase.getInstance().getPurchaseInvoices(mDateFrom, mDateTo);
            }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(invoices -> {
                mainWindow.showProgress(false);
                filteredList = new FilteredList<>(FXCollections.observableArrayList(invoices), s -> true);
                purchaseTable.setItems(filteredList);
                productItems.clear();
                
                // reset table selections
                purchaseTable.getSelectionModel().clearSelection();
                itemsTable.getSelectionModel().clearSelection();

                // recalculate total
                double monthlyTotal = 0;
                for (PurchaseInvoice invoice : invoices) {
                    monthlyTotal += invoice.getTotal();
                }
                lblMonthlyTotal.setText("P " + Utils.toMoneyFormat(monthlyTotal));
            }, err -> {
                mainWindow.showProgress(false);
                showErrorDialog("Database Error", "Error occurred while fetching inventory data.", err);
            }));
        }
    }
    
    private void updateMonthLabel() {
        if (mDateFrom != null && mDateTo != null && (mDateFrom.isBefore(mDateTo))) {
            String rangeText = String.format("%s - %s", mDateFrom.format(Utils.dateTimeFormat2), 
                    mDateTo.format(Utils.dateTimeFormat2));
            lblMonthYear.setText(rangeText);
        }
    }
    
    private void getPurchaseInvoiceItems(PurchaseInvoice invoice) {
        mainWindow.showProgress(true, "Fetching purchase invoice items...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getPurchaseInvoiceItems(invoice.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(items -> {
            mainWindow.showProgress(false);
            productItems.setAll(items);
            lblTotal.setText("P " + Utils.toMoneyFormat(invoice.getTotal()));
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching purchased products", err);
        }));
    }
    
    private void deleteInvoice(PurchaseInvoice invoice) {
        mainWindow.showProgress(true, "Deleting invoice...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().deleteEntry("purchase_invoices", "id", invoice.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            mainWindow.showProgress(false);
            onResume();
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while deleting purchase invoice entry.", err);
        }));
    }

    private void printPurchaseProducts(PurchaseInvoice invoice) {
        mainWindow.showProgress(true, "Preparing products for printing...");
        
        ArrayList<PrintPurchaseInvoice> ppis = new ArrayList<>();
        int maxPerPage = 42;
        int totalPage = (int) (productItems.size() / maxPerPage);
        if (totalPage > 0) {
            int startIndex = 0;
            for (int i = 1; i <= totalPage; i++) {
                ArrayList<PurchaseInvoiceItem> items = new ArrayList<>();
                int endIndex = startIndex + maxPerPage - 1;
                if (endIndex < productItems.size()) {
                    items.addAll(productItems.subList(startIndex, endIndex));
                } else {
                    items.addAll(productItems.subList(startIndex, productItems.size() - 1));
                }
                PrintPurchaseInvoice ppi = new PrintPurchaseInvoice();
                ppi.set(invoice, items, i, totalPage);
                ppis.add(ppi);
                startIndex = endIndex;
            }
        } else {
            PrintPurchaseInvoice ppi = new PrintPurchaseInvoice();
            ppi.set(invoice, new ArrayList<>(productItems), 1, 1);
            ppis.add(ppi);
        }
        
        mainWindow.showProgress(false);
        printWindow.show(ppis);
    }
    
    private void printList() {
        mainWindow.showProgress(true, "Preparing purchase invoices for printing...");
        
        ArrayList<PrintPurchaseInvoiceList> ppis = new ArrayList<>();
        int maxPerPage = 47;
        int totalPage = (int) (filteredList.size() / maxPerPage);
        if (totalPage > 0) {
            int startIndex = 0;
            for (int i = 1; i <= totalPage; i++) {
                ArrayList<PurchaseInvoice> items = new ArrayList<>();
                int endIndex = startIndex + maxPerPage - 1;
                if (endIndex < filteredList.size()) {
                    items.addAll(filteredList.subList(startIndex, endIndex));
                } else {
                    items.addAll(filteredList.subList(startIndex, filteredList.size() - 1));
                }
                PrintPurchaseInvoiceList ppi = new PrintPurchaseInvoiceList();
                ppi.set(items, i, totalPage);
                ppis.add(ppi);
                startIndex = endIndex;
            }
        } else {
            PrintPurchaseInvoiceList ppi = new PrintPurchaseInvoiceList();
            ppi.set(new ArrayList<>(filteredList), 1, 1);
            ppis.add(ppi);
        }
        
        mainWindow.showProgress(false);
        printWindow.show(ppis);
    }
    
    @Override
    public void onDispose() {
        disposables.dispose();
        addPurchaseWindow.onDispose();
        editPurchaseWindow.onDispose();
        printWindow.onDispose();
    }

}
