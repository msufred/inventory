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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.PurchaseInvoice;
import org.gemseeker.app.data.PurchaseProduct;
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
import org.gemseeker.app.views.tablecells.ProductPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSkuTableCell;
import org.gemseeker.app.views.tablecells.ProductTotalTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;
import org.gemseeker.app.views.tablecells.StockOrderedTableCell;
import org.gemseeker.app.views.tablecells.StockQuantityTableCell;
import org.gemseeker.app.views.tablecells.StockRemainingTableCell;

/**
 *
 * @author Gem
 */
public class InventoryPanel extends AbstractPanelController {
    
    @FXML private Button btnAdd;
    @FXML private Button btnRefresh;
    @FXML private Button btnPrint;
    
    @FXML private TableView<PurchaseInvoice> purchaseTable;
    @FXML private TableColumn<PurchaseInvoice, LocalDate> colPurchaseDate;
    @FXML private TableColumn<PurchaseInvoice, String> colPurchaseNo;
    @FXML private TableColumn<PurchaseInvoice, String> colPurchaseSupplier;
    @FXML private TableColumn<PurchaseInvoice, Double> colPurchaseTotal;
    
    @FXML private TableView<PurchaseProduct> stocksTable;
    @FXML private TableColumn<PurchaseProduct, Product> colName;
    @FXML private TableColumn<PurchaseProduct, Product> colUnit;
    @FXML private TableColumn<PurchaseProduct, Product> colUnitPrice;
    @FXML private TableColumn<PurchaseProduct, Product> colSku;
    @FXML private TableColumn<PurchaseProduct, Stock> colQuantity;
    @FXML private TableColumn<PurchaseProduct, Product> colTotal;
    @FXML private TableColumn<PurchaseProduct, Stock> colOrdered;
    @FXML private TableColumn<PurchaseProduct, Stock> colInStock;
    
    @FXML private SplitPane splitPane;
    private SplitController splitController;
    
    private final MainWindow mainWindow;
    private final CompositeDisposable disposables;
    
    private FilteredList<PurchaseInvoice> filteredList;
    private final ObservableList<PurchaseProduct> productItems = FXCollections.observableArrayList();
    
    private final AddPurchaseWindow addPurchaseWindow;
    
    private final AddProductWindow addProductWindow;
    private final EditProductWindow editProductWindow;
    private final ShowOrdersWindow showOrdersWindow;
    private final PrintWindow printWindow;
    
