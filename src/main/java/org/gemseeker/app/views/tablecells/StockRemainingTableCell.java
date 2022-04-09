package org.gemseeker.app.views.tablecells;

import javafx.scene.control.TableCell;
import org.gemseeker.app.data.Stock;

/**
 *
 * @author Gem
 */
public class StockRemainingTableCell<T> extends TableCell<T, Stock> {

    @Override
    protected void updateItem(Stock item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty && item != null) {
            int remaining = item.getQuantity() - item.getQuantityOut();
            setText(remaining + "");
        } else {
            setText("");
        }
    }
    
}
