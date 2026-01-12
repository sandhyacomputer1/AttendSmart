package com.sandhyyasofttech.attendsmart.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.FirebaseDatabase;
import com.sandhyyasofttech.attendsmart.Models.WorkSummary;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkReportPagerAdapter extends RecyclerView.Adapter<WorkReportPagerAdapter.ViewHolder> {

    private List<WorkSummary> works = new ArrayList<>();
    private Context context;
    private PrefManager pref;
    private String todayDate;
    private OnWorkActionListener listener;

    public interface OnWorkActionListener {
        void onEditClick(WorkSummary work);
        void onDeleteSuccess();
    }

    public WorkReportPagerAdapter(Context context, OnWorkActionListener listener) {
        this.context = context;
        this.pref = new PrefManager(context);
        this.todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        this.listener = listener;
    }

    public void updateWorks(List<WorkSummary> works) {
        this.works.clear();
        if (works != null) {
            this.works.addAll(works);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_work_report_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkSummary work = works.get(position);
        holder.bind(work);
    }

    @Override
    public int getItemCount() {
        return works.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvDate, tvDayName, tvEmployeeName, tvSubmittedTime;
        TextView tvCompletedWork, tvOngoingWork, tvTomorrowWork;
        View layoutCompleted, layoutOngoing, layoutTomorrow;
        MaterialButton btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardWorkReport);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDayName = itemView.findViewById(R.id.tvDayName);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
            tvSubmittedTime = itemView.findViewById(R.id.tvSubmittedTime);

            tvCompletedWork = itemView.findViewById(R.id.tvCompletedWork);
            tvOngoingWork = itemView.findViewById(R.id.tvOngoingWork);
            tvTomorrowWork = itemView.findViewById(R.id.tvTomorrowWork);

            layoutCompleted = itemView.findViewById(R.id.layoutCompleted);
            layoutOngoing = itemView.findViewById(R.id.layoutOngoing);
            layoutTomorrow = itemView.findViewById(R.id.layoutTomorrow);

            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(WorkSummary work) {
            // Format and display date
            if (work.workDate != null) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date date = inputFormat.parse(work.workDate);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

                    // Highlight today with border only
                    if (work.workDate.equals(todayDate)) {
                        tvDate.setText("  " + dateFormat.format(date));
                        tvDayName.setText("TODAY");
                        cardView.setStrokeColor(0xFFFF5722);
                        cardView.setStrokeWidth(2);
                    } else {
                        tvDate.setText(dateFormat.format(date));
                        tvDayName.setText(dayFormat.format(date).toUpperCase());
                        cardView.setStrokeColor(0xFFE0E0E0);
                        cardView.setStrokeWidth(2);
                    }

                    // Keep header text WHITE for all days
                    tvDate.setTextColor(0xFFFFFFFF);
                    tvDayName.setTextColor(0xFFBBDEFB);

                } catch (Exception e) {
                    tvDate.setText(work.workDate);
                    tvDayName.setText("");
                }
            }

            // Employee Name
            tvEmployeeName.setText(work.employeeName != null ? work.employeeName : "Employee");

            // Submitted Time
            tvSubmittedTime.setText("⏰ " + formatTime(work.submittedAt));

            // Completed Work
            if (!TextUtils.isEmpty(work.completedWork)) {
                layoutCompleted.setVisibility(View.VISIBLE);
                tvCompletedWork.setText(work.completedWork);
            } else {
                layoutCompleted.setVisibility(View.GONE);
            }

            // Ongoing Work
            if (!TextUtils.isEmpty(work.ongoingWork)) {
                layoutOngoing.setVisibility(View.VISIBLE);
                tvOngoingWork.setText(work.ongoingWork);
            } else {
                layoutOngoing.setVisibility(View.GONE);
            }

            // Tomorrow Work
            if (!TextUtils.isEmpty(work.tomorrowWork)) {
                layoutTomorrow.setVisibility(View.VISIBLE);
                tvTomorrowWork.setText(work.tomorrowWork);
            } else {
                layoutTomorrow.setVisibility(View.GONE);
            }

            // Edit Button - Only enabled for today's work
            boolean isToday = work.workDate != null && work.workDate.equals(todayDate);
            btnEdit.setEnabled(isToday);
            btnEdit.setAlpha(isToday ? 1.0f : 0.5f);
            btnEdit.setOnClickListener(v -> {
                if (isToday && listener != null) {
                    listener.onEditClick(work);
                } else {
                    Toast.makeText(context, "✏️ Only today's work can be edited", Toast.LENGTH_SHORT).show();
                }
            });

            // Delete Button - Only enabled for today's work
            btnDelete.setEnabled(isToday);
            btnDelete.setAlpha(isToday ? 1.0f : 0.5f);
            btnDelete.setOnClickListener(v -> {
                if (isToday) {
                    showDeleteDialog(work);
                } else {
                    Toast.makeText(context, "🗑️ Only today's work can be deleted", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private String formatTime(long timestamp) {
            try {
                if (timestamp <= 0) return "Unknown time";
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                return timeFormat.format(new Date(timestamp));
            } catch (Exception e) {
                return "Unknown time";
            }
        }

        private void showDeleteDialog(WorkSummary work) {
            new AlertDialog.Builder(context)
                    .setTitle("🗑️ Delete Work Report")
                    .setMessage("Are you sure you want to delete today's work report?\n\nThis action cannot be undone.")
                    .setPositiveButton("Yes, Delete", (dialog, which) -> deleteWork(work))
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void deleteWork(WorkSummary work) {
            String companyKey = pref.getCompanyKey();
            String employeeMobile = pref.getEmployeeMobile();

            if (TextUtils.isEmpty(companyKey) || TextUtils.isEmpty(employeeMobile)) {
                Toast.makeText(context, "⚠️ Session error. Please login again.", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseDatabase.getInstance()
                    .getReference("Companies")
                    .child(companyKey)
                    .child("dailyWork")
                    .child(work.workDate)
                    .child(employeeMobile)
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "✅ Work report deleted successfully", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onDeleteSuccess();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "❌ Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}