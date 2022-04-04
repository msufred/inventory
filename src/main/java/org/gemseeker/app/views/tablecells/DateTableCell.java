package org.gemseeker.app.views.tablecells;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.scene.control.TableCell;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class DateTableCell<T> extends TableCell<T, LocalDate> {

    @Override
    protected void updateItem(LocalDate item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty && item != null) {
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            setText(item.format(dateFormat));
        } else {
            setText("");
        }
    }
    
}
