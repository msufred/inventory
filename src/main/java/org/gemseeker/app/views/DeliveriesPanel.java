package org.gemseeker.app.views;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Optional;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.DeliveryInvoice;
import org.gemseeker.app.data.DeliveryInvoiceItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.frameworks.SplitController;
import org.gemseeker.app.views.icons.PrintIcon;
import org.gemseeker.app.views.prints.PrintInvoice;
import org.gemseeker.app.views.prints.PrintInvoiceList;
import org.gemseeker.app.views.prints.PrintWindow;
import org.gemseeker.app.views.tablecells.DateTableCell;
import org.gemseeker.app.views.tablecells.DiscountTableCell;
import org.gemseeker.app.views.tablecells.PriceTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductRetailPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSupplierTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author Gem
 */
public class DeliveriesPanel extends AbstractPanelController {
    
    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnPrintList;
    
    @FXML private RadioButton toggleShowAll;
    @FXML private RadioButton toggleFilterDate;
    @FXML private DatePicker dateFrom;
    @FXML private DatePicker dateTo;
    @FXML private TextField tfSearch;
    @FXML private Label lblTotal;
    @FXML private ComboBox<String> cbStatus;
    @FXML private Label lblMonthYear;
    @FXML private Label lblCash;
    @FXML private Label lblCheque;
    @FXML private Label lblReceivables;
    
    @FXML private TableView<DeliveryInvoice> invoicesTable;
    @FXML private TableColumn<DeliveryInvoice, LocalDate> colDate;
    @FXML private TableColumn<DeliveryInvoice, String> colId;
    @FXML private TableColumn<DeliveryInvoice, String> colCustomer;
    @FXML private TableColumn<DeliveryInvoice, String> colAddress;
    @FXML private TableColumn<DeliveryInvoice, String> colStatus;
    @FXML private TableColumn<DeliveryInvoice, Double> colTotal;
    
    @FXML private TableView<DeliveryInvoiceItem> itemsTable;
    @FXML private TableColumn<DeliveryInvoiceItem, Product> colItemName;
    @FXML private TableColumn<DeliveryInvoiceItem, Product> colItemSupplier;
    @FXML private TableColumn<DeliveryInvoiceItem, Product> colItemUnit; 
    @FXML private TableColumn<DeliveryInvoiceItem, Product> colItemPriceBefore; 
    @FXML private TableColumn<DeliveryInvoiceItem, Double> colItemDiscount; 
    @FXML private TableColumn<DeliveryInvoiceItem, Double> colItemPriceAfter; 
    @FXML private TableColumn<DeliveryInvoiceItem, Integer> colItemQuantity; 
    @FXML private TableColumn<DeliveryInvoiceItem, Double> colItemTotal;
    @FXML private SplitPane splitPane;
    private SplitController splitController;
    
    private final ObservableList<DeliveryInvoiceItem> invoiceItems = FXCollections.observableArrayList();
    private final SimpleDoubleProperty mTotal = new SimpleDoubleProperty(0);
    
    private final MainWindow mainWindow;
    private final CompositeDisposable disposables;
    
    private FilteredList<DeliveryInvoice> filteredList;
    
    private final AddDeliveryWindow addInvoiceWindow;
    private final EditDeliveryWindow editInvoiceWindow;
    private final PrintWindow printWindow;
    
    private final DirectoryChooser dirChooser;
    
    private LocalDate mDateFrom;
    private LocalDate mDateTo;
    
    public DeliveriesPanel(MainWindow mainWindow) {
        super(PurchasesPanel.class.getResource("deliveries.fxml"));
        this.mainWindow = mainWindow;
        disposables = new CompositeDisposable();
        
        addInvoiceWindow = new AddDeliveryWindow(this, mainWindow.getWindow());
        editInvoiceWindow = new EditDeliveryWindow(this, mainWindow.getWindow());
        printWindow = new PrintWindow(mainWindow.getWindow());
        
        dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Destination");
    }

    @Override
    public void onLoad() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDate.setCellFactory(col -> new DateTableCell<>());
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customer"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> new PriceTableCell<>());
        
