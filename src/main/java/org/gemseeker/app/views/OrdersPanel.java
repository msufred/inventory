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
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.EmbeddedDatabase;
import org.gemseeker.app.data.Order;
import org.gemseeker.app.data.OrderItem;
import org.gemseeker.app.data.Product;
import org.gemseeker.app.views.frameworks.AbstractPanelController;
import org.gemseeker.app.views.frameworks.SplitController;
import org.gemseeker.app.views.tablecells.DateTableCell;
import org.gemseeker.app.views.tablecells.DiscountTableCell;
import org.gemseeker.app.views.tablecells.PriceTableCell;
import org.gemseeker.app.views.tablecells.ProductNameTableCell;
import org.gemseeker.app.views.tablecells.ProductPriceTableCell;
import org.gemseeker.app.views.tablecells.ProductSupplierTableCell;
import org.gemseeker.app.views.tablecells.ProductUnitTableCell;

/**
 *
 * @author Gem
 */
public class OrdersPanel extends AbstractPanelController {
    
    @FXML private Button btnAdd;
    @FXML private Button btnExport;
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, LocalDate> colOrderDate;
    @FXML private TableColumn<Order, Double> colTotal;
    @FXML private TableColumn<Order, String> colOrderName;
    @FXML private TableView<OrderItem> orderItemsTable;
    @FXML private TableColumn<OrderItem, Product> colItemName;
    @FXML private TableColumn<OrderItem, Product> colItemSupplier;
    @FXML private TableColumn<OrderItem, Product> colItemUnit; 
    @FXML private TableColumn<OrderItem, Product> colItemPriceBefore; 
    @FXML private TableColumn<OrderItem, Double> colItemDiscount; 
    @FXML private TableColumn<OrderItem, Double> colItemPriceAfter; 
    @FXML private TableColumn<OrderItem, Integer> colItemQuantity; 
    @FXML private TableColumn<OrderItem, Double> colItemTotal; 
    @FXML private TableColumn<OrderItem, Integer> colItemQuantityOut; 
    @FXML private TableColumn<OrderItem, Double> colItemTotalOut; 
    @FXML private Label lblOrderTotal;
    @FXML private Label lblTotalOut;
    @FXML private SplitPane splitPane;
    private SplitController splitController;
    
    private final MainWindow mainWindow;
    private final CompositeDisposable disposables;
    
    private final ObservableList<OrderItem> orderItems = FXCollections.observableArrayList();
    private final SimpleDoubleProperty mOrderTotal = new SimpleDoubleProperty(0);
    private final SimpleDoubleProperty mTotalOut = new SimpleDoubleProperty(0);
    
    private FilteredList<Order> filteredList;
    
    private final AddOrderWindow addOrderWindow;
    
    private final DirectoryChooser dirChooser;
    
    public OrdersPanel(MainWindow mainWindow) {
        super(InventoryPanel.class.getResource("orders.fxml"));
        this.mainWindow = mainWindow;
        disposables = new CompositeDisposable();
        
        addOrderWindow = new AddOrderWindow(this, mainWindow.getWindow());
        
        dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Destination");
    }

    @Override
    public void onLoad() {
        // Order Table Columns
        colOrderDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colOrderDate.setCellFactory(col -> new DateTableCell<>());
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> new PriceTableCell<>());
        colOrderName.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        // OrderItems Table Columns
        colItemName.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemName.setCellFactory(col -> new ProductNameTableCell<>());
        colItemSupplier.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemSupplier.setCellFactory(col -> new ProductSupplierTableCell<>());
        colItemUnit.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemUnit.setCellFactory(col -> new ProductUnitTableCell<>());
        colItemPriceBefore.setCellValueFactory(new PropertyValueFactory<>("product"));
        colItemPriceBefore.setCellFactory(col -> new ProductPriceTableCell<>());
        colItemDiscount.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colItemDiscount.setCellFactory(col -> new DiscountTableCell<>());
        colItemPriceAfter.setCellValueFactory(new PropertyValueFactory<>("discountedPrice"));
        colItemPriceAfter.setCellFactory(col -> new PriceTableCell<>());
        colItemQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colItemTotal.setCellValueFactory(new PropertyValueFactory<>("listPrice"));
        colItemTotal.setCellFactory(col -> new PriceTableCell<>());
        colItemQuantityOut.setCellValueFactory(new PropertyValueFactory<>("quantityOut"));
        colItemTotalOut.setCellValueFactory(new PropertyValueFactory<>("totalOut"));
        colItemTotalOut.setCellFactory(col -> new PriceTableCell<>());
        
