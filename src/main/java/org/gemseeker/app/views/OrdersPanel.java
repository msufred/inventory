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
import javafx.beans.property.SimpleIntegerProperty;
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
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
import org.gemseeker.app.data.Order;
import org.gemseeker.app.data.OrderItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.Shipper;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.frameworks.SplitController;
import org.gemseeker.app.views.icons.PrintIcon;
import org.gemseeker.app.views.prints.PrintOrder;
import org.gemseeker.app.views.prints.PrintWindow;
import org.gemseeker.app.views.tablecells.DateTableCell;
import org.gemseeker.app.views.tablecells.PriceTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductRetailPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSupplierTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 * WITHDRAWAL PANEL
 *
 * @author Gem
 */
public class OrdersPanel extends AbstractPanelController {
    
    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnPrintList;
    
    @FXML private RadioButton toggleShowAll;
    @FXML private RadioButton toggleFilterDate;
    @FXML private DatePicker dateFrom;
    @FXML private DatePicker dateTo;
    @FXML private ComboBox<Shipper> cbTrucks;
    
    // Orders Table
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, LocalDate> colDate;
    @FXML private TableColumn<Order, Shipper> colShipperName;
    @FXML private TableColumn<Order, Double> colTotal;
    
    // Order Items Table
    @FXML private TableView<OrderItem> orderItemsTable;
    @FXML private TableColumn<OrderItem, Product> colItemName;
    @FXML private TableColumn<OrderItem, Product> colItemSupplier;
    @FXML private TableColumn<OrderItem, Product> colItemUnit;
    @FXML private TableColumn<OrderItem, Product> colItemUnitPrice;
    @FXML private TableColumn<OrderItem, Integer> colItemQuantity;
    @FXML private TableColumn<OrderItem, Double> colItemTotal;
    @FXML private Label lblTotal;

    @FXML private SplitPane splitPane;
    private SplitController splitController;
    
    private final MainWindow mainWindow;
    private final CompositeDisposable disposables;
    
    private final ObservableList<OrderItem> orderItems = FXCollections.observableArrayList();
    private final SimpleIntegerProperty selectedOrderIndex = new SimpleIntegerProperty();
    private final SimpleIntegerProperty selectedOrderItemIndex = new SimpleIntegerProperty();
    
    private FilteredList<Order> filteredList;
    
    private final AddOrderWindow addOrderWindow;
    private final EditOrderWindow editOrderWindow;
    private final PrintWindow printWindow;
    
    private final DirectoryChooser dirChooser;
    
    private LocalDate mDateFrom;
    private LocalDate mDateTo;
    
    public OrdersPanel(MainWindow mainWindow) {
        super(PurchasesPanel.class.getResource("orders.fxml"));
        this.mainWindow = mainWindow;
        disposables = new CompositeDisposable();
        
        addOrderWindow = new AddOrderWindow(this, mainWindow.getWindow());
        editOrderWindow = new EditOrderWindow(this, mainWindow.getWindow());
        printWindow = new PrintWindow(mainWindow.getWindow());
        
        dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Destination");
    }

