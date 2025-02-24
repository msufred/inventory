package io.zak.inventory;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public class Utils {

    /**
     * Returns a database-safe String. MUST BE USED when saving entry to the database.
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
