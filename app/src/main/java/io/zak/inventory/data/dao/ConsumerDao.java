package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.Consumer;

@Dao
public interface ConsumerDao {

    @Insert
    long insert(Consumer consumer);

    @Update
    int update(Consumer consumer);

    @Update
    void updateAll(Consumer...consumers);

    @Delete
    int delete(Consumer consumer);

    @Query("SELECT * FROM consumers")
    List<Consumer> getAll();

    @Query("SELECT * FROM consumers WHERE consumerId=:id")
    List<Consumer> getConsumer(int id);

    @Query("SELECT COUNT(*) FROM consumers")
    int getSize();
}
