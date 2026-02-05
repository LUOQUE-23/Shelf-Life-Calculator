package com.example.myapplication;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.example.myapplication.databinding.ContentMainBinding;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_POST_NOTIFICATIONS = 1001;

    private ActivityMainBinding binding;
    private ContentMainBinding contentBinding;
    private final List<ProductItem> items = new ArrayList<>();
    private ProductAdapter adapter;
    private LocalDate selectedProductionDate;
    private DateTimeFormatter displayFormatter;
    private AppDatabase database;
    private ProductDao productDao;
    private ExecutorService dbExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        contentBinding = binding.contentMain;

        displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        ReminderScheduler.createNotificationChannel(this);
        ensureNotificationPermission();
        setupUnitDropdown();
        setupRecyclerView();
        setupDatePicker();
        setupDatabase();

        if (TextUtils.isEmpty(getTextValue(contentBinding.inputRemindDays))) {
            contentBinding.inputRemindDays.setText(getString(R.string.default_remind_days));
        }

        contentBinding.buttonAdd.setOnClickListener(v -> addProduct());
    }

    private void setupDatabase() {
        database = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "product-db").build();
        productDao = database.productDao();
        dbExecutor = Executors.newSingleThreadExecutor();
        loadProductsFromDb();
    }

    private void setupUnitDropdown() {
        String[] units = getResources().getStringArray(R.array.shelf_life_units);
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                units
        );
        contentBinding.inputShelfLifeUnit.setAdapter(unitAdapter);
        if (units.length > 0) {
            contentBinding.inputShelfLifeUnit.setText(units[0], false);
        }
    }

    private void setupRecyclerView() {
        adapter = new ProductAdapter(this, items, this::openCalendarInsert);
        contentBinding.productList.setLayoutManager(new LinearLayoutManager(this));
        contentBinding.productList.setAdapter(adapter);
        updateEmptyState();
    }

    private void setupDatePicker() {
        contentBinding.inputProductionDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        LocalDate initialDate = selectedProductionDate != null ? selectedProductionDate : LocalDate.now();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedProductionDate = LocalDate.of(year, month + 1, dayOfMonth);
                    contentBinding.inputProductionDate.setText(displayFormatter.format(selectedProductionDate));
                    contentBinding.inputProductionDateLayout.setError(null);
                },
                initialDate.getYear(),
                initialDate.getMonthValue() - 1,
                initialDate.getDayOfMonth()
        );
        dialog.show();
    }

    private void addProduct() {
        clearErrors();

        boolean valid = true;
        String name = getTextValue(contentBinding.inputName);
        if (TextUtils.isEmpty(name)) {
            name = getString(R.string.default_product_name);
        }

        if (selectedProductionDate == null) {
            contentBinding.inputProductionDateLayout.setError(getString(R.string.error_pick_date));
            valid = false;
        } else if (selectedProductionDate.isAfter(LocalDate.now())) {
            contentBinding.inputProductionDateLayout.setError(getString(R.string.error_date_in_future));
            valid = false;
        }

        String shelfLifeValueText = getTextValue(contentBinding.inputShelfLifeValue);
        int shelfLifeValue = 0;
        if (TextUtils.isEmpty(shelfLifeValueText)) {
            contentBinding.inputShelfLifeValueLayout.setError(getString(R.string.error_required));
            valid = false;
        } else {
            shelfLifeValue = parseIntSafe(shelfLifeValueText);
            if (shelfLifeValue <= 0) {
                contentBinding.inputShelfLifeValueLayout.setError(getString(R.string.error_positive));
                valid = false;
            }
        }

        String remindDaysText = getTextValue(contentBinding.inputRemindDays);
        int remindDays = 0;
        if (TextUtils.isEmpty(remindDaysText)) {
            contentBinding.inputRemindDaysLayout.setError(getString(R.string.error_required));
            valid = false;
        } else {
            remindDays = parseIntSafe(remindDaysText);
            if (remindDays < 0) {
                contentBinding.inputRemindDaysLayout.setError(getString(R.string.error_non_negative));
                valid = false;
            }
        }

        if (!valid) {
            return;
        }

        ProductItem.ShelfLifeUnit unit = getSelectedUnit();
        ProductItem newItem = new ProductItem(
                0L,
                name,
                selectedProductionDate,
                shelfLifeValue,
                unit,
                remindDays
        );

        dbExecutor.execute(() -> {
            ProductEntity entity = new ProductEntity(
                    newItem.getName(),
                    newItem.getProductionDate().toEpochDay(),
                    newItem.getShelfLifeValue(),
                    newItem.getShelfLifeUnit().name(),
                    newItem.getRemindDays(),
                    newItem.getExpiryDate().toEpochDay(),
                    newItem.getRemindDate().toEpochDay(),
                    System.currentTimeMillis()
            );
            long id = productDao.insert(entity);
            entity.id = id;
            ProductItem savedItem = ProductItem.fromEntity(entity);
            runOnUiThread(() -> {
                items.add(0, savedItem);
                adapter.notifyItemInserted(0);
                contentBinding.productList.scrollToPosition(0);
                updateEmptyState();
                clearInputs();
                ReminderScheduler.scheduleReminder(getApplicationContext(), savedItem);
            });
        });
    }

    private ProductItem.ShelfLifeUnit getSelectedUnit() {
        String label = getTextValue(contentBinding.inputShelfLifeUnit);
        if (getString(R.string.unit_months).equals(label)) {
            return ProductItem.ShelfLifeUnit.MONTHS;
        }
        return ProductItem.ShelfLifeUnit.DAYS;
    }

    private void clearInputs() {
        contentBinding.inputName.setText("");
        contentBinding.inputProductionDate.setText("");
        contentBinding.inputShelfLifeValue.setText("");
        selectedProductionDate = null;
    }

    private void clearErrors() {
        contentBinding.inputNameLayout.setError(null);
        contentBinding.inputProductionDateLayout.setError(null);
        contentBinding.inputShelfLifeValueLayout.setError(null);
        contentBinding.inputRemindDaysLayout.setError(null);
    }

    private void updateEmptyState() {
        if (items.isEmpty()) {
            contentBinding.emptyStateText.setVisibility(android.view.View.VISIBLE);
        } else {
            contentBinding.emptyStateText.setVisibility(android.view.View.GONE);
        }
    }

    private String getTextValue(android.widget.TextView textView) {
        if (textView.getText() == null) {
            return "";
        }
        return textView.getText().toString().trim();
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void loadProductsFromDb() {
        dbExecutor.execute(() -> {
            List<ProductEntity> entities = productDao.getAll();
            List<ProductItem> loaded = new ArrayList<>();
            for (ProductEntity entity : entities) {
                loaded.add(ProductItem.fromEntity(entity));
            }
            runOnUiThread(() -> {
                items.clear();
                items.addAll(loaded);
                adapter.notifyDataSetChanged();
                updateEmptyState();
                for (ProductItem item : loaded) {
                    ReminderScheduler.scheduleReminder(getApplicationContext(), item);
                }
            });
        });
    }

    private void openCalendarInsert(ProductItem item) {
        long remindMillis = toEpochMillis(item.getRemindDate());
        long endMillis = remindMillis + 30 * 60 * 1000L;

        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(android.provider.CalendarContract.Events.CONTENT_URI);
        intent.putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, remindMillis);
        intent.putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, endMillis);
        intent.putExtra(
                android.provider.CalendarContract.Events.TITLE,
                getString(R.string.calendar_event_title, item.getName())
        );
        intent.putExtra(
                android.provider.CalendarContract.Events.DESCRIPTION,
                getString(R.string.calendar_event_description, displayFormatter.format(item.getExpiryDate()))
        );
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private long toEpochMillis(LocalDate date) {
        LocalDateTime dateTime = date.atTime(9, 0);
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void ensureNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_POST_NOTIFICATIONS
                );
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbExecutor != null) {
            dbExecutor.shutdown();
        }
        if (database != null) {
            database.close();
        }
    }
}
