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
import org.gemseeker.app.data.DeliveryInvoiceItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.tablecells.DiscountTableCell;
import org.gemseeker.app.views.tablecells.PriceTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSupplierTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class PrintInvoice extends AbstractPanelController {
    
    @FXML private Label lblDate;
    @FXML private Label lblInvoiceNo;
    @FXML private Label lblInvoiceDate;
    @FXML private Label lblCustomer;
    @FXML private Label lblAddress;
    @FXML private Label lblAmount;
    @FXML private Label lblStatus;
    @FXML private Label lblPage;
    
    @FXML private TableView<DeliveryInvoiceItem> itemsTable;
    @FXML private TableColumn<DeliveryInvoiceItem, Product> colName;
    @FXML private TableColumn<DeliveryInvoiceItem, Product> colSupplier;
    @FXML private TableColumn<DeliveryInvoiceItem, Product> colUnit; 
    @FXML private TableColumn<DeliveryInvoiceItem, Product> colUnitPrice; 
    @FXML private TableColumn<DeliveryInvoiceItem, Double> colDiscount; 
    @FXML private TableColumn<DeliveryInvoiceItem, Double> colDiscountedPrice; 
    @FXML private TableColumn<DeliveryInvoiceItem, Integer> colQuantity; 
    @FXML private TableColumn<DeliveryInvoiceItem, Double> colTotal; 
    
    private LocalDate mInvoiceDate;
    private String mInvoiceNo;
    private String mCustomerName;
    private String mAddress;
    private String mStatus;
    private double mTotal;
    private ArrayList<DeliveryInvoiceItem> mItems;
    private int mPage;
    private int mTotalPage;
    
    public PrintInvoice() {
        super(PrintInvoice.class.getResource("print_invoice.fxml"));
    }

    @Override
    public void onLoad() {
        colName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colName.setCellFactory(col -> new ProductNameTableCell<>());
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("product"));
        colSupplier.setCellFactory(col -> new ProductSupplierTableCell<>());
        colUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colUnitPrice.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnitPrice.setCellFactory(col -> new ProductUnitPriceTableCell<>());
        colDiscount.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colDiscount.setCellFactory(col -> new DiscountTableCell<>());
        colDiscountedPrice.setCellValueFactory(new PropertyValueFactory<>("discountedPrice"));
        colDiscountedPrice.setCellFactory(col -> new PriceTableCell<>());
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("listPrice"));
        colTotal.setCellFactory(col -> new PriceTableCell<>());
    }
    
    public void set(String invoiceNo, LocalDate invoiceDate, String customer, String address,
            double total, String status, ArrayList<DeliveryInvoiceItem> items, int page, int totalPage) {
        getContent();
        mInvoiceNo = invoiceNo;
        mInvoiceDate = invoiceDate;
        mCustomerName = customer;
        mAddress = address;
        mTotal = total;
        mStatus = status;
        mItems = items;
        mPage = page;
        mTotalPage = totalPage;
        fillUpFields();
    }

    private void fillUpFields() {
        lblDate.setText(LocalDate.now().format(Utils.dateTimeFormat));
        lblInvoiceNo.setText(mInvoiceNo);
        lblInvoiceDate.setText(mInvoiceDate.format(Utils.dateTimeFormat));
        lblCustomer.setText(mCustomerName);
        lblAddress.setText(mAddress);
        lblAmount.setText("P " + Utils.toMoneyFormat(mTotal));
        lblStatus.setText(mStatus);
        lblPage.setText("Page " + String.format("%d/%d", mPage, mTotalPage));
        itemsTable.setItems(FXCollections.observableArrayList(mItems));
    }
    
    public void clear() {
        lblInvoiceNo.setText("");
        lblInvoiceDate.setText("");
        lblCustomer.setText("");
        lblAddress.setText("");
        lblAmount.setText("P 0.00");
        lblStatus.setText("");
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
