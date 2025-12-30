package com.sandhyyasofttech.attendsmart.Adapters;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.sandhyyasofttech.attendsmart.Models.EmployeePunchModel;
import com.sandhyyasofttech.attendsmart.R;

import java.util.List;

public class PunchDetailsAdapter extends RecyclerView.Adapter<PunchDetailsAdapter.ViewHolder> {

    private static final String TAG = "PunchDetailsAdapter";
    private List<EmployeePunchModel> list;

    public PunchDetailsAdapter(List<EmployeePunchModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_punch_details, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        EmployeePunchModel m = list.get(position);

        // Set employee info
        h.tvEmployeeName.setText(m.employeeName != null ? m.employeeName : "Employee");
        h.tvEmployeeMobile.setText(m.mobile != null ? m.mobile : "N/A");

        // Set late badge and card background
        if (m.isLate) {
            h.tvLateTag.setVisibility(View.VISIBLE);
            h.tvLateTag.setText("LATE");
            h.cardView.setCardBackgroundColor(Color.parseColor("#FFF8E1")); // Light yellow
        } else if (m.status != null && m.status.equalsIgnoreCase("On Time")) {
            h.tvLateTag.setVisibility(View.VISIBLE);
            h.tvLateTag.setText("ON TIME");
            h.tvLateTag.setBackgroundResource(R.drawable.bg_ontime_badge);
            h.cardView.setCardBackgroundColor(Color.WHITE);
        } else {
            h.tvLateTag.setVisibility(View.GONE);
            h.cardView.setCardBackgroundColor(Color.WHITE);
        }

        // Check-in time
        if (m.checkInTime != null && !m.checkInTime.isEmpty()) {
            h.tvInTime.setText(m.checkInTime);
            h.tvInTime.setTextColor(Color.parseColor("#212121"));
        } else {
            h.tvInTime.setText("Not checked in");
            h.tvInTime.setTextColor(Color.parseColor("#F44336"));
        }

        // Check-in location
        if (m.checkInAddress != null && !m.checkInAddress.isEmpty()) {
            h.tvInLoc.setText(m.checkInAddress);
            h.tvInLoc.setVisibility(View.VISIBLE);
        } else {
            h.tvInLoc.setText("Location not available");
            h.tvInLoc.setVisibility(View.VISIBLE);
        }

        // Check-out time
        if (m.checkOutTime != null && !m.checkOutTime.isEmpty()) {
            h.tvOutTime.setText(m.checkOutTime);
            h.tvOutTime.setTextColor(Color.parseColor("#212121"));
        } else {
            h.tvOutTime.setText("Not checked out");
            h.tvOutTime.setTextColor(Color.parseColor("#F44336"));
        }

        // Check-out location
        if (m.checkOutAddress != null && !m.checkOutAddress.isEmpty()) {
            h.tvOutLoc.setText(m.checkOutAddress);
            h.tvOutLoc.setVisibility(View.VISIBLE);
        } else {
            h.tvOutLoc.setText("Location not available");
            h.tvOutLoc.setVisibility(View.VISIBLE);
        }

        // Working hours
        if (m.workingHours != null && !m.workingHours.isEmpty()) {
            h.layoutWorkingHours.setVisibility(View.VISIBLE);
            h.tvWorkingHours.setText(m.workingHours);
        } else {
            h.layoutWorkingHours.setVisibility(View.GONE);
        }

        // Load check-in image
        if (m.checkInPhoto != null && !m.checkInPhoto.isEmpty()) {
            h.ivIn.setVisibility(View.VISIBLE);

            Log.d(TAG, "Loading check-in image: " + m.checkInPhoto);

            Glide.with(h.itemView.getContext())
                    .load(m.checkInPhoto)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(h.ivIn);

            h.ivIn.setOnClickListener(v -> openImage(m.checkInPhoto, h.itemView));
        } else {
            h.ivIn.setVisibility(View.GONE);
            Log.d(TAG, "No check-in photo for: " + m.mobile);
        }

        // Load check-out image
        if (m.checkOutPhoto != null && !m.checkOutPhoto.isEmpty()) {
            h.ivOut.setVisibility(View.VISIBLE);

            Log.d(TAG, "Loading check-out image: " + m.checkOutPhoto);

            Glide.with(h.itemView.getContext())
                    .load(m.checkOutPhoto)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(h.ivOut);

            h.ivOut.setOnClickListener(v -> openImage(m.checkOutPhoto, h.itemView));
        } else {
            h.ivOut.setVisibility(View.GONE);
            Log.d(TAG, "No check-out photo for: " + m.mobile);
        }
    }

    private void openImage(String url, View view) {
        if (url != null && !url.isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(url), "image/*");
                view.getContext().startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening image: " + e.getMessage());
                // Fallback to browser
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    view.getContext().startActivity(browserIntent);
                } catch (Exception ex) {
                    Log.e(TAG, "Error opening in browser: " + ex.getMessage());
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvEmployeeName, tvEmployeeMobile, tvLateTag;
        TextView tvInTime, tvOutTime, tvInLoc, tvOutLoc;
        TextView tvWorkingHours;
        ImageView ivIn, ivOut;
        LinearLayout layoutWorkingHours;

        ViewHolder(@NonNull View v) {
            super(v);
            cardView = v.findViewById(R.id.cardView);
            tvEmployeeName = v.findViewById(R.id.tvEmployeeName);
            tvEmployeeMobile = v.findViewById(R.id.tvEmployeeMobile);
            tvLateTag = v.findViewById(R.id.tvLateTag);
            tvInTime = v.findViewById(R.id.tvCheckInTime);
            tvOutTime = v.findViewById(R.id.tvCheckOutTime);
            tvInLoc = v.findViewById(R.id.tvCheckInLoc);
            tvOutLoc = v.findViewById(R.id.tvCheckOutLoc);
            tvWorkingHours = v.findViewById(R.id.tvWorkingHours);
            ivIn = v.findViewById(R.id.ivCheckIn);
            ivOut = v.findViewById(R.id.ivCheckOut);
            layoutWorkingHours = v.findViewById(R.id.layoutWorkingHours);
        }
    }
}