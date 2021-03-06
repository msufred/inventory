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
import org.gemseeker.app.data.DeliveryInvoice;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.tablecells.DateTableCell;
import org.gemseeker.app.views.tablecells.PriceTableCell;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class PrintInvoiceList extends AbstractPanelController {
    
    @FXML private Label lblDate;
    @FXML private Label lblPage;
    
    @FXML private TableView<DeliveryInvoice> itemsTable;
    @FXML private TableColumn<DeliveryInvoice, LocalDate> colDate;
    @FXML private TableColumn<DeliveryInvoice, String> colNo;
    @FXML private TableColumn<DeliveryInvoice, String> colCustomer; 
    @FXML private TableColumn<DeliveryInvoice, String> colAddress; 
    @FXML private TableColumn<DeliveryInvoice, String> colStatus; 
    @FXML private TableColumn<DeliveryInvoice, Double> colTotal;
    
    private ArrayList<DeliveryInvoice> mItems;
    private int mPage;
    private int mTotalPage;
    
    public PrintInvoiceList() {
        super(PrintInvoiceList.class.getResource("print_invoice_list.fxml"));
    }

    @Override
    public void onLoad() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDate.setCellFactory(col -> new DateTableCell<>());
        colNo.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customer"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> new PriceTableCell<>());
    }
    
    public void set(ArrayList<DeliveryInvoice> items, int page, int totalPage) {
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