        // items table
        colItemName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemName.setCellFactory(col -> new ProductNameTableCell<>());
        colItemSupplier.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemSupplier.setCellFactory(col -> new ProductSupplierTableCell<>());
        colItemUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colItemPriceBefore.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemPriceBefore.setCellFactory(col -> new ProductRetailPriceTableCell<>());
        colItemDiscount.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colItemDiscount.setCellFactory(col -> new DiscountTableCell<>());
        colItemPriceAfter.setCellValueFactory(new PropertyValueFactory<>("discountedPrice"));
        colItemPriceAfter.setCellFactory(col -> new PriceTableCell<>());
        colItemQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colItemTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colItemTotal.setCellFactory(col -> new PriceTableCell<>());
        
        itemsTable.setItems(invoiceItems);
        
        cbStatus.setItems(FXCollections.observableArrayList("All", "Cash", "Cheque", "Receivable"));

        CheckMenuItem mShowDetails = new CheckMenuItem("Show Details");
        mShowDetails.setSelected(true);
        MenuItem mPrint = new MenuItem("Print");
        MenuItem mExport = new MenuItem("Export");
        MenuItem mDelete = new MenuItem("Delete");
        Menu menuUpdate = new Menu("Change Type/Status");
        MenuItem mCash = new MenuItem("Cash");
        MenuItem mCheque = new MenuItem("Cheque");
        MenuItem mReceivable = new MenuItem("Receivable");
        menuUpdate.getItems().addAll(mCash, mCheque, mReceivable);
        ContextMenu cm = new ContextMenu();
        cm.getItems().addAll(mShowDetails, mPrint, mExport, mDelete, menuUpdate);
        invoicesTable.setContextMenu(cm);
        
        // setup icons
        btnPrintList.setGraphic(new PrintIcon(14));
        
        disposables.addAll(
                JavaFxObservable.changesOf(cbStatus.valueProperty()).subscribe(status -> {
                    if (status.getNewVal() != null) {
                        if (status.getNewVal().equals("All")) filteredList.setPredicate(p -> true);
                        else filteredList.setPredicate(invoice -> invoice.getPaymentType().equals(status.getNewVal()));
                    }
                }),
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
                    addInvoiceWindow.show();
                }), 
                JavaFxObservable.actionEventsOf(btnEdit).subscribe(evt -> {
                    DeliveryInvoice invoice = invoicesTable.getSelectionModel().getSelectedItem();
                    if (invoice != null) editInvoiceWindow.show(invoice);
                }), 
                JavaFxObservable.actionEventsOf(btnPrintList).subscribe(evt -> {
                    
                }),
                JavaFxObservable.changesOf(toggleShowAll.selectedProperty()).subscribe(selected -> {
                    if (selected.getNewVal()) {
                        lblMonthYear.setText("All");
                        getDeliveryInvoices();
                    }
                }),
                JavaFxObservable.changesOf(toggleFilterDate.selectedProperty()).subscribe(selected -> {
                    boolean isSelected = selected.getNewVal();
                    dateFrom.setDisable(!isSelected);
                    dateTo.setDisable(!isSelected);
                    if (isSelected) {
                        updateMonthYearLabel();
                        filterByDate();
                    }
                }),
                JavaFxObservable.changesOf(dateFrom.valueProperty()).subscribe(value -> {
                    mDateFrom = value.getNewVal();
                    if (mDateFrom.isAfter(mDateTo)) {
                        YearMonth ym = YearMonth.from(mDateFrom);
                        mDateTo = ym.atEndOfMonth();
                        dateTo.setValue(mDateTo);
                    }
                    updateMonthYearLabel();
                    filterByDate();
                }),
                JavaFxObservable.changesOf(dateTo.valueProperty()).subscribe(value -> {
                    mDateTo = value.getNewVal();
                    updateMonthYearLabel();
                    filterByDate();
                }),
                JavaFxObservable.changesOf(tfSearch.textProperty()).subscribe(value -> {
                    String text = value.getNewVal();
                    if (text != null && filteredList != null) {
                        if (text.isEmpty()) filteredList.setPredicate(p -> true);
                        else {
                            filteredList.setPredicate(d -> d.getCustomer().toLowerCase().contains(text.toLowerCase()));
                        }
                    }
                }),
                JavaFxObservable.actionEventsOf(mShowDetails).subscribe(evt -> {
                    if (!splitController.isTargetVisible() && mShowDetails.isSelected()) {
                        splitController.showTarget();
                    } else {
                        splitController.hideTarget();
                    }
                }), 
                JavaFxObservable.actionEventsOf(mPrint).subscribe(evt -> {
                    DeliveryInvoice invoice = invoicesTable.getSelectionModel().getSelectedItem();
                    if (invoice != null) printInvoice(invoice);
                }), 
                JavaFxObservable.actionEventsOf(mExport).subscribe(evt -> {
                    DeliveryInvoice invoice = invoicesTable.getSelectionModel().getSelectedItem();
                    if (invoice != null) exportInvoice(invoice);
                }),
                JavaFxObservable.actionEventsOf(mDelete).subscribe(evt -> {
                    DeliveryInvoice invoice = invoicesTable.getSelectionModel().getSelectedItem();
                    if (invoice != null) {
                        Optional<ButtonType> result = showConfirmDialog("Delete Invoice?",
                                "You are about to delete this Invoice entry permanently. Proceed?");
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            deleteInvoice(invoice);
                        }
                    }
                }),
                JavaFxObservable.actionEventsOf(mCash).subscribe(evt -> {
                    DeliveryInvoice invoice = invoicesTable.getSelectionModel().getSelectedItem();
                    if (invoice != null && !invoice.getPaymentType().equals("Cash")) {
                        updateInvoiceStatus(invoice, "Cash");
                    }
                }),
                JavaFxObservable.actionEventsOf(mCheque).subscribe(evt -> {
                    DeliveryInvoice invoice = invoicesTable.getSelectionModel().getSelectedItem();
                    if (invoice != null && !invoice.getPaymentType().equals("Cheque")) {
                        updateInvoiceStatus(invoice, "Cheque");
                    }
                }),
                JavaFxObservable.actionEventsOf(mReceivable).subscribe(evt -> {
                    DeliveryInvoice invoice = invoicesTable.getSelectionModel().getSelectedItem();
                    if (invoice != null && !invoice.getPaymentType().equals("Receivable")) {
                        updateInvoiceStatus(invoice, "Receivable");
                    }
                }),
                JavaFxObservable.changesOf(invoicesTable.getSelectionModel().selectedItemProperty()).subscribe(item -> {
                    if (item.getNewVal() != null) {
                        mPrint.setDisable(false);
                        mExport.setDisable(false);
                        getInvoiceItems(item.getNewVal());
                    } else {
                        mPrint.setDisable(true);
                        mExport.setDisable(true);
                    }
                }),
                JavaFxObservable.changesOf(mTotal).subscribe(value -> {
                    if (value.getNewVal() != null) lblTotal.setText(Utils.toMoneyFormat(value.getNewVal().doubleValue()));
                }),
                JavaFxObservable.actionEventsOf(btnPrintList).subscribe(evt -> {
                    printInvoiceList();
                })
        );
        
        splitController = new SplitController(splitPane, SplitController.Target.LAST);