        orderItemsTable.setItems(orderItems);
        
        disposables.addAll(JavaFxObservable.changesOf(mOrderTotal).subscribe(value -> {
                    if (value.getNewVal() != null) {
                        lblOrderTotal.setText(Utils.getMoneyFormat(value.getNewVal().doubleValue()));
                    }
                }),
                JavaFxObservable.changesOf(mTotalOut).subscribe(value -> {
                    if (value.getNewVal() != null) {
                        lblTotalOut.setText(Utils.getMoneyFormat(value.getNewVal().doubleValue()));
                    }
                }),
                JavaFxObservable.changesOf(ordersTable.getSelectionModel().selectedItemProperty()).subscribe(order -> {
                    if (order.getNewVal() != null) {
                        btnExport.setDisable(false);
                        if (!splitController.isTargetVisible()) {
                            splitController.showTarget();
                        }
                        getOrderItems(order.getNewVal());
                    } else {
                        btnExport.setDisable(true);
                        splitController.hideTarget();
                    }
                }),
                JavaFxObservable.actionEventsOf(btnAdd).subscribe(evt -> {
                    addOrderWindow.show();
                }),
                JavaFxObservable.actionEventsOf(btnExport).subscribe(evt -> {
                    Order order = ordersTable.getSelectionModel().getSelectedItem();
                    if (order != null) {
                        exportOrder(order);
                    }
                })
        );
        
        splitController = new SplitController(splitPane, SplitController.Target.LAST);
        splitController.hideTarget();
    }
    
    @Override
    public void onPause() {
        ordersTable.getSelectionModel().clearSelection();
        splitController.hideTarget();
        btnExport.setDisable(true);
    }

    @Override
    public void onResume() {
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

    private void getOrderItems(Order order) {
        mainWindow.showProgress(true, "Fetching order items..");
        disposables.add(Single.fromCallable(() -> {
            return EmbeddedDatabase.getInstance().getOrderItems(order.getId());
        }).subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform()).subscribe(items -> {
            mainWindow.showProgress(false);
            orderItems.setAll(items);
            
            double total = 0;
            double totalOut = 0;
            for (OrderItem item : orderItems) {
                total += item.getListPrice();
                totalOut += item.getTotalOut();
            }
            mOrderTotal.set(total);
            mTotalOut.set(totalOut);
        }, err -> {
            mainWindow.showProgress(false);
            showErrorDialog("Database Error", "Error occurred while fetching order items.", err);
        }));
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
                customerCell.setCellValue(order.getName());
                
                XSSFRow totalRow = sheet.createRow(2);
                XSSFCell totalCellTitle = totalRow.createCell(0);
                totalCellTitle.setCellStyle(boldCell);
                totalCellTitle.setCellValue("Total:");
                XSSFCell totalCell = totalRow.createCell(1);
                totalCell.setCellValue(String.format("PhP %.2f", order.getTotal()));
                
                // Title/Header row
                String[] headers = new String[]{"Item", "Supplier", "Unit", "Unit Price", "Discount", "Discounted Price", 
                "Qty", "Total", "Qty. Out", "Total Out"};
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
                                cell.setCellValue(item.getListPrice());
                                cell.setCellStyle(borderCell);
                                break;
                            case 8:
                                cell.setCellValue(item.getQuantityOut());
                                cell.setCellStyle(borderCenterCell);
                                break;
                            case 9:
                                cell.setCellType(CellType.NUMERIC);
                                cell.setCellValue(item.getTotalOut());
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
                File file = new File(folder.getAbsolutePath() + Utils.getSeparator() +
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
    
    @Override
    public void onDispose() {
        disposables.dispose();
        addOrderWindow.onDispose();
    }

}
