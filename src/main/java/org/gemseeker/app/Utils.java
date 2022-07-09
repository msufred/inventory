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

    public static final String APP_FOLDER = getUserHome() + fileSeparator() + ".inventory";
    public static final String LOG_FOLDER = APP_FOLDER + fileSeparator() + "logs";
    public static final String DATA_FOLDER = APP_FOLDER + fileSeparator() + "data";
    public static final String IMAGES_FOLDER = APP_FOLDER + fileSeparator() + "images";
    public static final String TEMP_FOLDER = APP_FOLDER + fileSeparator() + "temp";
    
    public static final String DATABASE_PATH = DATA_FOLDER + fileSeparator() + DB_NAME;
    public static final String SETTINGS_FILE = APP_FOLDER + fileSeparator() + "settings.xml";

    public static final String TABLE_DATE_FORMAT_STR = "MMM dd, yyyy";
    
    /**
     * Date time format with a pattern MMMM dd, yyyy (ex. July 01, 2022)
     */
    public static final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    
    /**
     * Date time format with a patter MMM dd, yyyy (ex. Jul 01, 2022)
     */
    public static final DateTimeFormatter dateTimeFormat2 = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    
    /**
     * Date time format with a pattern MM-dd-yyyy (ex. 07-01-2022)
     */
    public static final DateTimeFormatter fileDateFormat = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    
    public static String toMoneyFormat(double value) {
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
    
    public static String toMoneyFormat(int value) {
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
    
    public static void setSafeTextField(TextField textField) {
        if (textField != null) {
            textField.addEventFilter(KeyEvent.KEY_TYPED, evt -> {
                String unsafe = "{}|\\^`~[]\'?!";
                if (unsafe.contains(evt.getCharacter())) evt.consume();
            });
        }
    }

    public static void setAsNumericalTextField(TextField...textFields) {
        for (TextField tf : textFields) {
            tf.addEventFilter(KeyEvent.KEY_TYPED, evt -> {
                if (!"-0123456789.".contains(evt.getCharacter())) evt.consume();
                String text = tf.getText();
                String chr = evt.getCharacter();
                if ((chr.equals("-") || chr.equals(".")) && (text.contains("-") || text.contains("."))) {
                    evt.consume();
                }
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
    
    public static int monthIntegerValue(String month) {
        switch (month.toUpperCase()) {
            case "JANUARY": return 1;
            case "FEBRUARY": return 2;
            case "MARCH": return 3;
            case "APRIL": return 4;
            case "MAY": return 5;
            case "JUNE": return 6;
            case "JULY": return 7;
            case "AUGUST": return 8;
            case "SEPTEMBER": return 9;
            case "OCTOBER": return 10;
            case "NOVEMBER": return 11;
            case "DECEMBER": return 12;
            default: return -1;
        }
    }
    
    public static String monthStringValue(int month) {
        if (month < 0 || month > 12) throw new RuntimeException("Invalid month value.");
        
        switch (month) {
            case 1: return "January";
            case 2: return "February";
            case 3: return "March";
            case 4: return "April";
            case 5: return "May";
            case 6: return "June";
            case 7: return "July";
            case 8: return "August";
            case 9: return "September";
            case 10: return "October";
            case 11: return "November";
            case 12: return "December";
            default: return "Invalid";
        }
    }
    
    public static String fileSeparator() {
        return System.getProperty("file.separator");
    }

    public static String getUserHome() {
        return System.getProperty("user.home");
    }
}
