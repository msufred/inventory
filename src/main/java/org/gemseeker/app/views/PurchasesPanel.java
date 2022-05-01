package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.PurchaseInvoice;
import org.gemseeker.app.data.PurchaseInvoiceItem;
import org.gemseeker.app.data.Stock;
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
    @FXML private Button btnRefresh;
    @FXML private Button btnPrint;
    
    @FXML private Label lblMonthYear;
    @FXML private Label lblMonthlyTotal;
    @FXML private ComboBox<String> cbMonths;
    @FXML private ComboBox<Integer> cbYears;
    
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
    
    // NOT RELATED TO PURCHASE INVOICE ITEMS
    // This columns represents the current stock and ordered quantity of the product
    @FXML private TableColumn<PurchaseInvoiceItem, Product> colOrdered;
    @FXML private TableColumn<PurchaseInvoiceItem, Product> colInStock;
    
    @FXML private SplitPane splitPane;
    private SplitController splitController;
    
    private final MainWindow mainWindow;
    private final CompositeDisposable disposables;
    
    private FilteredList<PurchaseInvoice> filteredList;
    private final ObservableList<PurchaseInvoiceItem> productItems = FXCollections.observableArrayList();
    
    private final AddPurchaseWindow addPurchaseWindow;
    private final PrintWindow printWindow;
    
