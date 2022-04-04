package org.gemseeker.app.views.tablecells;

import java.time.format.DateTimeFormatter;
import javafx.scene.control.TableCell;
import org.gemseeker.app.data.Product;

/**
 *
 * @author Gem
 */
public class ProductDateTableCell<T> extends TableCell<T, Product> {

    @Override
    protected void updateItem(Product item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty && item != null) {
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            setText(item.getDate().format(dateFormat));
        } else {
            setText("");
        }
    }
    
}
