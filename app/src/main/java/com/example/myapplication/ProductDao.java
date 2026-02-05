package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ProductDao {

    @Insert
    long insert(ProductEntity entity);

    @Query("SELECT * FROM products ORDER BY createdAtEpoch DESC")
    List<ProductEntity> getAll();

    @Query("DELETE FROM products")
    void clearAll();
}
