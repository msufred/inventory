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
            String text = ((int) item.doubleValue() * 100) + "%";
            setText(text);
        } else {
            setText("");
        }
    }
    
}
