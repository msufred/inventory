package org.gemseeker.app.views.tablecells;

import javafx.scene.control.TableCell;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.Product;

/**
 *
 * @author Gem
 */
public class ProductUnitPriceTableCell<T> extends TableCell<T, Product> {

    @Override
    protected void updateItem(Product item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty && item != null) {
            setText(Utils.toMoneyFormat(item.getUnitPrice()));
        } else {
            setText("");
        }
    }
    
}
