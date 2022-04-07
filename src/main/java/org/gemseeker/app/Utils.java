package org.gemseeker.app;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;

public final class Utils {

    public static final boolean DEBUG = true; // TODO set to false on deployment

    public static final String APP_NAME = "Inventory";
    public static final String VERSION = "0.0.1-beta";
    public static final String DB_NAME = "inventorydb";

    public static final String APP_FOLDER = ".inventory";
    public static final String LOG_FOLDER = "logs";
    public static final String DATA_FOLDER = "data";
    public static final String IMAGES_FOLDER = "images";

    public static final String TABLE_DATE_FORMAT_STR = "MMM dd, yyyy";
    
    public static final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    public static final DateTimeFormatter dateTimeFormat2 = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    public static final DateTimeFormatter fileDateFormat = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    public static String getSeparator() {
        return System.getProperty("file.separator");
    }

    public static String getUserHome() {
        return System.getProperty("user.home");
    }

    public static String getAppFolder() {
        return getUserHome() + getSeparator() + APP_FOLDER;
    }

    public static String getLogFolder() {
        return getAppFolder() + getSeparator() + LOG_FOLDER;
    }

    public static String getImagesFolder() {
        return getAppFolder() + getSeparator() + IMAGES_FOLDER;
    }

    public static String getTempFolder() {
        return getAppFolder() + getSeparator() + "temp";
    }

    public static String getDataFolder() {
        return getAppFolder() + getSeparator() + DATA_FOLDER;
    }

    public static String getDatabasePath() {
        return getDataFolder() + getSeparator() + DB_NAME;
    }

    public static String getMoneyFormat(double value) {
        String str = String.format("%.2f", value);
        StringBuilder sb = new StringBuilder();
        int startIndex = str.indexOf('.');
        if (startIndex < 0) startIndex = 0;
        int decCount = 0;
        for (int i=startIndex-1; i>=0; i--) {
            if (decCount == 3) {
                sb.append(',');
                decCount = 0;
            }
            sb.append(str.charAt(i));
            decCount++;
        }
        return sb.reverse().append(str.substring(startIndex)).toString();
    }
    
    public static String getMoneyFormat(int value) {
        String str = String.format("%d", value);
        StringBuilder sb = new StringBuilder();
        int decCount = 0;
        for (int i=str.length()-1; i>=0; i--) {
            if (decCount == 3) {
                sb.append(',');
                decCount = 0;
            }
            sb.append(str.charAt(i));
            decCount++;
        }
        return sb.reverse().toString();
    }

    public static Image getImage(URL imageUrl) throws IOException {
        BufferedImage bi = ImageIO.read(imageUrl);
        return SwingFXUtils.toFXImage(bi, null);
    }

    public static void setAsNumericalTextField(TextField textField) {
        if (textField != null) {
            textField.addEventFilter(KeyEvent.KEY_TYPED, evt -> {
                if (!"-0123456789.".contains(evt.getCharacter())) evt.consume();
            });
        }
    }

    public static void setAsIntegerTextField(TextField textField) {
        if (textField != null) {
            textField.addEventFilter(KeyEvent.KEY_TYPED, evt -> {
                if (!"0123456789".contains(evt.getCharacter())) evt.consume();
            });
        }
    }

    public static void setAsPhoneTextField(TextField textField) {
        if (textField != null) {
            textField.addEventFilter(KeyEvent.KEY_TYPED, evt -> {
                if (!"-0123456789".contains(evt.getCharacter())) evt.consume();
            });
        }
    }
}
