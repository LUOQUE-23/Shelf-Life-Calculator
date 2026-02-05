package com.example.myapplication;

import java.time.LocalDate;

public class ProductItem {

    public enum ShelfLifeUnit {
        DAYS,
        MONTHS
    }

    private final long id;
    private final String name;
    private final LocalDate productionDate;
    private final int shelfLifeValue;
    private final ShelfLifeUnit shelfLifeUnit;
    private final int remindDays;
    private final LocalDate expiryDate;
    private final LocalDate remindDate;

    public ProductItem(
            long id,
            String name,
            LocalDate productionDate,
            int shelfLifeValue,
            ShelfLifeUnit shelfLifeUnit,
            int remindDays
    ) {
        this.id = id;
        this.name = name;
        this.productionDate = productionDate;
        this.shelfLifeValue = shelfLifeValue;
        this.shelfLifeUnit = shelfLifeUnit;
        this.remindDays = remindDays;
        this.expiryDate = shelfLifeUnit == ShelfLifeUnit.DAYS
                ? productionDate.plusDays(shelfLifeValue)
                : productionDate.plusMonths(shelfLifeValue);
        this.remindDate = expiryDate.minusDays(remindDays);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getProductionDate() {
        return productionDate;
    }

    public int getShelfLifeValue() {
        return shelfLifeValue;
    }

    public ShelfLifeUnit getShelfLifeUnit() {
        return shelfLifeUnit;
    }

    public int getRemindDays() {
        return remindDays;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public LocalDate getRemindDate() {
        return remindDate;
    }

    public static ProductItem fromEntity(ProductEntity entity) {
        LocalDate productionDate = LocalDate.ofEpochDay(entity.productionDateEpoch);
        ShelfLifeUnit unit = ShelfLifeUnit.valueOf(entity.shelfLifeUnit);
        return new ProductItem(
                entity.id,
                entity.name,
                productionDate,
                entity.shelfLifeValue,
                unit,
                entity.remindDays
        );
    }
}
