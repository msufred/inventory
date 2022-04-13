package org.gemseeker.app.views.tablecells;

import javafx.scene.control.TableCell;

/**
 *
 * @author Gem
 */
public class IntegerOrangeTableCell<T> extends TableCell<T, Integer> {

    @Override
    protected void updateItem(Integer item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty && item != null) {
            setText(item + "");
            getStyleClass().add("table-cell-orange");
        } else {
            setText("");
            getStyleClass().remove("table-cell-orange");
        }
    }
    
}
