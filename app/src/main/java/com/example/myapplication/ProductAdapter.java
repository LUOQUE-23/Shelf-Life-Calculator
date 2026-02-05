package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.databinding.ItemProductBinding;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnAddToCalendarListener {
        void onAddToCalendar(ProductItem item);
    }

    private final List<ProductItem> items;
    private final DateTimeFormatter displayFormatter;
    private final Context context;
    private final OnAddToCalendarListener calendarListener;

    public ProductAdapter(Context context, List<ProductItem> items, OnAddToCalendarListener calendarListener) {
        this.context = context;
        this.items = items;
        this.calendarListener = calendarListener;
        this.displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemProductBinding binding = ItemProductBinding.inflate(inflater, parent, false);
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductItem item = items.get(position);
        holder.bind(context, displayFormatter, item, calendarListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {

        private final ItemProductBinding binding;

        ProductViewHolder(ItemProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Context context, DateTimeFormatter formatter, ProductItem item, OnAddToCalendarListener listener) {
            binding.textName.setText(item.getName());
            binding.textProductionDate.setText(context.getString(
                    R.string.label_production_date_format,
                    formatter.format(item.getProductionDate())
            ));
            String unitLabel = item.getShelfLifeUnit() == ProductItem.ShelfLifeUnit.DAYS
                    ? context.getString(R.string.unit_days)
                    : context.getString(R.string.unit_months);
            binding.textShelfLife.setText(context.getString(
                    R.string.label_shelf_life_format,
                    item.getShelfLifeValue(),
                    unitLabel
            ));
            binding.textExpiryDate.setText(context.getString(
                    R.string.label_expiry_date_format,
                    formatter.format(item.getExpiryDate())
            ));
            binding.textRemind.setText(context.getString(
                    R.string.label_remind_format,
                    item.getRemindDays(),
                    formatter.format(item.getRemindDate())
            ));

            LocalDate today = LocalDate.now();
            long daysRemaining = ChronoUnit.DAYS.between(today, item.getExpiryDate());
            if (daysRemaining < 0) {
                int overdueDays = (int) Math.abs(daysRemaining);
                binding.textStatus.setText(context.getString(R.string.status_expired, overdueDays));
                binding.textStatus.setTextColor(ContextCompat.getColor(context, R.color.status_expired));
            } else if (daysRemaining == 0) {
                binding.textStatus.setText(context.getString(R.string.status_due_today));
                binding.textStatus.setTextColor(ContextCompat.getColor(context, R.color.status_warning));
            } else if (daysRemaining <= item.getRemindDays()) {
                binding.textStatus.setText(context.getString(R.string.status_near_expiry, daysRemaining));
                binding.textStatus.setTextColor(ContextCompat.getColor(context, R.color.status_warning));
            } else {
                binding.textStatus.setText(context.getString(R.string.status_ok, daysRemaining));
                binding.textStatus.setTextColor(ContextCompat.getColor(context, R.color.status_normal));
            }

            binding.buttonAddToCalendar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddToCalendar(item);
                }
            });
        }
    }
}
