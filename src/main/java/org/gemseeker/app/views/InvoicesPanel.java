package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import java.time.LocalDate;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Invoice;
import org.gemseeker.app.data.InvoiceItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.frameworks.SplitController;
import org.gemseeker.app.views.tablecells.DateTableCell;
import org.gemseeker.app.views.tablecells.PriceTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSupplierTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author Gem
 */
public class InvoicesPanel extends AbstractPanelController {
    
    @FXML private Button btnAdd;
    @FXML private Label lblTotal;
    @FXML private TableView<Invoice> invoicesTable;
    @FXML private TableColumn<Invoice, LocalDate> colDate;
    @FXML private TableColumn<Invoice, String> colId;
    @FXML private TableColumn<Invoice, String> colCustomer;
    @FXML private TableColumn<Invoice, String> colAddress;
    @FXML private TableColumn<Invoice, String> colStatus;
    @FXML private TableColumn<Invoice, Double> colTotal;
    @FXML private TableView<InvoiceItem> itemsTable;
    @FXML private TableColumn<InvoiceItem, Product> colItemName;
    @FXML private TableColumn<InvoiceItem, Product> colItemSupplier;
    @FXML private TableColumn<InvoiceItem, Product> colItemUnit; 
    @FXML private TableColumn<InvoiceItem, Product> colItemPriceBefore; 
    @FXML private TableColumn<InvoiceItem, Double> colItemDiscount; 
    @FXML private TableColumn<InvoiceItem, Double> colItemPriceAfter; 
    @FXML private TableColumn<InvoiceItem, Integer> colItemQuantity; 
    @FXML private TableColumn<InvoiceItem, Double> colItemTotal;
    @FXML private SplitPane splitPane;
    private SplitController splitController;
    
    private final SimpleDoubleProperty mTotal = new SimpleDoubleProperty(0);
    
    private final MainWindow mainWindow;
    private final CompositeDisposable disposables;
    
    private FilteredList<Invoice> filteredList;
    
    private final AddInvoiceWindow addInvoiceWindow;
    
    public InvoicesPanel(MainWindow mainWindow) {
        super(InventoryPanel.class.getResource("invoices.fxml"));
        this.mainWindow = mainWindow;
        disposables = new CompositeDisposable();
        
        addInvoiceWindow = new AddInvoiceWindow(this, mainWindow.getWindow());
    }

    @Override
    public void onLoad() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDate.setCellFactory(col -> new DateTableCell<>());
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customer"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> new PriceTableCell<>());
        
        // items table
        colItemName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemName.setCellFactory(col -> new ProductNameTableCell<>());
        colItemSupplier.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemSupplier.setCellFactory(col -> new ProductSupplierTableCell<>());
        colItemUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colItemPriceBefore.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemPriceBefore.setCellFactory(col -> new ProductPriceTableCell<>());
        colItemDiscount.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colItemPriceAfter.setCellValueFactory(new PropertyValueFactory<>("discountedPrice"));
        colItemPriceAfter.setCellFactory(col -> new TableCell<InvoiceItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) setText(Utils.getMoneyFormat(item));
                else setText("");
            }
        });
        colItemQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colItemTotal.setCellValueFactory(new PropertyValueFactory<>("listPrice"));
        colItemTotal.setCellFactory(col -> new TableCell<InvoiceItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) setText(Utils.getMoneyFormat(item));
                else setText("");
            }
        });
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
                    addInvoiceWindow.show();
                }), 
                JavaFxObservable.changesOf(invoicesTable.getSelectionModel().selectedItemProperty()).subscribe(item -> {
                    if (item.getNewVal() != null) {
                        if (!splitController.isTargetVisible()) {
                            splitController.showTarget();
                        }
                        getInvoiceItems(item.getNewVal());
                    } else {
                        splitController.hideTarget();
                    }
                }),
                JavaFxObservable.changesOf(mTotal).subscribe(value -> {
                    if (value.getNewVal() != null) lblTotal.setText(Utils.getMoneyFormat(value.getNewVal().doubleValue()));
                })
        );
        
        splitController = new SplitController(splitPane, SplitController.Target.LAST);
        splitController.hideTarget();
    }

    @Override
    public void onPause() {
        itemsTable.getSelectionModel().clearSelection();
        splitController.hideTarget();
    }

    @Override
    public void onResume() {
        mainWindow.showProgress(true, "Fetching invoices...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getInvoices();
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(invoices -> {
            mainWindow.showProgress(false);
            filteredList = new FilteredList<>(FXCollections.observableArrayList(invoices));
            invoicesTable.setItems(filteredList);
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching invoices.", err);
        }));
    }
    
    private void getInvoiceItems(Invoice invoice) {
        mainWindow.showProgress(true, "Fetching invoice items...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getInvoiceItems(invoice.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(items -> {
            mainWindow.showProgress(false);
            itemsTable.setItems(FXCollections.observableArrayList(items));
            double total = 0;
            for (InvoiceItem item : items) {
                total += item.getListPrice();
            }
            mTotal.set(total);
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error while fetching invoice items.", err);
        }));
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }

}
