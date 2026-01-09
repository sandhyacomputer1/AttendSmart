package com.sandhyyasofttech.attendsmart.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sandhyyasofttech.attendsmart.Models.TodayWorkModel;
import com.sandhyyasofttech.attendsmart.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TodayWorkAdapter extends RecyclerView.Adapter<TodayWorkAdapter.VH> {

    private ArrayList<TodayWorkModel> list;

    public TodayWorkAdapter(ArrayList<TodayWorkModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_today_work, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        TodayWorkModel m = list.get(position);

        h.tvName.setText(m.getEmployeeName());
        h.tvSummary.setText(m.getWorkSummary());

        // Tasks visibility
        if (m.getTasks() != null && !m.getTasks().trim().isEmpty()) {
            h.tvTasks.setText("📋 " + m.getTasks());
            h.tvTasks.setVisibility(View.VISIBLE);
        } else {
            h.tvTasks.setVisibility(View.GONE);
        }

        // Issues visibility + color
        if (m.getIssues() != null && !m.getIssues().trim().isEmpty()) {
            h.tvIssues.setText("⚠️ " + m.getIssues());
            h.tvIssues.setVisibility(View.VISIBLE);
            h.itemView.findViewById(R.id.ivWarning).setVisibility(View.VISIBLE);
        } else {
            h.tvIssues.setVisibility(View.GONE);
            h.itemView.findViewById(R.id.ivWarning).setVisibility(View.GONE);
        }

        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(new Date(m.getSubmittedAt()));
        h.tvTime.setText(time);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvSummary, tvTasks, tvIssues, tvTime;

        VH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvEmployeeName);
            tvSummary = v.findViewById(R.id.tvSummary);
            tvTasks = v.findViewById(R.id.tvTasks);
            tvIssues = v.findViewById(R.id.tvIssues);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
}
