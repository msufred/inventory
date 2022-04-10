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
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.PurchaseInvoice;
import org.gemseeker.app.data.PurchaseInvoiceItem;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.tablecells.PriceTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class PrintPurchaseInvoice extends AbstractPanelController {
    
    @FXML private Label lblDate;
    @FXML private Label lblPage;
    
    @FXML private Label lblInvoiceNo;
    @FXML private Label lblInvoiceDate;
    @FXML private Label lblSupplier;
    @FXML private Label lblAmount;
    
    @FXML private TableView<PurchaseInvoiceItem> itemsTable;
    @FXML private TableColumn<PurchaseInvoiceItem, Product> colItem;
    @FXML private TableColumn<PurchaseInvoiceItem, Product> colUnit; 
    @FXML private TableColumn<PurchaseInvoiceItem, Product> colPrice; 
    @FXML private TableColumn<PurchaseInvoiceItem, Integer> colQuantity;
    @FXML private TableColumn<PurchaseInvoiceItem, Double> colTotal;
    
    private PurchaseInvoice mInvoice;
    private ArrayList<PurchaseInvoiceItem> mItems;
    private int mPage;
    private int mTotalPage;
    
    public PrintPurchaseInvoice() {
        super(PrintPurchaseInvoice.class.getResource("print_purchase_invoice.fxml"));
    }

    @Override
    public void onLoad() {
        colItem.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItem.setCellFactory(col -> new ProductNameTableCell<>());
        colUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colPrice.setCellValueFactory(new PropertyValueFactory<>("product"));
        colPrice.setCellFactory(col -> new ProductUnitPriceTableCell<>());
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> new PriceTableCell<>());
    }
    
    public void set(PurchaseInvoice invoice, ArrayList<PurchaseInvoiceItem> items, int page, int totalPage) {
        mInvoice = invoice;
        mItems = items;
        mPage = page;
        mTotalPage = totalPage;
        getContent();
        fillUpFields();
    }

    private void fillUpFields() {
        lblDate.setText(LocalDate.now().format(Utils.dateTimeFormat));
        lblPage.setText("Page " + String.format("%d/%d", mPage, mTotalPage));
        lblInvoiceNo.setText(mInvoice.getId());
        lblInvoiceDate.setText(Utils.dateTimeFormat.format(mInvoice.getDate()));
        lblSupplier.setText(mInvoice.getSupplier());
        lblAmount.setText("P " + Utils.getMoneyFormat(mInvoice.getTotal()));
        itemsTable.setItems(FXCollections.observableArrayList(mItems));
    }
    
    public void clear() {
        lblPage.setText("Page 1/1");
        lblInvoiceNo.setText("");
        lblInvoiceDate.setText("");
        lblSupplier.setText("");
        lblAmount.setText("");
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
