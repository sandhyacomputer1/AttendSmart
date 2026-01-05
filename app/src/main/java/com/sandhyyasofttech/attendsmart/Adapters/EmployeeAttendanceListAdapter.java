package com.sandhyyasofttech.attendsmart.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
import com.sandhyyasofttech.attendsmart.R;

import java.util.List;

public class EmployeeAttendanceListAdapter
        extends RecyclerView.Adapter<EmployeeAttendanceListAdapter.VH> {

    public interface ClickListener {
        void onEmployeeClick(EmployeeModel employee);
    }

    private final List<EmployeeModel> list;
    private final ClickListener listener;

    public EmployeeAttendanceListAdapter(List<EmployeeModel> list, ClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee_attendance, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        EmployeeModel e = list.get(i);

        h.tvName.setText(e.getEmployeeName());
        h.tvDept.setText(e.getEmployeeDepartment());
        h.tvMobile.setText(e.getEmployeeMobile());

        h.itemView.setOnClickListener(v -> listener.onEmployeeClick(e));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvDept, tvMobile;

        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvDept = v.findViewById(R.id.tvDepartment);
            tvMobile = v.findViewById(R.id.tvMobile);
        }
    }
}