//    private final AddProductWindow addProductWindow;
    private final EditProductWindow editProductWindow;
    private final ShowOrdersWindow showOrdersWindow;
    
    public PurchasesPanel(MainWindow mainWindow) {
        super(PurchasesPanel.class.getResource("purchases.fxml"));
        this.mainWindow = mainWindow;
        disposables = new CompositeDisposable();
        
        addPurchaseWindow = new AddPurchaseWindow(this, mainWindow.getWindow());
        printWindow = new PrintWindow(mainWindow.getWindow());
        
//        addProductWindow = new AddProductWindow(this, mainWindow.getWindow());
        editProductWindow = new EditProductWindow(this, mainWindow.getWindow());
        showOrdersWindow = new ShowOrdersWindow(mainWindow.getWindow());
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
        colOrdered.setCellValueFactory(new PropertyValueFactory<>("product"));
        colOrdered.setCellFactory(col -> new TableCell<PurchaseInvoiceItem, Product>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    Stock s = item.getStock();
                    setText(s.getQuantityOut() + "");
                } else setText("");
            }
        });
        colInStock.setCellValueFactory(new PropertyValueFactory<>("product"));
        colInStock.setCellFactory(col -> new TableCell<PurchaseInvoiceItem, Product>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    Stock s = item.getStock();
                    setText(s.getInStock()+ "");
                } else setText("");
            }
        });
        
        itemsTable.setItems(productItems);
        
        cbMonths.setItems(FXCollections.observableArrayList(
                "January", "February", "March", "April", "May", "June", "July",
                "August", "September", "October", "November", "December"
        ));
        
        cbYears.setItems(FXCollections.observableArrayList(
                2020, 2021, 2022, 2023, 2024, 2025, 2026, 2027, 2028
        ));
        
        // Set current month and year
        LocalDate now = LocalDate.now();
        cbMonths.setValue(Utils.monthStringValue(now.getMonthValue()));
        cbYears.setValue(now.getYear());
        updateMonthLabel();
        
        // Purchase Invoice Context Menu
        CheckMenuItem mShowDetails = new CheckMenuItem("Show Details");
        mShowDetails.setSelected(true);
        MenuItem mPrint = new MenuItem("Print");
        MenuItem mDeleteInvoice = new MenuItem("Delete");
        ContextMenu cm = new ContextMenu();
        cm.getItems().addAll(mShowDetails, mPrint, mDeleteInvoice);
        purchaseTable.setContextMenu(cm);
        
        // Purchase Invoice Item Context Menus
        MenuItem mOrders = new MenuItem("Show Orders");
        MenuItem mEdit = new MenuItem("Edit Details");
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(mOrders, mEdit);
        itemsTable.setContextMenu(contextMenu);
        
        disposables.addAll(
                // Add
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
                    addPurchaseWindow.show();
                }),
                // Refresh
                JavaFxObservable.actionEventsOf(btnRefresh).subscribe(evt -> {
                    onResume();
                }),
                // Print
                JavaFxObservable.actionEventsOf(btnPrint).subscribe(evt -> {
                    printList();
                }),
                // Purchase Invoice -> Print
                JavaFxObservable.actionEventsOf(mPrint).subscribe(evt -> {
                    PurchaseInvoice inv = purchaseTable.getSelectionModel().getSelectedItem();
                    if (inv != null) printPurchaseProducts(inv);
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
                // Purchase Invoice Item -> Show Orders
                JavaFxObservable.actionEventsOf(mOrders).subscribe(evt -> {
                    PurchaseInvoiceItem p = itemsTable.getSelectionModel().getSelectedItem();
                    if (p != null) {
                        showOrdersWindow.show(p.getProduct());
                    }
                }),
                // Purchase Invoice Item -> Edit Details
                JavaFxObservable.actionEventsOf(mEdit).subscribe(evt -> {
                    PurchaseInvoiceItem p = itemsTable.getSelectionModel().getSelectedItem();
                    if (p != null) editProductWindow.show(p.getProduct());
                }),
                JavaFxObservable.changesOf(purchaseTable.getSelectionModel().selectedItemProperty()).subscribe(inv -> {
                    if (inv.getNewVal() != null) {
                        getPurchaseInvoiceItems(inv.getNewVal());
                    }
                }),
                JavaFxObservable.changesOf(cbMonths.valueProperty()).subscribe(month -> {
                    updateMonthLabel();
                    filterByDate();
                }),
                JavaFxObservable.changesOf(cbYears.valueProperty()).subscribe(month -> {
                    updateMonthLabel();
                    filterByDate();
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
        filterByDate();
    }
    
    private void filterByDate() {
        if (cbMonths.getValue() != null && cbYears.getValue() != -1) {
            int month = Utils.monthIntegerValue(cbMonths.getValue());
            int year = cbYears.getValue();
            LocalDate date = LocalDate.of(year, month, 1);
            getPurchaseInvoices(date);
        }
    }
    
    private void getPurchaseInvoices(LocalDate date) {
        mainWindow.showProgress(true, "Fetching purchase invoices...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getPurchaseInvoices(date);
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(invoices -> {
            mainWindow.showProgress(false);
            filteredList = new FilteredList<>(FXCollections.observableArrayList(invoices), s -> true);
            purchaseTable.setItems(filteredList);
            productItems.clear();
            
            double monthlyTotal = 0;
            for (PurchaseInvoice invoice : invoices) {
                monthlyTotal += invoice.getTotal();
            }
            lblMonthlyTotal.setText("P " + Utils.getMoneyFormat(monthlyTotal));
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching inventory data.", err);
        }));
    }
    
    private void updateMonthLabel() {
        if (cbMonths.getValue() != null && cbYears.getValue() != -1) {
            int month = Utils.monthIntegerValue(cbMonths.getValue());
            int year = cbYears.getValue();
            lblMonthYear.setText(Utils.monthStringValue(month) + " " + year);
        }
    }
    
    private void getPurchaseInvoiceItems(PurchaseInvoice invoice) {
        mainWindow.showProgress(true, "Fetching purchase invoice items...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getPurchaseInvoiceItems(invoice.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(items -> {
            mainWindow.showProgress(false);
            productItems.setAll(items);
            lblTotal.setText("P " + Utils.getMoneyFormat(invoice.getTotal()));
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
//        addProductWindow.onDispose();
        editProductWindow.onDispose();
        showOrdersWindow.onDispose();
        printWindow.onDispose();
    }

}
