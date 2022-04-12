package org.gemseeker.app.views.tablecells;

import javafx.scene.control.TableCell;

/**
 *
 * @author Gem
 */
public class InventoryPurchasedTableCell<T> extends TableCell<T, Integer> {

    @Override
    protected void updateItem(Integer item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty && item != null) {
            setText(item + "");
            getStyleClass().add("table-cell-green");
        } else {
            setText("");
            getStyleClass().remove("table-cell-green");
        }
    }
    
}
