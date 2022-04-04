package org.gemseeker.app.views.tablecells;

import javafx.scene.control.TableCell;
import org.gemseeker.app.Utils;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class PriceTableCell<T> extends TableCell<T, Double> {

    @Override
    protected void updateItem(Double item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty && item != null) {
            setText(Utils.getMoneyFormat(item));
        } else {
            setText("");
        }
    }
    
}
