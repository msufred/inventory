package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import java.time.LocalDate;
import java.util.ArrayList;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.DeliveryInvoice;
import org.gemseeker.app.data.DeliveryInvoiceItem;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Order;
import org.gemseeker.app.data.OrderItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.Shipper;
import org.gemseeker.app.data.ShipperStock;
import org.gemseeker.app.data.views.ShipperStockMonthlyView;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.tablecells.DoubleGreenTableCell;
import org.gemseeker.app.views.tablecells.IntegerGreenTableCell;
import org.gemseeker.app.views.tablecells.IntegerOrangeTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductRetailPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSupplierTableCell;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class ShippersStocksPanel extends AbstractPanelController {
    
    @FXML private ListView<Shipper> shippersList;
    @FXML private ComboBox<String> cbMonths;
    @FXML private ComboBox<Integer> cbYears;
    @FXML private TableView<ShipperStockMonthlyView> stocksTable;
    @FXML private TableColumn<ShipperStockMonthlyView, Product> colName;
    @FXML private TableColumn<ShipperStockMonthlyView, Product> colSupplier;
    @FXML private TableColumn<ShipperStockMonthlyView, Product> colRetailPrice;
    @FXML private TableColumn<ShipperStockMonthlyView, ShipperStock> colInStock;
    @FXML private TableColumn<ShipperStockMonthlyView, Integer> colOrdered;
    @FXML private TableColumn<ShipperStockMonthlyView, Integer> colDelivered;
    @FXML private TableColumn<ShipperStockMonthlyView, Double> colSales;
    @FXML private TableColumn<ShipperStockMonthlyView, Double> colMonth;
    
    private final MainWindow mainWindow;
    private final CompositeDisposable disposables;
    
//    private final ObservableList<ShipperStock> stockItems = FXCollections.observableArrayList();
    private final ObservableList<ShipperStockMonthlyView> stockItems = FXCollections.observableArrayList();
    private final SimpleObjectProperty<Shipper> selected = new SimpleObjectProperty<>();
    
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
        colInStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colInStock.setCellFactory(col -> new TableCell<ShipperStockMonthlyView, ShipperStock>() {
            @Override
            protected void updateItem(ShipperStock item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item.getInStock() + "");
                } else setText("");
            }
        });
        colOrdered.setCellValueFactory(new PropertyValueFactory<>("ordered"));
        colOrdered.setCellFactory(col -> new IntegerGreenTableCell<>());
        colDelivered.setCellValueFactory(new PropertyValueFactory<>("delivered"));
        colDelivered.setCellFactory(col -> new IntegerOrangeTableCell<>());
        colSales.setCellValueFactory(new PropertyValueFactory<>("sales"));
        colSales.setCellFactory(col -> new DoubleGreenTableCell<>());
        
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
        
        stocksTable.setItems(stockItems);
        
        disposables.addAll(
                JavaFxObservable.changesOf(shippersList.getSelectionModel().selectedItemProperty()).subscribe(item -> {
                    selected.set(item.getNewVal());
                    updateMonthColumn();
                    filterByDate();
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
    
    private void updateMonthColumn() {
        if (cbMonths.getValue() != null && cbYears.getValue() != -1) {
            colMonth.setText(String.format("%s %d", cbMonths.getValue(), cbYears.getValue()).toUpperCase());
        }
    }
    
    private void filterByDate() {
        if (selected.get() != null && cbMonths.getValue() != null && cbYears.getValue() != -1) {
            int month = Utils.monthIntegerValue(cbMonths.getValue());
            int year = cbYears.getValue();
            LocalDate date = LocalDate.of(year, month, 1);
            getShipperStocks(selected.get(), date);
        }
    }
    
    private void getShipperStocks(Shipper shipper, LocalDate date) {
        mainWindow.showProgress(true, "Fetching shipper stocks data...");
        disposables.add(Single.fromCallable(() -> {
            EmbeddedDatabase database = EmbeddedDatabase.getInstance();
            ArrayList<ShipperStock> stocks = database.getShipperStocks(shipper.getId());                    // get all ShipperStocks
            ArrayList<Order> orders = database.getOrders(shipper.getId(), date);                            // get all Orders of the month
            ArrayList<OrderItem> orderItems = database.getOrderItems(date);                                 // get all OrderItems of the month
            ArrayList<DeliveryInvoice> deliveries = database.getDeliveryInvoices(shipper.getId(), date);    // get all DeliveryInvoice of the month
            ArrayList<DeliveryInvoiceItem> deliveryItems = database.getDeliveryInvoiceItems(date);          // get all DeliveryInvoiceItems of the month
            
            ArrayList<ShipperStockMonthlyView> monthlyStocks = new ArrayList<>();
            for (ShipperStock ss : stocks) {
                ShipperStockMonthlyView view = new ShipperStockMonthlyView();
                view.setProduct(ss.getProduct());
                view.setStock(ss);
                
                int ordered = 0;
                for (Order order : orders) {
                    for (OrderItem item : orderItems) {
                        if (item.getOrderId() == order.getId() && item.getProductId() == ss.getProductId()) {
                            ordered += item.getQuantity();
                        }
                    }
                }
                view.setOrdered(ordered);
                
                int delivered = 0;
                double sales = 0;
                for (DeliveryInvoice invoice: deliveries) {
                    for (DeliveryInvoiceItem item : deliveryItems) {
                        if (item.getInvoiceId().equals(invoice.getId()) && item.getProductId() == ss.getProductId()) {
                            delivered += item.getQuantity();
                            sales += item.getTotal();
                        }
                    }
                }
                view.setDelivered(delivered);
                view.setSales(sales);
                monthlyStocks.add(view);
            }
            
            return monthlyStocks;
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
