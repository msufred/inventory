package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.Shipper;
import org.gemseeker.app.data.ShipperStock;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.tablecells.PriceTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductRetailPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSupplierTableCell;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class ShippersStocksPanel extends AbstractPanelController {
    
    @FXML private ListView<Shipper> shippersList;
    @FXML private TableView<ShipperStock> stocksTable;
    @FXML private TableColumn<ShipperStock, Product> colName;
    @FXML private TableColumn<ShipperStock, Product> colSupplier;
    @FXML private TableColumn<ShipperStock, Product> colRetailPrice;
    @FXML private TableColumn<ShipperStock, Integer> colInStock;
    @FXML private TableColumn<ShipperStock, Integer> colDelivered;
    @FXML private TableColumn<ShipperStock, Double> colSales;
    
    private final MainWindow mainWindow;
    private final CompositeDisposable disposables;
    
    private final ObservableList<ShipperStock> stockItems = FXCollections.observableArrayList();
    
    public ShippersStocksPanel(MainWindow mainWindow) {
        super(ShippersStocksPanel.class.getResource("shipper_stocks.fxml"));
        this.mainWindow = mainWindow;
        disposables = new CompositeDisposable();
    }

    @Override
    public void onLoad() {
        colName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colName.setCellFactory(col -> new ProductNameTableCell<>());
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("product"));
        colSupplier.setCellFactory(col -> new ProductSupplierTableCell<>());
        colRetailPrice.setCellValueFactory(new PropertyValueFactory<>("product"));
        colRetailPrice.setCellFactory(col -> new ProductRetailPriceTableCell<>());
        colInStock.setCellValueFactory(new PropertyValueFactory<>("inStock"));
        colDelivered.setCellValueFactory(new PropertyValueFactory<>("delivered"));
        colSales.setCellValueFactory(new PropertyValueFactory<>("sales"));
        colSales.setCellFactory(col -> new PriceTableCell<>());
        
        stocksTable.setItems(stockItems);
        
        disposables.addAll(
                JavaFxObservable.changesOf(shippersList.getSelectionModel().selectedItemProperty()).subscribe(item -> {
                    if (item.getNewVal() != null) getShipperStocks(item.getNewVal());
                })
        );
    }

    @Override
    public void onPause() {
        shippersList.getSelectionModel().clearSelection();
        stockItems.clear();
    }

    @Override
    public void onResume() {
        mainWindow.showProgress(true, "Fetching shipper stocks data...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getShippers();
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(shippers -> {
            mainWindow.showProgress(false);
            shippersList.setItems(FXCollections.observableArrayList(shippers));
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error while fetching shipper stocks.", err);
        }));
    }
    
    private void getShipperStocks(Shipper shipper) {
        mainWindow.showProgress(true, "Fetching shipper stocks data...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getShipperStocks(shipper.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(stocks -> {
            mainWindow.showProgress(false);
            stockItems.setAll(stocks);
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error while fetching shipper stocks.", err);
        }));
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }
    
}
