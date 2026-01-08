//package com.sandhyyasofttech.attendsmart.Adapters;
//
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.sandhyyasofttech.attendsmart.Models.LeaveModel;
//import com.sandhyyasofttech.attendsmart.R;
//
//import java.util.List;
//
//public class MyLeavesAdapter extends RecyclerView.Adapter<MyLeavesAdapter.VH> {
//
//    private final List<LeaveModel> list;
//
//    public MyLeavesAdapter(List<LeaveModel> list) {
//        this.list = list;
//    }
//
//    @NonNull
//    @Override
//    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View v = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_leave, parent, false);
//        return new VH(v);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull VH h, int p) {
//        LeaveModel m = list.get(p);
//
//        h.tvDates.setText(m.fromDate + " → " + m.toDate);
//        h.tvType.setText(m.leaveType +
//                (m.halfDayType != null ? " (" + m.halfDayType + ")" : ""));
//
//        h.tvStatus.setText(m.status);
//
//        if ("REJECTED".equals(m.status) && m.adminReason != null) {
//            h.tvReason.setVisibility(View.VISIBLE);
//            h.tvReason.setText("Reason: " + m.adminReason);
//        } else {
//            h.tvReason.setVisibility(View.GONE);
//        }
//    }
//
//    @Override
//    public int getItemCount() {
//        return list.size();
//    }
//
//    static class VH extends RecyclerView.ViewHolder {
//        TextView tvDates, tvType, tvStatus, tvReason;
//        VH(View v) {
//            super(v);
//            tvDates = v.findViewById(R.id.tvDates);
//            tvType = v.findViewById(R.id.tvType);
//            tvStatus = v.findViewById(R.id.tvStatus);
//            tvReason = v.findViewById(R.id.tvReason);
//        }
//    }
//}



package com.sandhyyasofttech.attendsmart.Adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sandhyyasofttech.attendsmart.Models.LeaveModel;
import com.sandhyyasofttech.attendsmart.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MyLeavesAdapter extends RecyclerView.Adapter<MyLeavesAdapter.VH> {

    private final List<LeaveModel> list;

    public MyLeavesAdapter(List<LeaveModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leave, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int p) {
        LeaveModel m = list.get(p);

        // Format date range with year
        h.tvDateRange.setText(formatDateRangeWithYear(m.fromDate, m.toDate));

        // Calculate and display duration
        h.tvDuration.setText(calculateDuration(m.fromDate, m.toDate, m.leaveType));

        // Leave type with half-day indicator
        String typeText = formatLeaveType(m.leaveType);
        if (m.halfDayType != null && !m.halfDayType.isEmpty()) {
            typeText += " (" + m.halfDayType + ")";
        }
        h.tvType.setText(typeText);

        // Status badge with dynamic color
        h.tvStatus.setText(m.status);
        applyStatusStyle(h.tvStatus, m.status);

        // Show paid badge if applicable
        if (m.isPaid != null && m.isPaid) {
            h.chipPaid.setVisibility(View.VISIBLE);
        } else {
            h.chipPaid.setVisibility(View.GONE);
        }

        // Applied date
        String appliedDateStr = m.getAppliedDate();
        if (appliedDateStr != null && !appliedDateStr.isEmpty()) {
            h.tvAppliedDate.setVisibility(View.VISIBLE);
            h.tvAppliedDate.setText("Applied on " + formatAppliedDate(appliedDateStr));
        } else {
            h.tvAppliedDate.setVisibility(View.GONE);
        }

        // Rejection reason
        if ("REJECTED".equals(m.status) && m.adminReason != null && !m.adminReason.isEmpty()) {
            h.tvReason.setVisibility(View.VISIBLE);
            h.tvReason.setText("Reason: " + m.adminReason);
        } else {
            h.tvReason.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // Format date range with year (e.g., "07 Jan - 09 Jan 2026" or "15 Jan 2026")
    private String formatDateRangeWithYear(String fromDate, String toDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat dayMonthFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
            SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());

            Date from = inputFormat.parse(fromDate);
            Date to = inputFormat.parse(toDate);

            if (from != null && to != null) {
                String year = yearFormat.format(to);

                if (fromDate.equals(toDate)) {
                    return dayMonthFormat.format(from) + " " + year;
                } else {
                    return dayMonthFormat.format(from) + " - " + dayMonthFormat.format(to) + " " + year;
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return fromDate + " → " + toDate;
    }

    // Calculate duration in days
    private String calculateDuration(String fromDate, String toDate, String leaveType) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date from = sdf.parse(fromDate);
            Date to = sdf.parse(toDate);

            if (from != null && to != null) {
                long diffInMillis = to.getTime() - from.getTime();
                long days = TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1;

                // Handle half-day
                if (leaveType != null && leaveType.contains("HALF")) {
                    return "0.5 day";
                }

                if (days == 1) {
                    return "1 day";
                } else {
                    return days + " days";
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    // Format leave type to be more readable
    private String formatLeaveType(String type) {
        if (type == null) return "";

        // Convert "CASUAL_LEAVE" to "Casual Leave"
        String[] words = type.replace("_", " ").toLowerCase(Locale.getDefault()).split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    // Format applied date
    private String formatAppliedDate(String appliedDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

            Date date = inputFormat.parse(appliedDate);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return appliedDate;
    }

    // Apply dynamic status badge styling
    private void applyStatusStyle(TextView tv, String status) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12);

        if (status == null) status = "PENDING";

        switch (status.toUpperCase()) {
            case "APPROVED":
                bg.setColor(Color.parseColor("#E8F5E9"));
                tv.setTextColor(Color.parseColor("#2E7D32"));
                break;
            case "REJECTED":
                bg.setColor(Color.parseColor("#FFEBEE"));
                tv.setTextColor(Color.parseColor("#C62828"));
                break;
            case "PENDING":
                bg.setColor(Color.parseColor("#FFF3E0"));
                tv.setTextColor(Color.parseColor("#EF6C00"));
                break;
            case "CANCELLED":
                bg.setColor(Color.parseColor("#F5F5F5"));
                tv.setTextColor(Color.parseColor("#616161"));
                break;
            default:
                bg.setColor(Color.parseColor("#E3F2FD"));
                tv.setTextColor(Color.parseColor("#1976D2"));
                break;
        }

        tv.setBackground(bg);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDateRange, tvDuration, tvType, tvStatus, tvReason, chipPaid, tvAppliedDate;

        VH(View v) {
            super(v);
            tvDateRange = v.findViewById(R.id.tvDateRange);
            tvDuration = v.findViewById(R.id.tvDuration);
            tvType = v.findViewById(R.id.tvType);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvReason = v.findViewById(R.id.tvReason);
            chipPaid = v.findViewById(R.id.chipPaid);
            tvAppliedDate = v.findViewById(R.id.tvAppliedDate);
        }
    }
}