    public InventoryPanel(MainWindow mainWindow) {
        super(InventoryPanel.class.getResource("warehouse.fxml"));
        this.mainWindow = mainWindow;
        disposables = new CompositeDisposable();
        
        addPurchaseWindow = new AddPurchaseWindow(this, mainWindow.getWindow());
        
        addProductWindow = new AddProductWindow(this, mainWindow.getWindow());
        editProductWindow = new EditProductWindow(this, mainWindow.getWindow());
        showOrdersWindow = new ShowOrdersWindow(mainWindow.getWindow());
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
        colUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colUnitPrice.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnitPrice.setCellFactory(col -> new ProductPriceTableCell<>());
        colSku.setCellValueFactory(new PropertyValueFactory<>("product"));
        colSku.setCellFactory(col -> new ProductSkuTableCell<>());
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colQuantity.setCellFactory(col -> new StockQuantityTableCell<>());
        colTotal.setCellValueFactory(new PropertyValueFactory<>("product"));
        colTotal.setCellFactory(col -> new ProductTotalTableCell<>());
        colOrdered.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colOrdered.setCellFactory(col -> new StockOrderedTableCell<>());
        colInStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colInStock.setCellFactory(col -> new StockRemainingTableCell<>());
        
        stocksTable.setItems(productItems);
        
        // purchase invoice context menu
        MenuItem mPrint = new MenuItem("Print");
        CheckMenuItem mShowDetails = new CheckMenuItem("Show Details");
        MenuItem mDeleteInvoice = new MenuItem("Delete");
        ContextMenu cm = new ContextMenu();
        cm.getItems().addAll(mPrint, mShowDetails, mDeleteInvoice);
        purchaseTable.setContextMenu(cm);
        
        MenuItem mEdit = new MenuItem("Edit");
        MenuItem mDelete = new MenuItem("Delete");
        MenuItem mOrders = new MenuItem("Show Orders");
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(mEdit, mDelete, mOrders);
        stocksTable.setContextMenu(contextMenu);
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
                    addPurchaseWindow.show();
                }),
                JavaFxObservable.actionEventsOf(btnRefresh).subscribe(evt -> {
                    onResume();
                }),
                JavaFxObservable.actionEventsOf(btnPrint).subscribe(evt -> {
                    printList();
                }),
                JavaFxObservable.actionEventsOf(mPrint).subscribe(evt -> {
                    PurchaseInvoice inv = purchaseTable.getSelectionModel().getSelectedItem();
                    if (inv != null) printPurchaseProducts(inv);
                }),
                JavaFxObservable.actionEventsOf(mShowDetails).subscribe(evt -> {
                    if (mShowDetails.isSelected() && !splitController.isTargetVisible()) {
                        splitController.showTarget();
                    } else {
                        splitController.hideTarget();
                    }
                }),
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
                JavaFxObservable.actionEventsOf(mEdit).subscribe(evt -> {
                    PurchaseProduct p = stocksTable.getSelectionModel().getSelectedItem();
                    if (p != null) editProductWindow.show(p.getStock());
                }),
                JavaFxObservable.actionEventsOf(mDelete).subscribe(evt -> {
                    PurchaseProduct p = stocksTable.getSelectionModel().getSelectedItem();
                    if (p != null) {
                        Optional<ButtonType> result = showConfirmDialog("Delete Product?", "Deleting this entry will also "
                                + "delete any related data. Continue?");
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            deleteProduct(p.getStock());
                        }
                    }
                }),
                JavaFxObservable.actionEventsOf(mOrders).subscribe(evt -> {
                    PurchaseProduct p = stocksTable.getSelectionModel().getSelectedItem();
                    if (p != null) {
                        showOrdersWindow.show(p.getProductId());
                    }
                }),
                JavaFxObservable.changesOf(purchaseTable.getSelectionModel().selectedItemProperty()).subscribe(inv -> {
                    refreshSelectedInvoice();
                })
        );
        
        splitController = new SplitController(splitPane, SplitController.Target.LAST);
        splitController.hideTarget();
    }

    @Override
    public void onPause() {
        purchaseTable.getSelectionModel().clearSelection();
//        splitController.hideTarget();
        productItems.clear();
    }

    @Override
    public void onResume() {
        mainWindow.showProgress(true, "Fetching purchase invoices...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getAllPurchaseInvoices();
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(invoices -> {
            mainWindow.showProgress(false);
            filteredList = new FilteredList<>(FXCollections.observableArrayList(invoices), s -> true);
            purchaseTable.setItems(filteredList);
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching inventory data.", err);
        }));
    }
    
    public void refreshSelectedInvoice() {
        PurchaseInvoice pi = purchaseTable.getSelectionModel().getSelectedItem();
        if (pi != null) {
            getPurchaseProducts(pi);
        }
    }
    
    private void deleteProduct(Stock stock) {
        mainWindow.showProgress(true, "Deleting product...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().deleteEntry("products", "id", stock.getProductId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            mainWindow.showProgress(false);
            onResume();
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while deleting product entry.", err);
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
    
    private void getPurchaseProducts(PurchaseInvoice invoice) {
        mainWindow.showProgress(true, "Fetching purchase products...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getPurchaseProducts(invoice.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(products -> {
            mainWindow.showProgress(false);
            productItems.setAll(products);
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching purchased products", err);
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
                ArrayList<PurchaseProduct> items = new ArrayList<>();
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
        addProductWindow.onDispose();
        editProductWindow.onDispose();
        showOrdersWindow.onDispose();
        printWindow.onDispose();
    }

}
