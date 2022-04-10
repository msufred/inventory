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
import org.gemseeker.app.data.OrderItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.tablecells.PriceTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSupplierTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class PrintOrder extends AbstractPanelController {
    
    @FXML private Label lblDate;
    @FXML private Label lblOrderDate;
    @FXML private Label lblName;
    @FXML private Label lblAmount;
    @FXML private Label lblPage;
    
    @FXML private TableView<OrderItem> itemsTable;
    @FXML private TableColumn<OrderItem, Product> colName;
    @FXML private TableColumn<OrderItem, Product> colSupplier;
    @FXML private TableColumn<OrderItem, Product> colUnit;
    @FXML private TableColumn<OrderItem, Product> colUnitPrice;
    @FXML private TableColumn<OrderItem, Integer> colQuantity;
    @FXML private TableColumn<OrderItem, Double> colTotal;
    
    private LocalDate mOrderDate;
    private String mOrderName;
    private double mTotal;
    private ArrayList<OrderItem> mItems;
    private int mPage;
    private int mTotalPage;
    
    public PrintOrder() {
        super(PrintOrder.class.getResource("print_order.fxml"));
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
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("listPrice"));
        colTotal.setCellFactory(col -> new PriceTableCell<>());
    }
    
    public void set(LocalDate orderDate, String orderName, double total, ArrayList<OrderItem> items, int page, int totalPage) {
        mOrderDate = orderDate;
        mOrderName = orderName;
        mTotal = total;
        mItems = items;
        mPage = page;
        mTotalPage = totalPage;
        getContent();
        fillUpFields();
    }

    private void fillUpFields() {
        lblDate.setText(LocalDate.now().format(Utils.dateTimeFormat));
        lblOrderDate.setText(mOrderDate.format(Utils.dateTimeFormat));
        lblName.setText(mOrderName);
        lblAmount.setText("P " + Utils.getMoneyFormat(mTotal));
        lblPage.setText("Page " + String.format("%d/%d", mPage, mTotalPage));
        itemsTable.setItems(FXCollections.observableArrayList(mItems));
    }
    
    public void clear() {
        lblName.setText("");
        lblAmount.setText("P 0.00");
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
