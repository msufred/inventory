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
import org.gemseeker.app.data.PurchaseProduct;
import org.gemseeker.app.data.Stock;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductTotalTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;
import org.gemseeker.app.views.tablecells.StockQuantityTableCell;

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
    
    @FXML private TableView<PurchaseProduct> itemsTable;
    @FXML private TableColumn<PurchaseProduct, Product> colItem;
    @FXML private TableColumn<PurchaseProduct, Product> colUnit; 
    @FXML private TableColumn<PurchaseProduct, Product> colPrice; 
    @FXML private TableColumn<PurchaseProduct, Stock> colQuantity;
    @FXML private TableColumn<PurchaseProduct, Product> colTotal;
    
    private PurchaseInvoice mInvoice;
    private ArrayList<PurchaseProduct> mItems;
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
        colPrice.setCellFactory(col -> new ProductPriceTableCell<>());
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colQuantity.setCellFactory(col -> new StockQuantityTableCell<>());
        colTotal.setCellValueFactory(new PropertyValueFactory<>("product"));
        colTotal.setCellFactory(col -> new ProductTotalTableCell<>());
    }
    
    public void set(PurchaseInvoice invoice, ArrayList<PurchaseProduct> items, int page, int totalPage) {
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
