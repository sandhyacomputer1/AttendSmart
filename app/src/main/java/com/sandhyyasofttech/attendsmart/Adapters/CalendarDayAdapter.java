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
public class CalendarDayAdapter
        extends RecyclerView.Adapter<CalendarDayAdapter.ViewHolder> {

    private final List<AttendanceDayModel> days;
    private final View.OnClickListener clickListener;
    private final OnMonthStatsListener statsListener;

    private int present = 0, late = 0, halfDay = 0, absent = 0;

    public CalendarDayAdapter(List<AttendanceDayModel> days,
                              View.OnClickListener clickListener,
                              OnMonthStatsListener statsListener) {
        this.days = days;
        this.clickListener = clickListener;
        this.statsListener = statsListener;
        calculateStats();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_days, parent, false);
        return new ViewHolder(view);
    }
    public interface OnMonthStatsListener {
        void onStatsCalculated(int present, int late, int halfDay, int absent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceDayModel day = days.get(position);

        if (day.isEmpty) {
            holder.tvDay.setText("");
            holder.viewStatusIndicator.setVisibility(View.INVISIBLE);
            holder.llDayContainer.setBackgroundColor(Color.TRANSPARENT);
            holder.llDayContainer.setOnClickListener(null);
            return;
        }

        String[] parts = day.date.split("-");
        holder.tvDay.setText(parts.length == 3 ? parts[2] : day.date);
        holder.viewStatusIndicator.setVisibility(View.VISIBLE);

        int indicatorColor;
        int bgColor = Color.WHITE;
        int textColor = Color.parseColor("#212121");

        switch (day.status) {
            case "Present":
                indicatorColor = Color.parseColor("#4CAF50");
                bgColor = Color.parseColor("#E8F5E9");
                break;

            case "Absent":
                indicatorColor = Color.parseColor("#F44336");
                bgColor = Color.parseColor("#FDECEA");
                textColor = Color.WHITE;
                break;

            case "Half Day":
                indicatorColor = Color.parseColor("#2196F3");
                bgColor = Color.parseColor("#E3F2FD");
                break;

            case "Late":
                indicatorColor = Color.parseColor("#FFC107");
                bgColor = Color.parseColor("#FFF8E1");
                break;

            case "Future":
                indicatorColor = Color.parseColor("#E0E0E0");
                bgColor = Color.parseColor("#FAFAFA");
                textColor = Color.parseColor("#BDBDBD");
                break;

            default:
                indicatorColor = Color.parseColor("#BDBDBD");
        }

        holder.viewStatusIndicator.setBackgroundColor(indicatorColor);
        holder.llDayContainer.setBackgroundColor(bgColor);
        holder.tvDay.setTextColor(textColor);

        holder.itemView.setTag(day);
        holder.llDayContainer.setOnClickListener(
                day.status.equals("Future") ? null : clickListener
        );
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    /* ================== MONTHLY STATS ================== */

    private void calculateStats() {
        present = late = halfDay = absent = 0;

        for (AttendanceDayModel d : days) {
            if (d.isEmpty || d.status == null) continue;

            switch (d.status) {
                case "Present":
                    present++;
                    break;
                case "Late":
                    present++;
                    late++;
                    break;
                case "Half Day":
                    present++;
                    halfDay++;
                    break;
                case "Absent":
                    absent++;
                    break;
            }
        }

        if (statsListener != null) {
            statsListener.onStatsCalculated(present, late, halfDay, absent);
        }
    }

    public void updateData(List<AttendanceDayModel> newDays) {
        days.clear();
        days.addAll(newDays);
        calculateStats();
        notifyDataSetChanged();
    }

    /* ================== VIEW HOLDER ================== */

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
