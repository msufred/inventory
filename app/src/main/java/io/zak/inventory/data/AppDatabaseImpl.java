package io.zak.inventory.data;

import android.content.Context;

import androidx.room.Room;

public class AppDatabaseImpl {

    private static AppDatabase database;

    public static AppDatabase getDatabase(Context context) {
        if (database == null) {
            database = Room.databaseBuilder(context, AppDatabase.class, "zakinventorydb").build();
        }
        return database;
    }
}