    @Override
    public void onLoad() {
        // Order Table Columns
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDate.setCellFactory(col -> new DateTableCell<>());
        colShipperName.setCellValueFactory(new PropertyValueFactory<>("shipper"));
        colShipperName.setCellFactory(col -> new TableCell<Order, Shipper>() {
            @Override
            protected void updateItem(Shipper item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item.getName());
                } else setText("");
            }
        });
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> new PriceTableCell<>());
        
        // OrderItems Table Columns
        colItemName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemName.setCellFactory(col -> new ProductNameTableCell<>());
        colItemSupplier.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemSupplier.setCellFactory(col -> new ProductSupplierTableCell<>());
        colItemUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colItemUnitPrice.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemUnitPrice.setCellFactory(col -> new ProductRetailPriceTableCell<>());
        colItemQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colItemTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colItemTotal.setCellFactory(col -> new PriceTableCell<>());
        
        orderItemsTable.setItems(orderItems);
        
        selectedOrderIndex.bind(ordersTable.getSelectionModel().selectedIndexProperty());
        selectedOrderItemIndex.bind(orderItemsTable.getSelectionModel().selectedIndexProperty());
        
        CheckMenuItem mShowDetails = new CheckMenuItem("Show Details");
        mShowDetails.setSelected(true);
        MenuItem mPrint = new MenuItem("Print");
        MenuItem mEdit = new MenuItem("Edit");
        MenuItem mExport = new MenuItem("Export");
        MenuItem mDelete = new MenuItem("Delete");
        ContextMenu cm = new ContextMenu();
        cm.getItems().addAll(mShowDetails, mPrint, mEdit, mExport, mDelete);
        ordersTable.setContextMenu(cm);
        
        // setup icons
        btnPrintList.setGraphic(new PrintIcon(14));
        
        disposables.addAll(
                JavaFxObservable.changesOf(selectedOrderIndex).subscribe(index -> {
                    int selectedIndex = index.getNewVal().intValue();
                    btnEdit.setDisable(selectedIndex == -1);
                    if (selectedIndex > -1) refreshSelectedOrder();
                }),
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
                    addOrderWindow.show();
                }),
                JavaFxObservable.actionEventsOf(btnEdit).subscribe(evt -> {
                    Order selected = ordersTable.getSelectionModel().getSelectedItem();
                    if (selected != null) editOrderWindow.show(selected);
                }),
                JavaFxObservable.actionEventsOf(btnPrintList).subscribe(evt -> {
                    // TODO
                }),
                JavaFxObservable.changesOf(toggleShowAll.selectedProperty()).subscribe(selected -> {
                    boolean isSelected = selected.getNewVal();
                    if (isSelected) {
                        getOrders();
                    }
                }),
                JavaFxObservable.changesOf(toggleFilterDate.selectedProperty()).subscribe(selected -> {
                    boolean isSelected = selected.getNewVal();
                    dateFrom.setDisable(!isSelected);
                    dateTo.setDisable(!isSelected);
                    if (isSelected) {
                        getOrders(mDateFrom, mDateTo);
                    }
                }),
                JavaFxObservable.changesOf(dateFrom.valueProperty()).subscribe(value -> {
                    if (value.getNewVal() != null) {
                        mDateFrom = value.getNewVal();
                        if (mDateFrom.isAfter(mDateTo)) {
                            YearMonth ym = YearMonth.from(mDateFrom);
                            mDateTo = ym.atEndOfMonth();
                            dateTo.setValue(mDateTo);
                        }
                        getOrders(mDateFrom, mDateTo);
                    }
                }),
                JavaFxObservable.changesOf(dateTo.valueProperty()).subscribe(value -> {
                    if (value.getNewVal() != null) {
                        mDateTo = value.getNewVal();
                        getOrders(mDateFrom, mDateTo);
                    }
                }),
                JavaFxObservable.changesOf(cbTrucks.valueProperty()).subscribe(truck -> {
                    Shipper t = truck.getNewVal();
                    if (t != null && filteredList != null) {
                        orderItems.clear();
                        if (t.getName().equals("All")) filteredList.setPredicate(o -> true);
                        else filteredList.setPredicate(o -> o.getShipperId() == t.getId());
                    }
                }),
                JavaFxObservable.actionEventsOf(mShowDetails).subscribe(evt -> {
                    if (!splitController.isTargetVisible() && mShowDetails.isSelected()) splitController.showTarget();
                    else splitController.hideTarget();
                }),
                JavaFxObservable.actionEventsOf(mPrint).subscribe(evt -> {
                    Order order = ordersTable.getSelectionModel().getSelectedItem();
                    if (order != null) {
                        printOrder(order);
                    }
                }),
                JavaFxObservable.actionEventsOf(mEdit).subscribe(evt -> {
                    Order selected = ordersTable.getSelectionModel().getSelectedItem();
                    if (selected != null) editOrderWindow.show(selected);
                }),
                JavaFxObservable.actionEventsOf(mExport).subscribe(evt -> {
                    Order order = ordersTable.getSelectionModel().getSelectedItem();
                    if (order != null) {
                        exportOrder(order);
                    }
                }),
                JavaFxObservable.actionEventsOf(mDelete).subscribe(evt -> {
                    Order order = ordersTable.getSelectionModel().getSelectedItem();
                    if (order != null) {
                        Optional<ButtonType> result = showConfirmDialog("Delete Order?",
                                "You are about to delete this Order entry permanently. Proceed?");
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            deleteOrder(order);
                        }
                    }
                }),
                JavaFxObservable.actionEventsOf(btnPrintList).subscribe(evt -> {
                    showInfoDialog("Invalid Action", "This feature is not implemented yet.");
                })
        );
        
        splitController = new SplitController(splitPane, SplitController.Target.LAST);
