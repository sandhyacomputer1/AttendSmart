package com.sandhyyasofttech.attendsmart.Adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sandhyyasofttech.attendsmart.Models.AttendanceDayModel;
import com.sandhyyasofttech.attendsmart.R;

import java.util.List;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.ViewHolder> {

    private final List<AttendanceDayModel> days;
    private final View.OnClickListener clickListener;

    public CalendarDayAdapter(List<AttendanceDayModel> days, View.OnClickListener clickListener) {
        this.days = days;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_days, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceDayModel day = days.get(position);

        // Safety check
        if (holder.tvDay == null || holder.viewStatusIndicator == null || holder.llDayContainer == null) {
            return;
        }

        if (day.isEmpty) {
            holder.tvDay.setText("");
            holder.viewStatusIndicator.setVisibility(View.INVISIBLE);
            holder.llDayContainer.setBackgroundColor(Color.TRANSPARENT);
            holder.llDayContainer.setOnClickListener(null);
            return;
        }

        String[] parts = day.date.split("-");
        String dayNumber = parts.length == 3 ? parts[2] : day.date;
        holder.tvDay.setText(dayNumber);

        holder.viewStatusIndicator.setVisibility(View.VISIBLE);

        int statusColor;
        int bgColor = Color.WHITE;

        switch (day.status) {
            case "Present":
                statusColor = Color.parseColor("#4CAF50");
                break;
            case "Absent":
                statusColor = Color.parseColor("#F44336");
                break;
            case "Half Day":
                statusColor = Color.parseColor("#FF9800");
                break;
            case "Late":
                statusColor = Color.parseColor("#FFC107");
                break;
            case "Future":
                statusColor = Color.parseColor("#E0E0E0");
                holder.tvDay.setTextColor(Color.parseColor("#BDBDBD"));
                bgColor = Color.parseColor("#FAFAFA");
                break;
            default:
                statusColor = Color.parseColor("#CCCCCC");
                break;
        }

        holder.viewStatusIndicator.setBackgroundColor(statusColor);
        holder.llDayContainer.setBackgroundColor(bgColor);

        if (!day.status.equals("Future")) {
            holder.tvDay.setTextColor(Color.parseColor("#212121"));
        }

        holder.itemView.setTag(day);
        holder.llDayContainer.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay;
        View viewStatusIndicator;
        LinearLayout llDayContainer;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            viewStatusIndicator = itemView.findViewById(R.id.viewStatusIndicator);
            llDayContainer = itemView.findViewById(R.id.llDayContainer);
        }
    }
}