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
import org.gemseeker.app.views.tablecells.ProductDateTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductSkuTableCell;
import org.gemseeker.app.views.tablecells.ProductSupplierTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author Gem
 */
public class InventoryPanel extends AbstractPanelController {
    
    @FXML private Button btnAdd;
    @FXML private Button btnRefresh;
    @FXML private Button btnPrint;
    @FXML private TableView<Stock> stocksTable;
    @FXML private TableColumn<Stock, Product> colDate;
    @FXML private TableColumn<Stock, Product> colName;
    @FXML private TableColumn<Stock, Product> colUnit;
    @FXML private TableColumn<Stock, Product> colSku;
    @FXML private TableColumn<Stock, Integer> colQuantity;
    @FXML private TableColumn<Stock, Integer> colQuantityOut;
    @FXML private TableColumn<Stock, Product> colInStock;
    @FXML private TableColumn<Stock, Product> colSupplier;
    
    private final MainWindow mainWindow;
    private final CompositeDisposable disposables;
    
    private FilteredList<Stock> filteredList;
    
    private final AddProductWindow addProductWindow;
    
    public InventoryPanel(MainWindow mainWindow) {
        super(InventoryPanel.class.getResource("warehouse.fxml"));
        this.mainWindow = mainWindow;
        disposables = new CompositeDisposable();
        
        addProductWindow = new AddProductWindow(this, mainWindow.getWindow());
    }

    @Override
    public void onLoad() {
        // setup table
        colDate.setCellValueFactory(new PropertyValueFactory<>("product"));
        colDate.setCellFactory(col -> new ProductDateTableCell<>());
        colName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colName.setCellFactory(col -> new ProductNameTableCell<>());
        colUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colSku.setCellValueFactory(new PropertyValueFactory<>("product"));
        colSku.setCellFactory(col -> new ProductSkuTableCell<>());
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colQuantityOut.setCellValueFactory(new PropertyValueFactory<>("quantityOut"));
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("product"));
        colSupplier.setCellFactory(col -> new ProductSupplierTableCell<>());
        colInStock.setCellValueFactory(new PropertyValueFactory<>("product"));
        colInStock.setCellFactory(col -> new TableCell<Stock, Product>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    Stock stock = (Stock) getTableRow().getItem();
                    if (stock != null) {
                        int inStock = stock.getQuantity() - stock.getQuantityOut();
                        setText(inStock + "");
                    } else setText("");
                } else setText("");
            }
        });
        
        MenuItem mDelete = new MenuItem("Delete Product");
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(mDelete);
        stocksTable.setContextMenu(contextMenu);
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
                    addProductWindow.show();
                }),
                JavaFxObservable.actionEventsOf(btnRefresh).subscribe(evt -> {
                    onResume();
                }),
                JavaFxObservable.actionEventsOf(btnPrint).subscribe(evt -> {
                    
                }),
                JavaFxObservable.actionEventsOf(mDelete).subscribe(evt -> {
                    Stock s = stocksTable.getSelectionModel().getSelectedItem();
                    if (s != null) {
                        Optional<ButtonType> result = showConfirmDialog("Delete Product?", "Deleting this entry will also "
                                + "delete any related data. Continue?");
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            deleteProduct(s);
                        }
                    }
                })
        );
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
    
    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
        mainWindow.showProgress(true, "Fetching stocks...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getStocks();
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(stocks -> {
            mainWindow.showProgress(false);
            filteredList = new FilteredList<>(FXCollections.observableArrayList(stocks), s -> true);
            stocksTable.setItems(filteredList);
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching inventory data.", err);
        }));
    }

    @Override
    public void onDispose() {
        disposables.dispose();
        addProductWindow.onDispose();
    }

}
