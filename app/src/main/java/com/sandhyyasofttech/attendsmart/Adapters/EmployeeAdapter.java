package com.sandhyyasofttech.attendsmart.Adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sandhyyasofttech.attendsmart.Activities.AdminEmployeeAttendanceActivity;
import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
import com.sandhyyasofttech.attendsmart.R;

import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private static final String TAG = "EmployeeAdapter";
    private final List<EmployeeModel> employeeList;
    private final Context context;

    public EmployeeAdapter(List<EmployeeModel> employeeList, Context context) {
        this.employeeList = employeeList;
        this.context = context;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        EmployeeModel model = employeeList.get(position);

        // Basic Info
        safeSetText(holder.tvName, model.getEmployeeName() != null ? model.getEmployeeName() : "N/A");
        safeSetText(holder.tvMobile, model.getEmployeeMobile() != null ? model.getEmployeeMobile() : "N/A");
        safeSetText(holder.tvDepartment, model.getEmployeeDepartment() != null ? model.getEmployeeDepartment() : "N/A");

        // Status
        String status = model.getTodayStatus();
        safeSetText(holder.tvStatus, status != null ? status : "Absent");

        if (holder.tvStatus != null) {
            holder.tvStatus.setTextColor(getStatusColor(status));
        }

        // Check-in and Check-out Time Display
        String checkInTime = model.getCheckInTime();
        String checkOutTime = model.getCheckOutTime();

        if (holder.tvCheckInTime != null) {
            if ("Absent".equals(status)) {
                holder.tvCheckInTime.setText("No attendance today");
                holder.tvCheckInTime.setTextColor(ContextCompat.getColor(context, R.color.error));
            } else if (checkInTime != null && !checkInTime.isEmpty()) {
                if (checkOutTime != null && !checkOutTime.isEmpty()) {
                    // Both check-in and check-out available
                    holder.tvCheckInTime.setText("In: " + checkInTime + " • Out: " + checkOutTime);
                } else {
                    // Only check-in available
                    holder.tvCheckInTime.setText("Check-in: " + checkInTime);
                }
                holder.tvCheckInTime.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
            } else {
                holder.tvCheckInTime.setText("");
            }
        }

        // Photo logic
        if (holder.ivPhoto != null) {
            String photoUrl = model.getCheckInPhoto();
            Log.d(TAG, "Loading photo for " + model.getEmployeeName() + ": " + photoUrl);

            if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(context)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(holder.ivPhoto);
            } else {
                holder.ivPhoto.setImageResource(R.drawable.ic_profile);
            }
        }

        // Status icon and badge background
        if (holder.ivStatus != null && holder.statusBadge != null) {
            switch (status != null ? status : "Absent") {
                case "Present":
                    holder.ivStatus.setImageResource(R.drawable.ic_check_circle);
                    holder.ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.success));
                    holder.statusBadge.setBackgroundResource(R.drawable.status_badge_present);
                    break;
                case "Late":
                    holder.ivStatus.setImageResource(R.drawable.ic_schedule);
                    holder.ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.warning));
                    holder.statusBadge.setBackgroundResource(R.drawable.status_badge_late);
                    break;
                case "Half Day":
                    holder.ivStatus.setImageResource(R.drawable.ic_half_day);
                    holder.ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.warning));
                    holder.statusBadge.setBackgroundResource(R.drawable.status_badge_halfday);
                    break;
                case "Absent":
                default:
                    holder.ivStatus.setImageResource(R.drawable.ic_close_circle);
                    holder.ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.error));
                    holder.statusBadge.setBackgroundResource(R.drawable.status_badge_absent);
                    break;
            }
        }

        // Click listener to open attendance report
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminEmployeeAttendanceActivity.class);
            intent.putExtra("employeeMobile", model.getEmployeeMobile());
            intent.putExtra("employeeName", model.getEmployeeName());
            intent.putExtra("employeeRole", model.getEmployeeRole());
            context.startActivity(intent);
        });
    }

    private void safeSetText(TextView textView, String text) {
        if (textView != null) {
            textView.setText(text);
        } else {
            Log.e(TAG, "TextView is null: " + text);
        }
    }

    static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMobile, tvDepartment, tvStatus, tvCheckInTime;
        ImageView ivStatus, ivPhoto;
        View statusBadge;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEmpName);
            tvMobile = itemView.findViewById(R.id.tvEmpMobile);
            tvDepartment = itemView.findViewById(R.id.tvEmpDepartment);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCheckInTime = itemView.findViewById(R.id.tvCheckInTime);
            ivStatus = itemView.findViewById(R.id.ivStatus);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
            statusBadge = itemView.findViewById(R.id.statusBadge);

            if (tvName == null) Log.e("EmployeeVH", "tvEmpName NOT FOUND in item_employee.xml");
            if (tvMobile == null) Log.e("EmployeeVH", "tvEmpMobile NOT FOUND in item_employee.xml");
            if (tvDepartment == null) Log.e("EmployeeVH", "tvEmpDepartment NOT FOUND");
        }
    }

    private int getStatusColor(String status) {
        if ("Present".equals(status)) {
            return ContextCompat.getColor(context, R.color.success);
        } else if ("Late".equals(status)) {
            return ContextCompat.getColor(context, R.color.warning);
        } else if ("Half Day".equals(status)) {
            return ContextCompat.getColor(context, R.color.warning);
        } else {
            return ContextCompat.getColor(context, R.color.error);
        }
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }
}