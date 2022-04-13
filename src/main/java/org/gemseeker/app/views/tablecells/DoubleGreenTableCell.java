package org.gemseeker.app.views.tablecells;

import javafx.scene.control.TableCell;
import org.gemseeker.app.Utils;

/**
 *
 * @author Gem
 */
public class DoubleGreenTableCell<T> extends TableCell<T, Double> {

    @Override
    protected void updateItem(Double item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty && item != null) {
            setText(Utils.getMoneyFormat(item));
            getStyleClass().add("table-cell-green");
        } else {
            setText("");
            getStyleClass().remove("table-cell-green");
        }
    }
    
}
