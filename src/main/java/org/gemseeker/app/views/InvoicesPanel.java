package org.gemseeker.app.views;

import io.reactivex.disposables.CompositeDisposable;
import org.gemseeker.app.views.frameworks.AbstractPanelController;

/**
 *
 * @author Gem
 */
public class InvoicesPanel extends AbstractPanelController {
    
    private final MainWindow mainWindow;
    private final CompositeDisposable disposables;
    
    public InvoicesPanel(MainWindow mainWindow) {
        super(InventoryPanel.class.getResource("invoices.fxml"));
        this.mainWindow = mainWindow;
        disposables = new CompositeDisposable();
    }

    @Override
    public void onLoad() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }

}
