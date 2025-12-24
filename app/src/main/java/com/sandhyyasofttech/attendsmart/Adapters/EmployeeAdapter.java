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

public class EmployeeAdapter
        extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private final List<EmployeeModel> employeeList;

    public EmployeeAdapter(List<EmployeeModel> employeeList) {
        this.employeeList = employeeList;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee, parent, false);

        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull EmployeeViewHolder holder, int position) {

        EmployeeModel model = employeeList.get(position);

        holder.tvName.setText(model.getEmployeeName());
        holder.tvMobile.setText(model.getEmployeeMobile());
        holder.tvRole.setText(model.getEmployeeRole());
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }

    static class EmployeeViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvMobile, tvRole;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvEmpName);
            tvMobile = itemView.findViewById(R.id.tvEmpMobile);
            tvRole = itemView.findViewById(R.id.tvEmpRole);
        }
    }
}
