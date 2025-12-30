package com.sandhyyasofttech.attendsmart.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sandhyyasofttech.attendsmart.Models.ShiftModel;
import com.sandhyyasofttech.attendsmart.R;

import java.util.List;

public class ShiftAdapter extends RecyclerView.Adapter<ShiftAdapter.ViewHolder> {

    private final List<ShiftModel> shiftList;

    public ShiftAdapter(List<ShiftModel> shiftList) {
        this.shiftList = shiftList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shift, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShiftModel model = shiftList.get(position);
        holder.tvShiftName.setText(model.name);
        holder.tvShiftTime.setText(model.startTime + " - " + model.endTime);
    }

    @Override
    public int getItemCount() {
        return shiftList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvShiftName, tvShiftTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvShiftName = itemView.findViewById(R.id.tvShiftName);
            tvShiftTime = itemView.findViewById(R.id.tvShiftTime);
        }
    }
}
