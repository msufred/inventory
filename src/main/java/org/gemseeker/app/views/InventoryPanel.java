package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.Stock;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.tablecells.PriceTableCell;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class InventoryPanel extends AbstractPanelController {
    
    @FXML private Button btnRefresh;
    @FXML private Button btnPrint;
    @FXML private TableView<Product> itemsTable;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colSku;
    @FXML private TableColumn<Product, String> colSupplier;
    @FXML private TableColumn<Product, String> colUnit;
    @FXML private TableColumn<Product, Double> colUnitPrice;
    @FXML private TableColumn<Product, Double> colRetailPrice;
    @FXML private TableColumn<Product, Stock> colOrdered;
    @FXML private TableColumn<Product, Stock> colInStock;
    
    private FilteredList<Product> filteredList;
    
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
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colUnitPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colUnitPrice.setCellFactory(col -> new PriceTableCell<>());
        colRetailPrice.setCellValueFactory(new PropertyValueFactory<>("retailPrice"));
        colRetailPrice.setCellFactory(col -> new PriceTableCell<>());
        colOrdered.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colOrdered.setCellFactory(col -> new TableCell<Product, Stock>() {
            @Override
            protected void updateItem(Stock item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) setText(item.getQuantityOut() + "");
                else setText("");
            }
            
        });
        colInStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colInStock.setCellFactory(col -> new TableCell<Product, Stock>() {
            @Override
            protected void updateItem(Stock item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    if (item.getInStock() == 0) setText("Out of Stock");
                    else setText(item.getInStock()+ "");
                }
                else setText("");
            }
            
        });
        
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
                    Product p = itemsTable.getSelectionModel().getSelectedItem();
                    if (p != null) {
                        showOrdersWindow.show(p);
                    }
                }),
                JavaFxObservable.actionEventsOf(mEdit).subscribe(evt -> {
                    Product p = itemsTable.getSelectionModel().getSelectedItem();
                    if (p != null) {
                        editProductWindow.show(p);
                    }
                }),
                JavaFxObservable.actionEventsOf(mDelete).subscribe(evt -> {
                    Product p = itemsTable.getSelectionModel().getSelectedItem();
                    if (p != null) {
                        Optional<ButtonType> result = showConfirmDialog("Delete Product?", "This will delete this product permanently. Deleting "
                                + "this entry might affect related data. Continue?");
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            deleteProduct(p);
                        }
                    }
                })
        );
    }

    @Override
    public void onPause() {
        itemsTable.getSelectionModel().clearSelection();
    }

    @Override
    public void onResume() {
        mainWindow.showProgress(true, "Fetching all products...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getProducts();
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
