//package com.sandhyyasofttech.attendsmart.Adapters;
//
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.google.android.material.button.MaterialButton;
//import com.sandhyyasofttech.attendsmart.Models.Employee;
//import com.sandhyyasofttech.attendsmart.R;
//
//import java.util.List;
//
//public class EmployeeSalaryAdapter extends RecyclerView.Adapter<EmployeeSalaryAdapter.ViewHolder> {
//
//    private List<Employee> employeeList;
//    private OnEmployeeClickListener listener;
//
//    public interface OnEmployeeClickListener {
//        void onEmployeeClick(Employee employee);
//    }
//
//    public EmployeeSalaryAdapter(List<Employee> employeeList, OnEmployeeClickListener listener) {
//        this.employeeList = employeeList;
//        this.listener = listener;
//    }
//
//    @NonNull
//    @Override
//    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_employee_salary, parent, false);
//        return new ViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//        Employee employee = employeeList.get(position);
//
//        holder.tvEmployeeName.setText(employee.getName());
//        holder.tvEmployeeMobile.setText(employee.getMobile());
//
//        if (employee.isHasSalaryConfig()) {
//            holder.tvSalaryStatus.setText("Salary: ₹" + employee.getMonthlySalary() + "/month");
//            holder.tvSalaryStatus.setTextColor(holder.itemView.getContext()
//                    .getResources().getColor(android.R.color.holo_green_dark));
//            holder.btnConfigureSalary.setText("Edit");
//        } else {
//            holder.tvSalaryStatus.setText("Salary: Not Configured");
//            holder.tvSalaryStatus.setTextColor(holder.itemView.getContext()
//                    .getResources().getColor(android.R.color.holo_red_dark));
//            holder.btnConfigureSalary.setText("Configure");
//        }
//
//        holder.btnConfigureSalary.setOnClickListener(v -> {
//            if (listener != null) {
//                listener.onEmployeeClick(employee);
//            }
//        });
//    }
//
//    @Override
//    public int getItemCount() {
//        return employeeList.size();
//    }
//
//    static class ViewHolder extends RecyclerView.ViewHolder {
//        TextView tvEmployeeName, tvEmployeeMobile, tvSalaryStatus;
//        MaterialButton btnConfigureSalary;
//
//        ViewHolder(@NonNull View itemView) {
//            super(itemView);
//            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
//            tvEmployeeMobile = itemView.findViewById(R.id.tvEmployeeMobile);
//            tvSalaryStatus = itemView.findViewById(R.id.tvSalaryStatus);
//            btnConfigureSalary = itemView.findViewById(R.id.btnConfigureSalary);
//        }
//    }
//}


package com.sandhyyasofttech.attendsmart.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sandhyyasofttech.attendsmart.Models.Employee;
import com.sandhyyasofttech.attendsmart.R;

import java.util.List;

public class EmployeeSalaryAdapter extends RecyclerView.Adapter<EmployeeSalaryAdapter.ViewHolder> {

    private List<Employee> employeeList;
    private OnEmployeeClickListener listener;

    public interface OnEmployeeClickListener {
        void onEmployeeClick(Employee employee);
    }

    public EmployeeSalaryAdapter(List<Employee> employeeList, OnEmployeeClickListener listener) {
        this.employeeList = employeeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee_salary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Employee employee = employeeList.get(position);

        holder.tvEmployeeName.setText(employee.getName());
        holder.tvEmployeeMobile.setText("Mobile: " + employee.getMobile());

        // Check if department TextView exists
        if (holder.tvEmployeeDepartment != null) {
            holder.tvEmployeeDepartment.setText("Department: " + employee.getDepartment());
            holder.tvEmployeeDepartment.setVisibility(View.VISIBLE);
        }

        if (employee.isHasSalaryConfig()) {
            holder.tvSalaryStatus.setText("Salary: ₹" + employee.getMonthlySalary() + "/month");
            holder.tvSalaryStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(android.R.color.holo_green_dark));
            holder.btnConfigureSalary.setText("Edit");
        } else {
            holder.tvSalaryStatus.setText("Salary: Not Configured");
            holder.tvSalaryStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(android.R.color.holo_red_dark));
            holder.btnConfigureSalary.setText("Configure");
        }

        holder.btnConfigureSalary.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEmployeeClick(employee);
            }
        });
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmployeeName, tvEmployeeMobile, tvSalaryStatus, tvEmployeeDepartment;
        MaterialButton btnConfigureSalary;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
            tvEmployeeMobile = itemView.findViewById(R.id.tvEmployeeMobile);
            tvSalaryStatus = itemView.findViewById(R.id.tvSalaryStatus);
            btnConfigureSalary = itemView.findViewById(R.id.btnConfigureSalary);

            // Try to find department TextView (might not exist in old layout)
            tvEmployeeDepartment = itemView.findViewById(R.id.tvEmployeeDepartment);
        }
    }
}