package org.gemseeker.app.views.prints;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.PrintQuality;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import org.gemseeker.app.Utils;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.frameworks.AbstractWindowController;
import org.gemseeker.app.views.icons.PrintIcon;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class PrintWindow extends AbstractWindowController {
    
    @FXML private ComboBox<Printer> cbPrinters;
    @FXML private ComboBox<Paper> cbPapers;
    @FXML private TextField tfCopies;
    @FXML private Button btnPrint;
    @FXML private Button btnCancel;
    @FXML private Slider zoomSlider;
    @FXML private VBox pages;
    @FXML private ProgressBar progressBar;
    
    private final CompositeDisposable disposables;
    
    private ArrayList<? extends AbstractPanelController> mPanels;
    
    public PrintWindow(Stage mainStage) {
        super("Print", PrintWindow.class.getResource("print_window.fxml"), mainStage);
        disposables = new CompositeDisposable();
    }
    
    @Override
    public void onLoad() {
        Utils.setAsIntegerTextField(tfCopies);
        
        Scale scale = new Scale();
        scale.xProperty().bind(zoomSlider.valueProperty());
        scale.yProperty().bind(zoomSlider.valueProperty());
        pages.getTransforms().add(scale);
        
        btnPrint.setGraphic(new PrintIcon(14));
        
        disposables.addAll(
                JavaFxObservable.actionEventsOf(btnPrint).subscribe(evt -> {
                    if (!mPanels.isEmpty() && !tfCopies.getText().isEmpty()) {
                        printPages();
                    }
                }),
                JavaFxObservable.actionEventsOf(btnCancel).subscribe(evt -> {
                    close();
                }),
                JavaFxObservable.changesOf(cbPapers.valueProperty()).subscribe(paper -> {
                    if (paper.getNewVal() != null) {
                        resizePages();
                    }
                })
        );
    }

    public void show(ArrayList<? extends AbstractPanelController> panels) {
        super.show();
        showProgress(true);
        cbPrinters.setItems(FXCollections.observableArrayList(Printer.getAllPrinters()));
        cbPrinters.setValue(Printer.getDefaultPrinter());
        
        cbPapers.setItems(FXCollections.observableArrayList(
                Paper.A4, Paper.NA_LETTER, Paper.LEGAL
        ));
        cbPapers.setValue(Paper.A4);
        
        addAll(panels);
        showProgress(false);
    }

    private void addAll(ArrayList<? extends AbstractPanelController> panels) {
        for (AbstractPanelController panel : panels) {
            System.out.println("Adding panel");
            pages.getChildren().add(panel.getContent());
        }
        mPanels = panels;
    }
    
    private void resizePages() {
        if (mPanels != null) {
            Paper paper = cbPapers.getValue();
            double width = Math.round(paper.getWidth() / 72 * 150);
            double height = Math.round(paper.getHeight() / 72 * 150);
            for (AbstractPanelController panel : mPanels) {
                VBox pane = (VBox) panel.getContent();
                pane.setMinSize(width, height);
                pane.setMaxSize(width, height);
                pane.setPrefSize(width, height);
            }
        }
    }
    
    private void printPages() {
        Printer printer = cbPrinters.getValue();
        Paper paper = cbPapers.getValue();
        PageLayout pageLayout = printer.createPageLayout(paper, PageOrientation.PORTRAIT, 0, 0, 0, 0);
        
        showProgress(true);
        PrinterJob printerJob = PrinterJob.createPrinterJob(printer);
        if (printerJob != null) {
            printerJob.getJobSettings().setPrintQuality(PrintQuality.NORMAL);
            printerJob.getJobSettings().setPageLayout(pageLayout);
            
            for (AbstractPanelController panel : mPanels) {
                Pane pane = panel.getContent();
                double scaleX = pageLayout.getPrintableWidth() / pane.getBoundsInParent().getWidth();
                double scaleY = pageLayout.getPrintableHeight() / pane.getBoundsInParent().getHeight();
                Scale printScale = new Scale(scaleX, scaleY);
                pane.getTransforms().add(printScale);
                
                int copies = Integer.parseInt(tfCopies.getText().trim());
                for (int i = 0; i < copies; i++) {
                    printerJob.printPage(pane);
                }
                pane.getTransforms().remove(printScale);
            }
            printerJob.endJob();
        }
        showProgress(false);
    }
    
    private void showProgress(boolean show) {
        progressBar.setVisible(show);
        btnPrint.setDisable(show);
        btnCancel.setDisable(show);
    }
    
    @Override
    public void onClose() {
        pages.getChildren().clear();
        zoomSlider.setValue(0.5);
        mPanels = null;
    }

    @Override
    public void onDispose() {
        disposables.dispose();
    }

}
