//package com.sandhyyasofttech.attendsmart.Adapters;
//
//import android.graphics.Color;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.sandhyyasofttech.attendsmart.Models.AttendanceDayModel;
//import com.sandhyyasofttech.attendsmart.R;
//
//import java.text.SimpleDateFormat;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//
//public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.ViewHolder> {
//
//    private final List<AttendanceDayModel> days;
//    private final View.OnClickListener clickListener;
//    private final String companyKey;
//    private final String employeeMobile;
//
//    private int[] weeklyHolidays = {};
//    private String today;
//
//    public CalendarDayAdapter(List<AttendanceDayModel> days,
//                              View.OnClickListener clickListener,
//                              String companyKey,
//                              String employeeMobile) {
//        this.days = days;
//        this.clickListener = clickListener;
//        this.companyKey = companyKey;
//        this.employeeMobile = employeeMobile;
//
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//        this.today = sdf.format(new Date());
//    }
//
//    @NonNull
//    @Override
//    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_calendar_days, parent, false);
//        return new ViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//        AttendanceDayModel day = days.get(position);
//
//        // ✅ Empty cell (before month starts)
//        if (day.isEmpty) {
//            holder.tvDay.setText("");
//            holder.viewStatusIndicator.setVisibility(View.INVISIBLE);
//            holder.llDayContainer.setBackgroundColor(Color.TRANSPARENT);
//            holder.llDayContainer.setOnClickListener(null);
//            holder.itemView.setTag(null);
//            return;
//        }
//
//        // ✅ Extract day number
//        String[] parts = day.date.split("-");
//        holder.tvDay.setText(parts.length == 3 ? parts[2] : day.date);
//        holder.viewStatusIndicator.setVisibility(View.VISIBLE);
//
//        // ✅ Check if it's today
//        boolean isTodayDay = day.date.equals(today);
//
//        int indicatorColor;
//        int bgColor = Color.WHITE;
//        int textColor = Color.parseColor("#212121");
//
//        // ✅ Status-based coloring with today priority
//        switch (day.status) {
//            case "Present":
//                if (isTodayDay) {
//                    indicatorColor = Color.parseColor("#9C27B0"); // Purple indicator
//                    bgColor = Color.parseColor("#F3E5F5"); // Very light purple
//                    textColor = Color.parseColor("#4A148C"); // Dark purple text
//                } else {
//                    indicatorColor = Color.parseColor("#4CAF50"); // Green indicator
//                    bgColor = Color.parseColor("#E8F5E9"); // Very light green
//                    textColor = Color.parseColor("#1B5E20"); // Dark green text
//                }
//                break;
//
//            case "Absent":
//                if (isTodayDay) {
//                    indicatorColor = Color.parseColor("#9C27B0"); // Purple indicator
//                    bgColor = Color.parseColor("#F3E5F5"); // Very light purple
//                    textColor = Color.parseColor("#4A148C"); // Dark purple text
//                } else {
//                    indicatorColor = Color.parseColor("#F44336"); // Red indicator
//                    bgColor = Color.parseColor("#FFEBEE"); // Very light red
//                    textColor = Color.parseColor("#B71C1C"); // Dark red text
//                }
//                break;
//
//            case "Half Day":
//                if (isTodayDay) {
//                    indicatorColor = Color.parseColor("#9C27B0"); // Purple indicator
//                    bgColor = Color.parseColor("#F3E5F5"); // Very light purple
//                    textColor = Color.parseColor("#4A148C"); // Dark purple text
//                } else {
//                    indicatorColor = Color.parseColor("#2196F3"); // Blue indicator
//                    bgColor = Color.parseColor("#E3F2FD"); // Very light blue
//                    textColor = Color.parseColor("#0D47A1"); // Dark blue text
//                }
//                break;
//
//            case "Late":
//                if (isTodayDay) {
//                    indicatorColor = Color.parseColor("#9C27B0"); // Purple indicator
//                    bgColor = Color.parseColor("#F3E5F5"); // Very light purple
//                    textColor = Color.parseColor("#4A148C"); // Dark purple text
//                } else {
//                    indicatorColor = Color.parseColor("#FFC107"); // Yellow indicator
//                    bgColor = Color.parseColor("#FFFDE7"); // Very light yellow
//                    textColor = Color.parseColor("#F57F17"); // Dark yellow/amber text
//                }
//                break;
//
//            case "Holiday":
//                indicatorColor = Color.parseColor("#424242"); // Dark gray indicator
//                bgColor = Color.parseColor("#FAFAFA"); // Very light gray (almost white)
//                textColor = Color.parseColor("#212121"); // Very dark gray/black text
//                break;
//
//            case "Future":
//            case "Before Joining":
//                indicatorColor = Color.parseColor("#E0E0E0");
//                bgColor = Color.parseColor("#FAFAFA");
//                textColor = Color.parseColor("#BDBDBD");
//                break;
//
//            case "Loading":
//                indicatorColor = Color.parseColor("#BDBDBD");
//                bgColor = Color.parseColor("#F5F5F5");
//                textColor = Color.parseColor("#9E9E9E");
//                break;
//
//            default:
//                indicatorColor = Color.parseColor("#BDBDBD");
//                bgColor = Color.WHITE;
//        }
//
//        holder.viewStatusIndicator.setBackgroundColor(indicatorColor);
//        holder.llDayContainer.setBackgroundColor(bgColor);
//        holder.tvDay.setTextColor(textColor);
//
//        holder.itemView.setTag(day);
//
//        // ✅ Only allow clicks on valid dates
//        if (day.status.equals("Future") ||
//                day.status.equals("Before Joining") ||
//                day.status.equals("Loading") ||
//                day.status.equals("Holiday")) {
//            holder.llDayContainer.setOnClickListener(null);
//        } else {
//            holder.llDayContainer.setOnClickListener(clickListener);
//        }
//    }
//
//    @Override
//    public int getItemCount() {
//        return days.size();
//    }
//
//    /**
//     * ✅ Update weekly holidays dynamically
//     */
//    public void updateWeeklyHolidays(int[] holidays) {
//        this.weeklyHolidays = holidays != null ? holidays : new int[]{};
//        notifyDataSetChanged();
//    }
//
//    /**
//     * ✅ Update all data at once
//     */
//    public void updateData(List<AttendanceDayModel> newDays) {
//        days.clear();
//        days.addAll(newDays);
//        notifyDataSetChanged();
//    }
//
//    /**
//     * ✅ Check if a date is a holiday
//     */
//    private boolean isHolidayDay(String dateStr) {
//        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//            Date date = sdf.parse(dateStr);
//            if (date == null) return false;
//
//            Calendar cal = Calendar.getInstance();
//            cal.setTime(date);
//            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
//
//            for (int holiday : weeklyHolidays) {
//                if (dayOfWeek == holiday) {
//                    return true;
//                }
//            }
//        } catch (Exception e) {
//            return false;
//        }
//        return false;
//    }
//
//    /* ================== VIEW HOLDER ================== */
//
//    static class ViewHolder extends RecyclerView.ViewHolder {
//        TextView tvDay;
//        View viewStatusIndicator;
//        LinearLayout llDayContainer;
//
//        ViewHolder(@NonNull View itemView) {
//            super(itemView);
//            tvDay = itemView.findViewById(R.id.tvDay);
//            viewStatusIndicator = itemView.findViewById(R.id.viewStatusIndicator);
//            llDayContainer = itemView.findViewById(R.id.llDayContainer);
//        }
//    }
//}




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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.ViewHolder> {

    private final List<AttendanceDayModel> days;
    private final View.OnClickListener clickListener;
    private final String companyKey;
    private final String employeeMobile;

    private int[] weeklyHolidays = {};
    private String today;

    public CalendarDayAdapter(List<AttendanceDayModel> days,
                              View.OnClickListener clickListener,
                              String companyKey,
                              String employeeMobile) {
        this.days = days;
        this.clickListener = clickListener;
        this.companyKey = companyKey;
        this.employeeMobile = employeeMobile;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.today = sdf.format(new Date());
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

        // ✅ Empty cell (before month starts)
        if (day.isEmpty) {
            holder.tvDay.setText("");
            holder.viewStatusIndicator.setVisibility(View.INVISIBLE);
            holder.llDayContainer.setBackgroundColor(Color.TRANSPARENT);
            holder.llDayContainer.setOnClickListener(null);
            holder.itemView.setTag(null);
            return;
        }

        // ✅ Extract day number
        String[] parts = day.date.split("-");
        holder.tvDay.setText(parts.length == 3 ? parts[2] : day.date);
        holder.viewStatusIndicator.setVisibility(View.VISIBLE);

        // ✅ Check if it's today
        boolean isTodayDay = day.date.equals(today);

        int indicatorColor;
        int bgColor = Color.WHITE;
        int textColor = Color.parseColor("#212121");
        boolean isHolidayDay = isHolidayDay(day.date); // Check weekly holiday

        // ✅ Status-based coloring
        switch (day.status) {
            case "Holiday":
                // HOLIDAY (from attendance) - Purple
                indicatorColor = Color.parseColor("#9C27B0"); // Purple indicator
                bgColor = Color.parseColor("#F3E5F5"); // Very light purple background
                textColor = Color.parseColor("#7B1FA2"); // Dark purple text
                break;

            case "Weekly Holiday":
                // WEEKLY HOLIDAY - Black/Gray
                indicatorColor = Color.parseColor("#424242"); // Dark gray/black indicator
                bgColor = Color.parseColor("#F5F5F5"); // Light gray background
                textColor = Color.parseColor("#212121"); // Black text
                break;

            case "Present":
                if (isTodayDay) {
                    // TODAY - Dark Blue
                    indicatorColor = Color.parseColor("#1565C0"); // Dark blue indicator
                    bgColor = Color.parseColor("#E3F2FD"); // Light blue background
                    textColor = Color.parseColor("#0D47A1"); // Dark blue text
                } else {
                    // REGULAR PRESENT - Green
                    indicatorColor = Color.parseColor("#4CAF50"); // Green indicator
                    bgColor = Color.parseColor("#E8F5E9"); // Very light green
                    textColor = Color.parseColor("#1B5E20"); // Dark green text
                }
                break;

            case "Absent":
                if (isTodayDay) {
                    // TODAY - Dark Blue
                    indicatorColor = Color.parseColor("#1565C0"); // Dark blue indicator
                    bgColor = Color.parseColor("#E3F2FD"); // Light blue background
                    textColor = Color.parseColor("#0D47A1"); // Dark blue text
                } else {
                    // REGULAR ABSENT - Red
                    indicatorColor = Color.parseColor("#F44336"); // Red indicator
                    bgColor = Color.parseColor("#FFEBEE"); // Very light red
                    textColor = Color.parseColor("#B71C1C"); // Dark red text
                }
                break;

            case "Half Day":
                if (isTodayDay) {
                    // TODAY - Dark Blue
                    indicatorColor = Color.parseColor("#1565C0"); // Dark blue indicator
                    bgColor = Color.parseColor("#E3F2FD"); // Light blue background
                    textColor = Color.parseColor("#0D47A1"); // Dark blue text
                } else {
                    // REGULAR HALF DAY - Light Blue
                    indicatorColor = Color.parseColor("#03A9F4"); // Light blue indicator
                    bgColor = Color.parseColor("#E1F5FE"); // Very light blue background
                    textColor = Color.parseColor("#0277BD"); // Medium blue text
                }
                break;

            case "Late":
                if (isTodayDay) {
                    // TODAY - Dark Blue
                    indicatorColor = Color.parseColor("#1565C0"); // Dark blue indicator
                    bgColor = Color.parseColor("#E3F2FD"); // Light blue background
                    textColor = Color.parseColor("#0D47A1"); // Dark blue text
                } else {
                    // REGULAR LATE - Yellow
                    indicatorColor = Color.parseColor("#FFC107"); // Yellow indicator
                    bgColor = Color.parseColor("#FFFDE7"); // Very light yellow
                    textColor = Color.parseColor("#F57F17"); // Dark yellow/amber text
                }
                break;

            case "Future":
            case "Before Joining":
                indicatorColor = Color.parseColor("#E0E0E0"); // Light gray
                bgColor = Color.parseColor("#FAFAFA"); // Very light gray
                textColor = Color.parseColor("#BDBDBD"); // Medium gray text
                break;

            case "Loading":
                indicatorColor = Color.parseColor("#BDBDBD"); // Medium gray
                bgColor = Color.parseColor("#F5F5F5"); // Light gray
                textColor = Color.parseColor("#9E9E9E"); // Dark gray text
                break;

            default:
                // Check if it's a weekly holiday day
                if (isHolidayDay) {
                    // WEEKLY HOLIDAY - Black/Gray
                    indicatorColor = Color.parseColor("#424242"); // Dark gray/black indicator
                    bgColor = Color.parseColor("#F5F5F5"); // Light gray background
                    textColor = Color.parseColor("#212121"); // Black text
                } else {
                    indicatorColor = Color.parseColor("#BDBDBD"); // Default gray
                    bgColor = Color.WHITE;
                }
        }

        holder.viewStatusIndicator.setBackgroundColor(indicatorColor);
        holder.llDayContainer.setBackgroundColor(bgColor);
        holder.tvDay.setTextColor(textColor);

        holder.itemView.setTag(day);

        // ✅ Only allow clicks on working days with attendance
        if (day.status.equals("Future") ||
                day.status.equals("Before Joining") ||
                day.status.equals("Loading") ||
                day.status.equals("Holiday") ||
                day.status.equals("Weekly Holiday") ||
                isHolidayDay) {
            holder.llDayContainer.setOnClickListener(null);
        } else {
            holder.llDayContainer.setOnClickListener(clickListener);
        }
    }
    @Override
    public int getItemCount() {
        return days.size();
    }

    /**
     * ✅ Update weekly holidays dynamically
     */
    public void updateWeeklyHolidays(int[] holidays) {
        this.weeklyHolidays = holidays != null ? holidays : new int[]{};
        notifyDataSetChanged();
    }

    /**
     * ✅ Update all data at once
     */
    public void updateData(List<AttendanceDayModel> newDays) {
        days.clear();
        days.addAll(newDays);
        notifyDataSetChanged();
    }

    /**
     * ✅ Check if a date is a holiday
     */
    private boolean isHolidayDay(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            if (date == null) return false;

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

            for (int holiday : weeklyHolidays) {
                if (dayOfWeek == holiday) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
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