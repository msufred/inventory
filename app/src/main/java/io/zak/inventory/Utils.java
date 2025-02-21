package io.zak.inventory;

import android.content.Context;
import android.content.SharedPreferences;

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

}
