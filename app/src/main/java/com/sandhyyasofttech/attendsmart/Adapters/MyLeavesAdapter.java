package com.sandhyyasofttech.attendsmart.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sandhyyasofttech.attendsmart.Models.LeaveModel;
import com.sandhyyasofttech.attendsmart.R;

import java.util.List;

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

        h.tvDates.setText(m.fromDate + " → " + m.toDate);
        h.tvType.setText(m.leaveType +
                (m.halfDayType != null ? " (" + m.halfDayType + ")" : ""));

        h.tvStatus.setText(m.status);

        if ("REJECTED".equals(m.status) && m.adminReason != null) {
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

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDates, tvType, tvStatus, tvReason;
        VH(View v) {
            super(v);
            tvDates = v.findViewById(R.id.tvDates);
            tvType = v.findViewById(R.id.tvType);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvReason = v.findViewById(R.id.tvReason);
        }
    }
}
