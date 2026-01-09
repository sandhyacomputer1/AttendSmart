package com.sandhyyasofttech.attendsmart.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.FirebaseDatabase;
import com.sandhyyasofttech.attendsmart.Activities.EmployeeTodayWorkActivity;
import com.sandhyyasofttech.attendsmart.Models.WorkSummary;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmployeeWorkAdapter extends RecyclerView.Adapter<EmployeeWorkAdapter.ViewHolder> {
    private List<WorkSummary> works = new ArrayList<>();
    private Context context;
    private PrefManager pref;
    private String todayDate;

    public EmployeeWorkAdapter(Context context) {
        this.context = context;
        this.pref = new PrefManager(context);
        this.todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_employee_work, parent, false);
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
        TextView tvEmployeeName, tvWorkSummary, tvTasks, tvIssues, tvSubmittedTime, tvDate;
        MaterialButton btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
            tvWorkSummary = itemView.findViewById(R.id.tvWorkSummary);
            tvTasks = itemView.findViewById(R.id.tvTasks);
            tvIssues = itemView.findViewById(R.id.tvIssues);
            tvSubmittedTime = itemView.findViewById(R.id.tvSubmittedTime);
            tvDate = itemView.findViewById(R.id.tvDate);  // ✅ NOW EXISTS!
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(WorkSummary work) {
            // 🔥 SAFE NULL CHECKS - NO CRASH!
            if (tvEmployeeName != null)
                tvEmployeeName.setText(work.employeeName != null ? work.employeeName : "Unknown");
            if (tvWorkSummary != null)
                tvWorkSummary.setText(work.workSummary != null ? work.workSummary : "No summary");
            if (tvTasks != null)
                tvTasks.setText("Tasks: " + (work.tasks != null && !work.tasks.isEmpty() ? work.tasks : "None"));
            if (tvIssues != null)
                tvIssues.setText("Issues: " + (work.issues != null && !work.issues.isEmpty() ? work.issues : "None"));
            if (tvSubmittedTime != null)
                tvSubmittedTime.setText("Submitted: " + safeFormatTime(work.submittedAt));
            if (tvDate != null)
                tvDate.setText(work.workDate != null ? work.workDate : "No date");

            // Today highlight
            if (work.workDate != null && work.workDate.equals(todayDate)) {
                itemView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light, null));
            } else {
                itemView.setBackgroundColor(0);
            }

            if (btnEdit != null) btnEdit.setOnClickListener(v -> openEdit(work));
            if (btnDelete != null) btnDelete.setOnClickListener(v -> deleteWork(work));
        }

        private void openEdit(WorkSummary work) {
            if (work.workDate != null && work.workDate.equals(todayDate)) {
                Intent intent = new Intent(context, EmployeeTodayWorkActivity.class);
                intent.putExtra("editMode", true);
                intent.putExtra("workData", work);
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "✏️ फक्त आजचाच edit होऊ शकतो", Toast.LENGTH_SHORT).show();
            }
        }

        private void deleteWork(WorkSummary work) {
            if (work.workDate == null || !work.workDate.equals(todayDate)) {
                Toast.makeText(context, "🗑️ फक्त आजचाच delete होऊ शकतो", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(context)
                    .setTitle("🗑️ Delete Work")
                    .setMessage("आजचा work delete कराल?")
                    .setPositiveButton("हो", (d, w) -> {
                        String companyKey = pref.getCompanyKey();
                        String employeeMobile = pref.getEmployeeMobile();

                        FirebaseDatabase.getInstance().getReference("Companies")
                                .child(companyKey)
                                .child("dailyWork")
                                .child(work.workDate)
                                .child(employeeMobile)
                                .removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    works.remove(work);
                                    notifyItemRemoved(getAdapterPosition());
                                    Toast.makeText(context, "✅ Work deleted!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(context, "❌ Delete failed", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("नाही", null)
                    .show();
        }

        private String safeFormatTime(long timestamp) {
            try {
                return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(Math.max(timestamp, 0)));
            } catch (Exception e) {
                return "Unknown time";
            }
        }
    }


    // 🔥 SINGLE WORK SUPPORT - EmployeeMyWorksActivity साठी
    public void updateWork(WorkSummary work) {
        this.works.clear();
        if (work != null) {
            this.works.add(work);
        }
        notifyDataSetChanged();
    }

}
