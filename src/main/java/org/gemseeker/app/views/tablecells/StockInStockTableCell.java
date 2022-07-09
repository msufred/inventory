package org.gemseeker.app.views.tablecells;

import javafx.scene.control.TableCell;
import org.gemseeker.app.data.Stock;

/**
 *
 * @author Gem
 */
public class StockInStockTableCell<T> extends TableCell<T, Stock> {

    @Override
    protected void updateItem(Stock item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty && item != null) {
            setText(item.getInStock() + "");
            getStyleClass().add("table-cell-green");
        } else {
            setText("");
            getStyleClass().remove("table-cell-green");
        }
    }
    
}
