package com.sandhyyasofttech.attendsmart.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sandhyyasofttech.attendsmart.Models.EmployeeWorkStatus;
import com.sandhyyasofttech.attendsmart.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmployeeWorkStatusAdapter extends RecyclerView.Adapter<EmployeeWorkStatusAdapter.ViewHolder> {

    private List<EmployeeWorkStatus> employeeList;
    private OnEmployeeClickListener listener;

    public interface OnEmployeeClickListener {
        void onEmployeeClick(EmployeeWorkStatus employee);
    }

    public EmployeeWorkStatusAdapter(List<EmployeeWorkStatus> employeeList, OnEmployeeClickListener listener) {
        this.employeeList = employeeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee_work_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmployeeWorkStatus employee = employeeList.get(position);

        holder.tvName.setText(employee.getEmployeeName());
        holder.tvDepartment.setText(employee.getEmployeeDepartment() != null ? 
                employee.getEmployeeDepartment() : "N/A");
        holder.tvMobile.setText(employee.getEmployeeMobile());

        // Load profile image
        if (employee.getProfileImage() != null && !employee.getProfileImage().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(employee.getProfileImage())
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(R.drawable.ic_person);
        }

        // Set submission status
        if (employee.isHasSubmitted()) {
            holder.tvStatus.setText(" Submitted");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.light_green));
            
            // Show submission time
            if (employee.getSubmittedAt() > 0) {
                String time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                        .format(new Date(employee.getSubmittedAt()));
                holder.tvSubmissionTime.setVisibility(View.VISIBLE);
                holder.tvSubmissionTime.setText("at " + time);
            } else {
                holder.tvSubmissionTime.setVisibility(View.GONE);
            }
        } else {
            holder.tvStatus.setText(" Not Submitted");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark));
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.light_orange));
            holder.tvSubmissionTime.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEmployeeClick(employee);
            }
        });
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivProfile;
        TextView tvName, tvDepartment, tvMobile, tvStatus, tvSubmissionTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvName = itemView.findViewById(R.id.tvName);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvMobile = itemView.findViewById(R.id.tvMobile);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvSubmissionTime = itemView.findViewById(R.id.tvSubmissionTime);
        }
    }
}