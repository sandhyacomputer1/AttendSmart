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
//import com.sandhyyasofttech.attendsmart.Models.SalarySnapshot;
//import com.sandhyyasofttech.attendsmart.R;
//
//import java.util.List;
//
//public class SalaryListAdapter
//        extends RecyclerView.Adapter<SalaryListAdapter.VH> {
//
//    public interface OnSalaryClick {
//        void onClick(SalarySnapshot s);
//    }
//
//    private final List<SalarySnapshot> list;
//    private final OnSalaryClick listener;
//
//    public SalaryListAdapter(List<SalarySnapshot> list,
//                             OnSalaryClick listener) {
//        this.list = list;
//        this.listener = listener;
//    }
//
//    @NonNull
//    @Override
//    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View v = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_salary, parent, false);
//        return new VH(v);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull VH h, int position) {
//        SalarySnapshot s = list.get(position);
//
//        h.tvEmployee.setText("Employee: " + s.employeeMobile);
//        h.tvMonth.setText("Month: " + s.month);
//        h.tvNetSalary.setText("Net Salary: ₹" +
//                s.calculationResult.netSalary);
//        h.tvStatus.setText("Generated");
//
//        h.itemView.setOnClickListener(v -> listener.onClick(s));
//    }
//
//    @Override
//    public int getItemCount() {
//        return list.size();
//    }
//
//    static class VH extends RecyclerView.ViewHolder {
//
//        TextView tvEmployee, tvMonth, tvNetSalary, tvStatus;
//
//        VH(@NonNull View itemView) {
//            super(itemView);
//            tvEmployee = itemView.findViewById(R.id.tvEmployee);
//            tvMonth = itemView.findViewById(R.id.tvMonth);
//            tvNetSalary = itemView.findViewById(R.id.tvNetSalary);
//            tvStatus = itemView.findViewById(R.id.tvStatus);
//        }
//    }
//}



package com.sandhyyasofttech.attendsmart.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Models.SalarySnapshot;
import com.sandhyyasofttech.attendsmart.R;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SalaryListAdapter extends RecyclerView.Adapter<SalaryListAdapter.VH> {

    public interface OnSalaryClick {
        void onClick(SalarySnapshot s);
    }

    private final List<SalarySnapshot> list;
    private final OnSalaryClick listener;
    private final Context context;
    private final String companyKey;
    private final Map<String, EmployeeInfo> employeeCache = new HashMap<>();
    private final NumberFormat currencyFormat;

    public SalaryListAdapter(List<SalarySnapshot> list,
                             OnSalaryClick listener,
                             Context context,
                             String companyKey) {
        this.list = list;
        this.listener = listener;
        this.context = context;
        this.companyKey = companyKey;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
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

        // Set basic information first
        h.tvMonth.setText(s.month);

        // Format net salary
        double netSalary = parseSalaryValue(s.calculationResult.netSalary);
        h.tvNetSalary.setText(currencyFormat.format(netSalary));

        // Set loading text for employee info
        h.tvEmployeeName.setText("Loading...");
        h.tvEmployeeId.setText("ID: ...");
        h.tvEmployeeMobile.setText(s.employeeMobile);

        // Fetch employee details
        fetchEmployeeDetails(s.employeeMobile, h);

        // Set status
        if (netSalary > 0) {
            h.tvStatus.setText("Paid");
            h.tvStatus.setBackgroundResource(R.drawable.badge_paid);
        } else {
            h.tvStatus.setText("Pending");
            h.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
        }

        h.itemView.setOnClickListener(v -> listener.onClick(s));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private void fetchEmployeeDetails(String employeeMobile, VH holder) {
        // Check cache first
        if (employeeCache.containsKey(employeeMobile)) {
            EmployeeInfo info = employeeCache.get(employeeMobile);
            holder.tvEmployeeName.setText(info.name);
            holder.tvEmployeeId.setText("ID: " + info.id);
            return;
        }

        DatabaseReference employeeRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees")
                .child(employeeMobile)
                .child("info");

        employeeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String empName = snapshot.child("employeeName").getValue(String.class);
                    String empId = snapshot.child("employeeId").getValue(String.class);

                    // Set default values if null
                    if (empName == null) empName = "Unknown Employee";
                    if (empId == null) empId = "N/A";

                    // Update cache
                    employeeCache.put(employeeMobile, new EmployeeInfo(empName, empId));

                    // Update UI
                    holder.tvEmployeeName.setText(empName);
                    holder.tvEmployeeId.setText("ID: " + empId);
                } else {
                    holder.tvEmployeeName.setText("Employee Not Found");
                    holder.tvEmployeeId.setText("ID: N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.tvEmployeeName.setText("Error Loading");
                holder.tvEmployeeId.setText("ID: N/A");
            }
        });
    }

    private double parseSalaryValue(Object salaryValue) {
        if (salaryValue == null) return 0.0;
        try {
            if (salaryValue instanceof String) {
                String str = ((String) salaryValue).replaceAll("[₹$,]", "").trim();
                return str.isEmpty() ? 0.0 : Double.parseDouble(str);
            } else if (salaryValue instanceof Number) {
                return ((Number) salaryValue).doubleValue();
            }
        } catch (Exception e) {
            return 0.0;
        }
        return 0.0;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEmployeeName, tvEmployeeId, tvEmployeeMobile, tvMonth, tvNetSalary, tvStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
            tvEmployeeId = itemView.findViewById(R.id.tvEmployeeId);
            tvEmployeeMobile = itemView.findViewById(R.id.tvEmployeeMobile);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvNetSalary = itemView.findViewById(R.id.tvNetSalary);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }

    private static class EmployeeInfo {
        String name;
        String id;

        EmployeeInfo(String name, String id) {
            this.name = name;
            this.id = id;
        }
    }
}