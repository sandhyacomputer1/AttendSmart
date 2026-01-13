package com.sandhyyasofttech.attendsmart.Adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.sandhyyasofttech.attendsmart.Models.WorkHistoryModel;
import com.sandhyyasofttech.attendsmart.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkHistoryAdapter extends RecyclerView.Adapter<WorkHistoryAdapter.ViewHolder> {

    private List<WorkHistoryModel> workList;

    public WorkHistoryAdapter(List<WorkHistoryModel> workList) {
        this.workList = workList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_work_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkHistoryModel work = workList.get(position);

        // Format and display date
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, EEEE", Locale.getDefault());
            Date date = inputFormat.parse(work.getWorkDate());
            holder.tvDate.setText(" " + outputFormat.format(date));
        } catch (Exception e) {
            holder.tvDate.setText(" " + work.getWorkDate());
        }

        // Display submission time
        if (work.getSubmittedAt() > 0) {
            String time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                    .format(new Date(work.getSubmittedAt()));
            holder.tvSubmissionTime.setText("Submitted at: " + time);
            holder.tvSubmissionTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvSubmissionTime.setVisibility(View.GONE);
        }

        // Display completed work
        if (!TextUtils.isEmpty(work.getCompletedWork())) {
            holder.tvCompletedWork.setText(work.getCompletedWork());
            holder.layoutCompleted.setVisibility(View.VISIBLE);
        } else {
            holder.layoutCompleted.setVisibility(View.GONE);
        }

        // Display ongoing work
        if (!TextUtils.isEmpty(work.getOngoingWork())) {
            holder.tvOngoingWork.setText(work.getOngoingWork());
            holder.layoutOngoing.setVisibility(View.VISIBLE);
        } else {
            holder.layoutOngoing.setVisibility(View.GONE);
        }

        // Display tomorrow's work
        if (!TextUtils.isEmpty(work.getTomorrowWork())) {
            holder.tvTomorrowWork.setText(work.getTomorrowWork());
            holder.layoutTomorrow.setVisibility(View.VISIBLE);
        } else {
            holder.layoutTomorrow.setVisibility(View.GONE);
        }

        // Check if this is today's date
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if (work.getWorkDate().equals(todayDate)) {
            holder.tvDate.append(" • TODAY");
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.light_blue));
        } else {
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext()
                    .getResources().getColor(android.R.color.white));
        }
    }

    @Override
    public int getItemCount() {
        return workList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvDate, tvSubmissionTime;
        View layoutCompleted, layoutOngoing, layoutTomorrow;
        TextView tvCompletedWork, tvOngoingWork, tvTomorrowWork;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            tvDate = itemView.findViewById(R.id.tvDate);
            tvSubmissionTime = itemView.findViewById(R.id.tvSubmissionTime);
            
            layoutCompleted = itemView.findViewById(R.id.layoutCompleted);
            layoutOngoing = itemView.findViewById(R.id.layoutOngoing);
            layoutTomorrow = itemView.findViewById(R.id.layoutTomorrow);
            
            tvCompletedWork = itemView.findViewById(R.id.tvCompletedWork);
            tvOngoingWork = itemView.findViewById(R.id.tvOngoingWork);
            tvTomorrowWork = itemView.findViewById(R.id.tvTomorrowWork);
        }
    }
}