//        splitController.hideTarget();
    }
    
    @Override
    public void onPause() {
        ordersTable.getSelectionModel().clearSelection();
        orderItems.clear();
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
        
        mainWindow.showProgress(true, "Fetching truckers...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getShippers();
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(shippers -> {
            mainWindow.showProgress(false);
            Shipper all = new Shipper();
            all.setName("All");
            shippers.add(0, all);
            cbTrucks.setItems(FXCollections.observableArrayList(shippers));
            getOrders(mDateFrom, mDateTo);
            cbTrucks.setValue(all);
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error while retrieving truckers...", err);
        }));
    }
    
    private void getOrders() {
        mainWindow.showProgress(true, "Fetching orders...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getOrders();
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(orders -> {
            mainWindow.showProgress(false);
            filteredList = new FilteredList<>(FXCollections.observableArrayList(orders), o -> true);
            ordersTable.setItems(filteredList);
            ordersTable.getSelectionModel().clearSelection();
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching orders data.", err);
        }));
    }
    
    private void getOrders(LocalDate dFrom, LocalDate dTo) {
        mainWindow.showProgress(true, "Fetching orders...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getOrders(dFrom, dTo);
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(orders -> {
            mainWindow.showProgress(false);
            filteredList = new FilteredList<>(FXCollections.observableArrayList(orders), o -> true);
            ordersTable.setItems(filteredList);
            ordersTable.getSelectionModel().clearSelection();
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching orders data.", err);
        }));
    }
    
    private void refreshSelectedOrder() {
        Order order = ordersTable.getSelectionModel().getSelectedItem();
        if (order != null) getOrderItems(order);
    }

    private void getOrderItems(Order order) {
        mainWindow.showProgress(true, "Fetching order items..");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getOrderItems(order.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(items -> {
            mainWindow.showProgress(false);
            orderItems.setAll(items);
            lblTotal.setText("P " + Utils.toMoneyFormat(order.getTotal()));
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching order items.", err);
        }));
    }
    
    private void printOrder(Order order) {
        mainWindow.showProgress(true, "Preparing orders for printing...");
        ArrayList<PrintOrder> pos = new ArrayList<>();
        int maxPerPage = 30;
        int totalPage = (int) (orderItems.size() / maxPerPage);
        if (totalPage > 1) {
            int startIndex = 0;
            for (int i = 1; i <= totalPage; i++) {
                ArrayList<OrderItem> items = new ArrayList<>();
                int endIndex = startIndex + maxPerPage - 1;
                if (endIndex < orderItems.size()) {
                    items.addAll(orderItems.subList(startIndex, endIndex));
                } else {
                    items.addAll(orderItems.subList(startIndex, orderItems.size() - 1));
                }
                PrintOrder po = new PrintOrder();
                po.set(order.getDate(), order.getShipper().getName(), order.getTotal(), items, i, totalPage);
                pos.add(po);
                startIndex = endIndex;
            }
        } else {
            PrintOrder po = new PrintOrder();
            po.set(order.getDate(), order.getShipper().getName(), order.getTotal(),
                    new ArrayList<>(orderItems), 1, 1);
            pos.add(po);
        }
        
        mainWindow.showProgress(false);
        printWindow.show(pos);
    }
    
    private void exportOrder(Order order) {
        File folder = dirChooser.showDialog(mainWindow.getWindow());
        if (folder != null) {
            try {
                XSSFWorkbook workbook = new XSSFWorkbook();
                XSSFSheet sheet = workbook.createSheet("Order Details");
                
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
                
                // Order Details
                XSSFRow dateRow = sheet.createRow(0);
                XSSFCell dateCellTitle = dateRow.createCell(0);
                dateCellTitle.setCellStyle(boldCell);
                dateCellTitle.setCellValue("Order Date:");
                XSSFCell dateCell = dateRow.createCell(1);
                dateCell.setCellValue(order.getDate().format(Utils.dateTimeFormat));
                
                XSSFRow customerRow = sheet.createRow(1);
                XSSFCell customerCellTitle = customerRow.createCell(0);
                customerCellTitle.setCellStyle(boldCell);
                customerCellTitle.setCellValue("Customer:");
                XSSFCell customerCell = customerRow.createCell(1);
                customerCell.setCellValue(order.getShipper().getName());
                
                XSSFRow totalRow = sheet.createRow(2);
                XSSFCell totalCellTitle = totalRow.createCell(0);
                totalCellTitle.setCellStyle(boldCell);
                totalCellTitle.setCellValue("Total:");
                XSSFCell totalCell = totalRow.createCell(1);
                totalCell.setCellValue(String.format("PhP %.2f", order.getTotal()));
                
                // Title/Header row
                String[] headers = new String[]{"Item", "Supplier", "Unit", "Unit Price", "Qty", "Total"};
                XSSFRow headerRow = sheet.createRow(4);
                for (int i = 0; i < headers.length; i++) {
                    XSSFCell cell = headerRow.createCell(i, CellType.STRING);
                    cell.setCellStyle(borderBoldCell);
                    cell.setCellValue(headers[i]);
                }
                
                // Order Details
                XSSFRow row;
                for (int i = 5; i < orderItems.size() + 5; i++) {
                    row = sheet.createRow(i);
                    OrderItem item = orderItems.get(i-5);
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
                                cell.setCellValue(item.getQuantity());
                                cell.setCellStyle(borderCenterCell);
                                break;
                            case 5:
                                cell.setCellType(CellType.NUMERIC);
                                cell.setCellValue(item.getTotal());
                                cell.setCellStyle(borderCell);
                                break;
                        }
                    }
                }
                
                // Autosize all column
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }
                
                // Save
                File file = new File(folder.getAbsolutePath() + Utils.fileSeparator() +
                        String.format("order_%d_%s.xlsx", order.getId(), LocalDate.now().format(Utils.fileDateFormat)));
                try (FileOutputStream out = new FileOutputStream(file)) {
                    workbook.write(out);
                }
                
                // Open File
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                showErrorDialog("Error", "Error occurred while exporting order information.", e);
            }
        }
    }
    
    private void deleteOrder(Order order) {
        mainWindow.showProgress(true, "Deleting order entry...");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().deleteEntry("orders", "id", order.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(success -> {
            mainWindow.showProgress(false);
            if (!success) {
                showInfoDialog("Failed", "Failed to delete order entry.");
            }
            onResume();
            splitController.hideTarget();
            orderItems.clear();
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while deleting order entry.", err);
        }));
    }
    
    @Override
    public void onDispose() {
        disposables.dispose();
        addOrderWindow.onDispose();
        printWindow.onDispose();
    }

}
