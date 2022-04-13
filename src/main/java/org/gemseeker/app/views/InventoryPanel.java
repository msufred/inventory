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
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.OrderItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.PurchaseInvoiceItem;
import org.gemseeker.app.data.Stock;
import org.gemseeker.app.data.views.ProductMonthlyView;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.tablecells.DoubleGreenTableCell;
import org.gemseeker.app.views.tablecells.DoubleOrangeTableCell;
import org.gemseeker.app.views.tablecells.IntegerGreenTableCell;
import org.gemseeker.app.views.tablecells.IntegerOrangeTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductRetailPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSkuTableCell;
import org.gemseeker.app.views.tablecells.ProductSupplierTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;
import org.gemseeker.app.views.tablecells.StockInStockTableCell;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class InventoryPanel extends AbstractPanelController {
    
    @FXML private Button btnRefresh;
    @FXML private Button btnPrint;
    @FXML private ComboBox<String> cbMonths;
    @FXML private ComboBox<Integer> cbYears;
    @FXML private TableView<ProductMonthlyView> itemsTable;
    @FXML private TableColumn<ProductMonthlyView, Product> colName;
    @FXML private TableColumn<ProductMonthlyView, Product> colSku;
    @FXML private TableColumn<ProductMonthlyView, Product> colSupplier;
    @FXML private TableColumn<ProductMonthlyView, Product> colUnit;
    @FXML private TableColumn<ProductMonthlyView, Product> colUnitPrice;
    @FXML private TableColumn<ProductMonthlyView, Product> colRetailPrice;
    @FXML private TableColumn<ProductMonthlyView, Stock> colInStock;
    @FXML private TableColumn<ProductMonthlyView, Integer> colPurchased;
    @FXML private TableColumn<ProductMonthlyView, Double> colPurchasedTotal;
    @FXML private TableColumn<ProductMonthlyView, Integer> colOrdered;
    @FXML private TableColumn<ProductMonthlyView, Double> colOrderedTotal;
    @FXML private TableColumn<ProductMonthlyView, Product> colMonth;
    
//    private FilteredList<Product> filteredList;
    private FilteredList<ProductMonthlyView> filteredList;
    
    private final MainWindow mainWindow;
    private final CompositeDisposable disposables;
    
    private final ShowOrdersWindow showOrdersWindow;
    private final EditProductWindow editProductWindow;
    
    public InventoryPanel(MainWindow mainWindow) {
        super(InventoryPanel.class.getResource("inventory.fxml"));
        this.mainWindow = mainWindow;
        disposables = new CompositeDisposable();
        
        showOrdersWindow = new ShowOrdersWindow(mainWindow.getMainStage());
        editProductWindow = new EditProductWindow(this, mainWindow.getMainStage());
    }

    @Override
    public void onLoad() {
        colName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colName.setCellFactory(col -> new ProductNameTableCell<>());
        colSku.setCellValueFactory(new PropertyValueFactory<>("product"));
        colSku.setCellFactory(col -> new ProductSkuTableCell<>());
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("product"));
        colSupplier.setCellFactory(col -> new ProductSupplierTableCell<>());
        colUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colUnitPrice.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnitPrice.setCellFactory(col -> new ProductUnitPriceTableCell<>());
        colRetailPrice.setCellValueFactory(new PropertyValueFactory<>("product"));
        colRetailPrice.setCellFactory(col -> new ProductRetailPriceTableCell<>());
        colInStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colInStock.setCellFactory(col -> new StockInStockTableCell<>());
        colPurchased.setCellValueFactory(new PropertyValueFactory<>("purchased"));
        colPurchased.setCellFactory(col -> new IntegerGreenTableCell<>());
        colPurchasedTotal.setCellValueFactory(new PropertyValueFactory<>("purchasedTotal"));
        colPurchasedTotal.setCellFactory(col -> new DoubleGreenTableCell<>());
        colOrdered.setCellValueFactory(new PropertyValueFactory<>("ordered"));
        colOrdered.setCellFactory(col -> new IntegerOrangeTableCell<>());
        colOrderedTotal.setCellValueFactory(new PropertyValueFactory<>("orderedTotal"));
        colOrderedTotal.setCellFactory(col -> new DoubleOrangeTableCell<>());

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
        updateMonthColumn();
        
        MenuItem mShowOrders = new MenuItem("Show Orders");
        MenuItem mEdit = new MenuItem("Edit Details");
        MenuItem mDelete = new MenuItem("Delete Product");
        ContextMenu cm = new ContextMenu();
        cm.getItems().addAll(mShowOrders, mEdit, mDelete);
        itemsTable.setContextMenu(cm);
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(btnRefresh).subscribe(evt -> {
                    onResume();
                }),
                JavaFxObservable.actionEventsOf(btnPrint).subscribe(evt -> {
                    
                }),
                JavaFxObservable.actionEventsOf(mShowOrders).subscribe(evt -> {
                    ProductMonthlyView p = itemsTable.getSelectionModel().getSelectedItem();
                    if (p != null) {
                        showOrdersWindow.show(p.getProduct());
                    }
                }),
                JavaFxObservable.actionEventsOf(mEdit).subscribe(evt -> {
                    ProductMonthlyView p = itemsTable.getSelectionModel().getSelectedItem();
                    if (p != null) {
                        editProductWindow.show(p.getProduct());
                    }
                }),
                JavaFxObservable.actionEventsOf(mDelete).subscribe(evt -> {
                    ProductMonthlyView p = itemsTable.getSelectionModel().getSelectedItem();
                    if (p != null) {
                        Optional<ButtonType> result = showConfirmDialog("Delete Product?", "This will delete this product permanently. Deleting "
                                + "this entry might affect related data. Continue?");
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            deleteProduct(p.getProduct());
                        }
                    }
                }),
                JavaFxObservable.changesOf(cbMonths.valueProperty()).subscribe(month -> {
                    updateMonthColumn();
                    filterByDate();
                }),
                JavaFxObservable.changesOf(cbYears.valueProperty()).subscribe(month -> {
                    updateMonthColumn();
                    filterByDate();
                })
        );
    }

    @Override
    public void onPause() {
        itemsTable.getSelectionModel().clearSelection();
    }

    @Override
    public void onResume() {
//        mainWindow.showProgress(true, "Fetching all products...");
//        disposables.add(Single.fromCallable(() -> {
////            return EmbeddedDatabase.getInstance().getProducts();
//        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(products -> {
//            mainWindow.showProgress(false);
//            filteredList = new FilteredList<>(FXCollections.observableArrayList(products));
//            itemsTable.setItems(filteredList);
//        }, err -> {
//            mainWindow.showProgress(false);
//            showErrorDialog("Database Error", "Error while fetching products.", err);
//        }));
        filterByDate();
    }
    
    private void updateMonthColumn() {
        if (cbMonths.getValue() != null && cbYears.getValue() != -1) {
            colMonth.setText(String.format("%s %d", cbMonths.getValue(), cbYears.getValue()).toUpperCase());
        }
    }
    
    private void filterByDate() {
        if (cbMonths.getValue() != null && cbYears.getValue() != -1) {
            int month = Utils.monthIntegerValue(cbMonths.getValue());
            int year = cbYears.getValue();
            LocalDate date = LocalDate.of(year, month, 1);
            getProducts(date);
        }
    }
    
    private void getProducts(LocalDate date) {
        mainWindow.showProgress(true, "Fetching all products...");
        disposables.add(Single.fromCallable(() -> {
            EmbeddedDatabase database = EmbeddedDatabase.getInstance();
            ArrayList<Product> products = database.getProducts();
            ArrayList<PurchaseInvoiceItem> purchaseItems = database.getPurchaseInvoiceItems(date);
            ArrayList<OrderItem> orderedItems = database.getOrderItems(date);
            ArrayList<ProductMonthlyView> views = new ArrayList<>();
            for (Product p : products) {
                ProductMonthlyView view = new ProductMonthlyView();
                view.setProduct(p);
                view.setStock(p.getStock());
                int purchasedQty = 0;
                double purchasedTotal = 0;
                for (PurchaseInvoiceItem item : purchaseItems) {
                    if (item.getProductId() == p.getId()) {
                        purchasedQty += item.getQuantity();
                        purchasedTotal += item.getTotal();
                    }
                }
                view.setPurchased(purchasedQty);
                view.setPurchasedTotal(purchasedTotal);
                int orderedQty = 0;
                double orderedTotal = 0;
                for (OrderItem item : orderedItems) {
                    if (item.getProductId() == p.getId()) {
                        orderedQty += item.getQuantity();
                        orderedTotal += item.getTotal();
                    }
                }
                view.setOrdered(orderedQty);
                view.setOrderedTotal(orderedTotal);
                views.add(view);
            }
            return views;
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(products -> {
            mainWindow.showProgress(false);
            filteredList = new FilteredList<>(FXCollections.observableArrayList(products));
            itemsTable.setItems(filteredList);
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error while fetching products.", err);
        }));
    }
    
    private void deleteProduct(Product product) {
        mainWindow.showProgress(true, "Deleting product...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().deleteEntry("products", "id", product.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            mainWindow.showProgress(false);
            onResume();
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while deleting product entry.", err);
        }));
    }

    @Override
    public void onDispose() {
        disposables.dispose();
        editProductWindow.onDispose();
    }
    
}
