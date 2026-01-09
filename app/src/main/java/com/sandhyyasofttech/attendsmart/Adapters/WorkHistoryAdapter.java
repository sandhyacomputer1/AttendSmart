package com.sandhyyasofttech.attendsmart.Adapters;

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
import com.sandhyyasofttech.attendsmart.Activities.EmployeeTodayWorkActivity;
import com.sandhyyasofttech.attendsmart.Models.WorkSummary;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkHistoryAdapter extends RecyclerView.Adapter<WorkHistoryAdapter.ViewHolder> {
    private List<WorkSummary> works = new ArrayList<>();
    private Context context;
    private String todayDate;

    public WorkHistoryAdapter(Context context) {
        this.context = context;
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee_work, parent, false);
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
        TextView tvDate, tvSummary, tvTime, tvEmployeeName, tvTasks, tvIssues;
        MaterialButton btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            // 🔥 ALL VIEWS INITIALIZE केले
            tvDate = itemView.findViewById(R.id.tvDate);
            tvSummary = itemView.findViewById(R.id.tvWorkSummary);
            tvTime = itemView.findViewById(R.id.tvSubmittedTime);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
            tvTasks = itemView.findViewById(R.id.tvTasks);
            tvIssues = itemView.findViewById(R.id.tvIssues);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            // 🔥 CLICK LISTENERS
            btnEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    WorkSummary work = works.get(position);
                    if (work.workDate != null && work.workDate.equals(todayDate)) {
                        openEdit(work);
                    } else {
                        Toast.makeText(itemView.getContext(), "✏️ फक्त आजचाच edit होऊ शकतो", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    WorkSummary work = works.get(position);
                    if (work.workDate != null && work.workDate.equals(todayDate)) {
                        deleteWork(work);
                    } else {
                        Toast.makeText(itemView.getContext(), "🗑️ फक्त आजचाच delete होऊ शकतो", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        void bind(WorkSummary work) {
            tvDate.setText(work.workDate);
            tvSummary.setText(work.workSummary);
            tvEmployeeName.setText(work.employeeName);
            tvTasks.setText("Tasks: " + (work.tasks.isEmpty() ? "None" : work.tasks));
            tvIssues.setText("Issues: " + (work.issues.isEmpty() ? "None" : work.issues));
            tvTime.setText("Submitted: " + formatTime(work.submittedAt));

            // Today highlight
            if (work.workDate != null && work.workDate.equals(todayDate)) {
                itemView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light));
            }
        }

        private void openEdit(WorkSummary work) {
            Intent intent = new Intent(context, EmployeeTodayWorkActivity.class);
            intent.putExtra("editMode", true);
            intent.putExtra("workData", work);
            context.startActivity(intent);
        }

        private void deleteWork(WorkSummary work) {
            PrefManager pref = new PrefManager(context);
            String companyKey = pref.getCompanyKey();
            String employeeMobile = pref.getEmployeeMobile();

            // Firebase delete
            com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("Companies")
                    .child(companyKey)
                    .child("dailyWork")
                    .child(work.workDate)
                    .child(employeeMobile)
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {
                        works.remove(work);
                        notifyDataSetChanged();
                        Toast.makeText(context, "✅ Work deleted!", Toast.LENGTH_SHORT).show();
                    });
        }

        private String formatTime(long timestamp) {
            return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(timestamp));
        }
    }
}
