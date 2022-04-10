package org.gemseeker.app.views.prints;

import java.time.LocalDate;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.Stock;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSupplierTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class PrintInventoryList extends AbstractPanelController {
    
    @FXML private Label lblDate;
    @FXML private Label lblPage;
    
    @FXML private TableView<Stock> itemsTable;
    @FXML private TableColumn<Stock, Product> colDate;
    @FXML private TableColumn<Stock, Product> colItem;
    @FXML private TableColumn<Stock, Product> colSupplier; 
    @FXML private TableColumn<Stock, Product> colUnit; 
    @FXML private TableColumn<Stock, Product> colPrice; 
    @FXML private TableColumn<Stock, Integer> colQuantity;
    @FXML private TableColumn<Stock, Integer> colQuantityOut;
    @FXML private TableColumn<Stock, Product> colInStock;
    
    private ArrayList<Stock> mItems;
    private int mPage;
    private int mTotalPage;
    
    public PrintInventoryList() {
        super(PrintInvoiceList.class.getResource("print_inventory_list.fxml"));
    }

    @Override
    public void onLoad() {
        colItem.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItem.setCellFactory(col -> new ProductNameTableCell<>());
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("product"));
        colSupplier.setCellFactory(col -> new ProductSupplierTableCell<>());
        colUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colPrice.setCellValueFactory(new PropertyValueFactory<>("product"));
        colPrice.setCellFactory(col -> new ProductUnitPriceTableCell<>());
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colQuantityOut.setCellValueFactory(new PropertyValueFactory<>("quantityOut"));
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
    }
    
    public void set(ArrayList<Stock> items, int page, int totalPage) {
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
