package org.gemseeker.app.views.prints;

import java.time.LocalDate;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.PurchaseInvoice;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.tablecells.DateTableCell;
import org.gemseeker.app.views.tablecells.PriceTableCell;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class PrintPurchaseInvoiceList extends AbstractPanelController {
    
    @FXML private Label lblDate;
    @FXML private Label lblPage;
    
    @FXML private TableView<PurchaseInvoice> itemsTable;
    @FXML private TableColumn<PurchaseInvoice, String> colNo;
    @FXML private TableColumn<PurchaseInvoice, LocalDate> colDate;
    @FXML private TableColumn<PurchaseInvoice, String> colSupplier; 
    @FXML private TableColumn<PurchaseInvoice, Double> colTotal;
    
    private ArrayList<PurchaseInvoice> mItems;
    private int mPage;
    private int mTotalPage;
    
    public PrintPurchaseInvoiceList() {
        super(PrintPurchaseInvoiceList.class.getResource("print_purchase_invoice_list.fxml"));
    }

    @Override
    public void onLoad() {
        colNo.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDate.setCellFactory(col -> new DateTableCell<>());
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> new PriceTableCell<>());
    }
    
    public void set(ArrayList<PurchaseInvoice> items, int page, int totalPage) {
        mItems = items;
        mPage = page;
        mTotalPage = totalPage;
        getContent();
        fillUpFields();
    }

    private void fillUpFields() {
        lblDate.setText(LocalDate.now().format(Utils.dateTimeFormat));
        lblPage.setText("Page " + String.format("%d/%d", mPage, mTotalPage));
        itemsTable.setItems(FXCollections.observableArrayList(mItems));
    }
    
    public void clear() {
        lblPage.setText("Page 1/1");
        itemsTable.setItems(null);
    }
    
    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onDispose() {
    }
    
}
