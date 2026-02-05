package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class ProductEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name;

    public long productionDateEpoch;

    public int shelfLifeValue;

    @NonNull
    public String shelfLifeUnit;

    public int remindDays;

    public long expiryDateEpoch;

    public long remindDateEpoch;

    public long createdAtEpoch;

    public ProductEntity(
            @NonNull String name,
            long productionDateEpoch,
            int shelfLifeValue,
            @NonNull String shelfLifeUnit,
            int remindDays,
            long expiryDateEpoch,
            long remindDateEpoch,
            long createdAtEpoch
    ) {
        this.name = name;
        this.productionDateEpoch = productionDateEpoch;
        this.shelfLifeValue = shelfLifeValue;
        this.shelfLifeUnit = shelfLifeUnit;
        this.remindDays = remindDays;
        this.expiryDateEpoch = expiryDateEpoch;
        this.remindDateEpoch = remindDateEpoch;
        this.createdAtEpoch = createdAtEpoch;
    }
}
