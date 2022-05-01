package org.gemseeker.app.views.tablecells;

import javafx.scene.control.TableCell;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class DiscountTableCell<T> extends TableCell<T, Double> {

    @Override
    protected void updateItem(Double item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty && item != null) {
            setText((int) (item * 100) + "%");
        } else {
            setText("");
        }
    }
    
}
