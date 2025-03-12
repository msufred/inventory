package io.zak.inventory;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Utils {

    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);

    /**
     * Returns a database-safe String. MUST BE USED when saving entry to or querying the database.
     * @param str String value
     * @return String safe for database
     */
    public static String normalize(String str) {
        return str.replace("'", "`");
    }

    /**
     * Convert double value to String money format. Example, 1000.00 is converted to 10,000.00.
     * @param amount amount to be converted to a String money format
     * @return String money format
     */
    public static String toStringMoneyFormat(double amount) {
        String str = String.format(Locale.getDefault(), "%.2f", Math.abs(amount));
        StringBuilder sb = new StringBuilder();
        int startIndex = str.indexOf('.');
        if (startIndex < 0) startIndex = 0;
        int decimalCount = 0;
        for (int i = startIndex - 1; i >= 0; i--) {
            if (decimalCount == 3) {
                sb.append(',');
                decimalCount = 0;
            }
            sb.append(str.charAt(i));
            decimalCount++;
        }
        String output = "";
        if (amount < 0) {
            output = "-";
        }
        output += sb.reverse().append(str.substring(startIndex)).toString();
        // if output is like `-,500.00`, remove first two characters
        if (output.charAt(0) == '-' && output.charAt(1) == ',') {
            output = output.substring(2);
        }
        return output;
    }

    /**
     * Return a human readable date. Ex. Today, Yesterday, 2 days ago etc.
     * @param date Date
     * @return String human readable date
     */
    public static String humanizeDate(Date date) {
        Calendar now = Calendar.getInstance();
        int currDay = now.get(Calendar.DAY_OF_MONTH);
        int currMonth = now.get(Calendar.MONTH);
        int currYear = now.get(Calendar.YEAR);

        Calendar calDate = Calendar.getInstance();
        calDate.setTime(date);
        int day = calDate.get(Calendar.DAY_OF_MONTH);
        int month = calDate.get(Calendar.MONTH);
        int year = calDate.get(Calendar.YEAR);


        if (day == currDay && month == currMonth && year == currYear) {
            return "Today";
        }

        int diffDays = currDay - day;

        if (diffDays == 1) return "Yesterday";
        if (diffDays > 1 && diffDays <= 5) {
            return day + " days ago";
        } else {
            return dateFormat.format(date);
        }
    }

    /**
     * Generate a Bitmap QR Code from String str
     * @param str String used to generate QR Code
     * @param width Bitmap width
     * @param height Bitmap height
     * @return Bitmap
     */
    public static Bitmap generateQrCode(String str, int width, int height) {
        Bitmap bitmap = null;
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            BitMatrix matrix = qrCodeWriter.encode(str, BarcodeFormat.QR_CODE, width, height);

            int w = matrix.getWidth();
            int h = matrix.getHeight();
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    pixels[y * w + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }

            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        } catch (WriterException e) {
            Log.e("Utils", "Failed to write QR Code: " + e);
        }
        return bitmap;
    }

    public static boolean isTheSameDate(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return (cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)) &&
                (cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)) &&
                (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR));
    }

    /**
     * Save User id to SharedPreferences.
     * @param context Context
     * @param id User id
     */
    public static void saveLoginId(Context context, int id) {
        SharedPreferences sp = context.getSharedPreferences("login", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("user_id", id);
        editor.apply();
    }

    /**
     * Retrieve saved User id in SharedPreferences. Returns -1 if no User is logged in.
     * @param context Context
     * @return User id or -1 of no User logged in
     */
    public static int getLoginId(Context context) {
        SharedPreferences sp = context.getSharedPreferences("login", Context.MODE_PRIVATE);
        return sp.getInt("user_id", -1);
    }

    /**
     * Logout User by removing user id in SharedPreferences.
     * @param context Context
     */
    public static void logout(Context context) {
        SharedPreferences sp = context.getSharedPreferences("login", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove("user_id");
        editor.apply();
    }

}
