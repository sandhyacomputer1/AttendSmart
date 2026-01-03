package com.sandhyyasofttech.attendsmart.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sandhyyasofttech.attendsmart.Models.SalarySnapshot;
import com.sandhyyasofttech.attendsmart.R;

import java.util.List;

public class SalaryListAdapter
        extends RecyclerView.Adapter<SalaryListAdapter.VH> {

    public interface OnSalaryClick {
        void onClick(SalarySnapshot s);
    }

    private final List<SalarySnapshot> list;
    private final OnSalaryClick listener;

    public SalaryListAdapter(List<SalarySnapshot> list,
                             OnSalaryClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_salary, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SalarySnapshot s = list.get(position);

        h.tvEmployee.setText("Employee: " + s.employeeMobile);
        h.tvMonth.setText("Month: " + s.month);
        h.tvNetSalary.setText("Net Salary: ₹" +
                s.calculationResult.netSalary);
        h.tvStatus.setText("Generated");

        h.itemView.setOnClickListener(v -> listener.onClick(s));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView tvEmployee, tvMonth, tvNetSalary, tvStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            tvEmployee = itemView.findViewById(R.id.tvEmployee);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvNetSalary = itemView.findViewById(R.id.tvNetSalary);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
