package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.User;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) // replace if duplicate is found
    void insertAll(User...users);

    @Update
    void updateAll(User...users);

    @Delete
    void delete(User user);

    @Query("SELECT * FROM users WHERE username=:username AND password=:password")
    List<User> getUser(String username, String password);

    @Query("SELECT * FROM users WHERE id=:id")
    List<User> getUser(int id);

    @Query("SELECT * FROM users")
    List<User> getAll();

    @Query("SELECT COUNT(*) FROM users")
    int getSize();
}