//        splitController.hideTarget();
    }

    @Override
    public void onPause() {
        itemsTable.getSelectionModel().clearSelection();
        invoiceItems.clear();
        lblTotal.setText("P 0.00");
    }

    @Override
    public void onResume() {
        LocalDate now = LocalDate.now();
        YearMonth ym = YearMonth.from(now);
        mDateFrom = ym.atDay(1);
        mDateTo = ym.atEndOfMonth();
        dateFrom.setValue(mDateFrom);
        dateTo.setValue(mDateTo);
        updateMonthYearLabel();
        filterByDate();
    }
    
    private void filterByDate() {
        mainWindow.showProgress(true, "Fetching delviery invoices...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getDeliveryInvoices(mDateFrom, mDateTo);
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(invoices -> {
            mainWindow.showProgress(false);
            double totalCash = 0;
            double totalCheque = 0;
            double totalReceivables = 0;
            for (DeliveryInvoice i : invoices) {
                switch (i.getPaymentType()) {
                    case "Cash": totalCash += i.getTotal(); break;
                    case "Cheque": totalCheque += i.getTotal(); break;
                    case "Receivable": totalReceivables += i.getTotal(); break;
                }
            }
            lblCash.setText("P " + Utils.toMoneyFormat(totalCash));
            lblCheque.setText("P " + Utils.toMoneyFormat(totalCheque));
            lblReceivables.setText("P " + Utils.toMoneyFormat(totalReceivables));
            
            filteredList = new FilteredList<>(FXCollections.observableArrayList(invoices));
            invoicesTable.setItems(filteredList);
            cbStatus.setValue("All");
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching invoices.", err);
        }));
    }
    
    private void getDeliveryInvoices() {
        mainWindow.showProgress(true, "Fetching delviery invoices...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getDeliveryInvoices();
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(invoices -> {
            mainWindow.showProgress(false);
            double totalCash = 0;
            double totalCheque = 0;
            double totalReceivables = 0;
            for (DeliveryInvoice i : invoices) {
                switch (i.getPaymentType()) {
                    case "Cash": totalCash += i.getTotal(); break;
                    case "Cheque": totalCheque += i.getTotal(); break;
                    case "Receivable": totalReceivables += i.getTotal(); break;
                }
            }
            lblCash.setText("P " + Utils.toMoneyFormat(totalCash));
            lblCheque.setText("P " + Utils.toMoneyFormat(totalCheque));
            lblReceivables.setText("P " + Utils.toMoneyFormat(totalReceivables));
            
            filteredList = new FilteredList<>(FXCollections.observableArrayList(invoices));
            invoicesTable.setItems(filteredList);
            cbStatus.setValue("All");
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching invoices.", err);
        }));
    }
    
    private void updateMonthYearLabel() {
        if (mDateFrom != null && mDateTo != null && (mDateFrom.isBefore(mDateTo))) {
            String rangeText = String.format("%s - %s", mDateFrom.format(Utils.dateTimeFormat2), 
                    mDateTo.format(Utils.dateTimeFormat2));
            lblMonthYear.setText(rangeText);
        }
    }
    
    private void getInvoiceItems(DeliveryInvoice invoice) {
        mainWindow.showProgress(true, "Fetching invoice items...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getDeliveryInvoiceItems(invoice.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(items -> {
            mainWindow.showProgress(false);
            invoiceItems.setAll(items);
            double total = 0;
            for (DeliveryInvoiceItem item : items) {
                total += item.getTotal();
            }
            mTotal.set(total);
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error while fetching invoice items.", err);
        }));
    }
    
    private void printInvoiceList() {
        mainWindow.showProgress(true, "Preparing invoices for printing...");
        
        ArrayList<PrintInvoiceList> pils = new ArrayList<>();
        int maxPerPage = 47;
        int totalPage = (int) (filteredList.size() / maxPerPage);
        if (totalPage > 0) {
            int startIndex = 0;
            for (int i = 1; i <= totalPage; i++) {
                ArrayList<DeliveryInvoice> items = new ArrayList<>();
                int endIndex = startIndex + maxPerPage - 1;
                if (endIndex < filteredList.size()) {
                    items.addAll(filteredList.subList(startIndex, endIndex));
                } else {
                    items.addAll(filteredList.subList(startIndex, filteredList.size() - 1));
                }
                PrintInvoiceList pil = new PrintInvoiceList();
                pil.set(items, i, totalPage);
                pils.add(pil);
                startIndex = endIndex;
            }
        } else {
            PrintInvoiceList pil = new PrintInvoiceList();
            pil.set(new ArrayList<>(filteredList), 1, 1);
            pils.add(pil);
        }
        
        mainWindow.showProgress(false);
        printWindow.show(pils);
    }
    
    private void printInvoice(DeliveryInvoice invoice) {
        mainWindow.showProgress(true, "Preparing invoice for printing...");

        ArrayList<PrintInvoice> pis = new ArrayList<>();
        int maxPerPage = 30;
        int totalPage = (int) (invoiceItems.size() / maxPerPage);
        if (totalPage > 1) {
            int startIndex = 0;
            for (int i = 1; i <= totalPage; i++) {
                ArrayList<DeliveryInvoiceItem> items = new ArrayList<>();
                int endIndex = startIndex + maxPerPage - 1;
                if (endIndex < invoiceItems.size()) {
                    items.addAll(invoiceItems.subList(startIndex, endIndex));
                } else {
                    items.addAll(invoiceItems.subList(startIndex, invoiceItems.size() - 1));
                }
                PrintInvoice pi = new PrintInvoice();
                pi.set(invoice.getId(), invoice.getDate(), invoice.getCustomer(), invoice.getAddress(), invoice.getTotal(), invoice.getPaymentType(), items, i, totalPage);
                pis.add(pi);
                startIndex = endIndex;
            }
        } else {
            PrintInvoice pi = new PrintInvoice();
            pi.set(invoice.getId(), invoice.getDate(), invoice.getCustomer(), invoice.getAddress(), invoice.getTotal(), invoice.getPaymentType(), new ArrayList<>(invoiceItems), 1, 1);
            pis.add(pi);
        }
        
        mainWindow.showProgress(false);
        printWindow.show(pis);
    }
    
    private void exportInvoice(DeliveryInvoice invoice) {
        File folder = dirChooser.showDialog(mainWindow.getWindow());
        if (folder != null) {
            try {
                XSSFWorkbook workbook = new XSSFWorkbook();
                XSSFSheet sheet = workbook.createSheet("Invoice Details");
                
                // Cell Fonts
                XSSFFont boldFont = workbook.createFont();
                boldFont.setBold(true);
                
                // Cell Styles
                
                // Bold Cell
                XSSFCellStyle boldCell = workbook.createCellStyle();
                boldCell.setFont(boldFont);
                
                // Center Cell
                XSSFCellStyle centerCell = workbook.createCellStyle();
                centerCell.setAlignment(HorizontalAlignment.CENTER);
                
                // Border Cell
                XSSFCellStyle borderCell = workbook.createCellStyle();
                borderCell.setBorderLeft(BorderStyle.THIN);
                borderCell.setBorderBottom(BorderStyle.THIN);
                borderCell.setBorderRight(BorderStyle.THIN);
                borderCell.setBorderTop(BorderStyle.THIN);
                
                // Border/Center Cell
                XSSFCellStyle borderCenterCell = workbook.createCellStyle();
                borderCenterCell.setBorderLeft(BorderStyle.THIN);
                borderCenterCell.setBorderBottom(BorderStyle.THIN);
                borderCenterCell.setBorderRight(BorderStyle.THIN);
                borderCenterCell.setBorderTop(BorderStyle.THIN);
                
                // Border/Bold Cell
                XSSFCellStyle borderBoldCell = workbook.createCellStyle();
                borderBoldCell.setBorderLeft(BorderStyle.THIN);
                borderBoldCell.setBorderBottom(BorderStyle.THIN);
                borderBoldCell.setBorderRight(BorderStyle.THIN);
                borderBoldCell.setBorderTop(BorderStyle.THIN);
                borderBoldCell.setFont(boldFont);
                
                int rowIndex = 0;
                // Invoice Details
                // Invoice No
                XSSFRow noRow = sheet.createRow(rowIndex++);
                XSSFCell noCellTitle = noRow.createCell(0);
                noCellTitle.setCellStyle(boldCell);
                noCellTitle.setCellValue("Invoice No:");
                XSSFCell noCell = noRow.createCell(1);
                noCell.setCellValue(invoice.getId());
                
                // Invoice Date
                XSSFRow dateRow = sheet.createRow(rowIndex++);
                XSSFCell dateCellTitle = dateRow.createCell(0);
                dateCellTitle.setCellStyle(boldCell);
                dateCellTitle.setCellValue("Invoice Date:");
                XSSFCell dateCell = dateRow.createCell(1);
                dateCell.setCellValue(invoice.getDate().format(Utils.dateTimeFormat));
                
                // Customer
                XSSFRow customerRow = sheet.createRow(rowIndex++);
                XSSFCell customerCellTitle = customerRow.createCell(0);
                customerCellTitle.setCellStyle(boldCell);
                customerCellTitle.setCellValue("Customer:");
                XSSFCell customerCell = customerRow.createCell(1);
                customerCell.setCellValue(invoice.getCustomer());
                
                // Address
                XSSFRow addressRow = sheet.createRow(rowIndex++);
                XSSFCell addressCellTitle = addressRow.createCell(0);
                addressCellTitle.setCellStyle(boldCell);
                addressCellTitle.setCellValue("Address:");
                XSSFCell addressCell = addressRow.createCell(1);
                addressCell.setCellValue(invoice.getAddress());
                
                // Total Amount
                XSSFRow totalRow = sheet.createRow(rowIndex++);
                XSSFCell totalCellTitle = totalRow.createCell(0);
                totalCellTitle.setCellStyle(boldCell);
                totalCellTitle.setCellValue("Total:");
                XSSFCell totalCell = totalRow.createCell(1);
                totalCell.setCellValue(String.format("PhP %.2f", invoice.getTotal()));
                
                // Status
                XSSFRow statusRow = sheet.createRow(rowIndex++);
                XSSFCell statusCellTitle = statusRow.createCell(0);
                statusCellTitle.setCellStyle(boldCell);
                statusCellTitle.setCellValue("Status");
                XSSFCell statusCell = statusRow.createCell(1);
                statusCell.setCellValue(invoice.getPaymentType());
                
                rowIndex++; // adding blank row
                
                // Title/Header row
                String[] headers = new String[]{"Item", "Supplier", "Unit", "Unit Price", "Discount", "Discounted Price", "Qty", "Total"};
                XSSFRow headerRow = sheet.createRow(rowIndex++);
                for (int i = 0; i < headers.length; i++) {
                    XSSFCell cell = headerRow.createCell(i, CellType.STRING);
                    cell.setCellStyle(borderBoldCell);
                    cell.setCellValue(headers[i]);
                }
                
                // Order Details
                XSSFRow row;
                int index = 0;
                for (int i = rowIndex; i < invoiceItems.size() + rowIndex; i++) {
                    row = sheet.createRow(i);
                    DeliveryInvoiceItem item = invoiceItems.get(index);
                    Product p = item.getProduct();
                    for (int j = 0; j < headers.length; j++) {
                        XSSFCell cell = row.createCell(j);
                        switch (j) {
                            case 0:
                                cell.setCellType(CellType.STRING);
                                cell.setCellValue(p.getName());
                                cell.setCellStyle(borderCell);
                                break;
                            case 1:
                                cell.setCellType(CellType.STRING);
                                cell.setCellValue(p.getSupplier());
                                cell.setCellStyle(borderCell);
                                break;
                            case 2:
                                cell.setCellType(CellType.STRING);
                                cell.setCellValue(p.getUnit());
                                cell.setCellStyle(borderCenterCell);
                                break;
                            case 3:
                                cell.setCellType(CellType.NUMERIC);
                                cell.setCellValue(p.getUnitPrice());
                                cell.setCellStyle(borderCell);
                                break;
                            case 4:
                                cell.setCellType(CellType.NUMERIC);
                                cell.setCellValue(item.getDiscount() * 100 + "%");
                                cell.setCellStyle(borderCell);
                                break;
                            case 5:
                                cell.setCellType(CellType.NUMERIC);
                                cell.setCellValue(item.getDiscountedPrice());
                                cell.setCellStyle(borderCell);
                                break;
                            case 6:
                                cell.setCellValue(item.getQuantity());
                                cell.setCellStyle(borderCenterCell);
                                break;
                            case 7:
                                cell.setCellType(CellType.NUMERIC);
                                cell.setCellValue(item.getTotal());
                                cell.setCellStyle(borderCell);
                                break;
                        }
                    }
                    index++;
                }
                
                // Autosize all column
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }
                
                // Save
                File file = new File(folder.getAbsolutePath() + Utils.fileSeparator() +
                        String.format("invoice_%s_%s.xlsx", invoice.getId(), LocalDate.now().format(Utils.fileDateFormat)));
                try (FileOutputStream out = new FileOutputStream(file)) {
                    workbook.write(out);
                }
                
                // Open File
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                showErrorDialog("Error", "Error occurred while exporting invoice information.", e);
            }
        }
    }
    
    private void deleteInvoice(DeliveryInvoice invoice) {
        mainWindow.showProgress(true, "Deleting invoice entry...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().deleteEntry("invoices", "id", invoice.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            mainWindow.showProgress(false);
            if (!success) {
                showInfoDialog("Failed", "Failed to delete invoice entry.");
            }
            onResume();
            splitController.hideTarget();
            invoiceItems.clear();
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while deleting invoice entry.", err);
        }));
    }
    
    private void updateInvoiceStatus(DeliveryInvoice invoice, String status) {
        mainWindow.showProgress(true, "Updating invoice status...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().updateEntry("delivery_invoices", "payment_type", status, "id", invoice.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            mainWindow.showProgress(false);
            if (!success) {
                showInfoDialog("Failed", "Failed to update invoice entry.");
            }
            onResume();
            splitController.hideTarget();
            invoiceItems.clear();
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while updating invoice entry.", err);
        }));
    }

    @Override
    public void onDispose() {
        disposables.dispose();
        addInvoiceWindow.onDispose();
        printWindow.onDispose();
    }

}
