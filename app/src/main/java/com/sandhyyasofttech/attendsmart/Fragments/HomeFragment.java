package com.sandhyyasofttech.attendsmart.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Activities.DepartmentActivity;
import com.sandhyyasofttech.attendsmart.Activities.ShiftActivity;
import com.sandhyyasofttech.attendsmart.Adapters.EmployeeAdapter;
import com.sandhyyasofttech.attendsmart.Admin.AddEmployeeActivity;
import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvTotalEmployees, tvPresent, tvAbsent, tvLate;
    private TextView tvDepartmentCount, tvShiftCount;
    private MaterialButton btnManageDepartments, btnManageShifts, btnViewAllEmployees;
    private RecyclerView rvEmployees;
    private ExtendedFloatingActionButton fabAddEmployee;
    private ArrayList<EmployeeModel> employeeList;
    private EmployeeAdapter adapter;
    private String companyKey;
    private DatabaseReference employeesRef, departmentsRef, shiftsRef, attendanceRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initViews(view);
        setupFirebase();
        setupRecyclerView();
        setupClickListeners();
        loadDashboardData();
        return view;
    }

    private void initViews(View view) {
        tvTotalEmployees = view.findViewById(R.id.tvTotalEmployees);
        tvPresent = view.findViewById(R.id.tvPresent);
        tvAbsent = view.findViewById(R.id.tvAbsent);
        tvLate = view.findViewById(R.id.tvLate);
        tvDepartmentCount = view.findViewById(R.id.tvDepartmentCount);
        tvShiftCount = view.findViewById(R.id.tvShiftCount);
        btnManageDepartments = view.findViewById(R.id.btnManageDepartments);
        btnManageShifts = view.findViewById(R.id.btnManageShifts);
        btnViewAllEmployees = view.findViewById(R.id.btnViewAllEmployees);
        rvEmployees = view.findViewById(R.id.rvEmployees);
        fabAddEmployee = view.findViewById(R.id.fabAddEmployee);
    }

    private void setupFirebase() {
        PrefManager pref = new PrefManager(requireContext());
        String email = pref.getUserEmail();
        if (email == null) return;
        companyKey = email.replace(".", ",");

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference companyRef = db.getReference("Companies").child(companyKey);
        employeesRef = companyRef.child("employees");
        departmentsRef = companyRef.child("departments");
        shiftsRef = companyRef.child("shifts");
        attendanceRef = companyRef.child("attendance");
    }

    private void setupRecyclerView() {
        employeeList = new ArrayList<>();
        adapter = new EmployeeAdapter(employeeList);
        rvEmployees.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEmployees.setHasFixedSize(true);
        rvEmployees.setAdapter(adapter);
    }

    private void setupClickListeners() {
        fabAddEmployee.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddEmployeeActivity.class)));
        btnManageDepartments.setOnClickListener(v -> startActivity(new Intent(requireContext(), DepartmentActivity.class)));
        btnManageShifts.setOnClickListener(v -> startActivity(new Intent(requireContext(), ShiftActivity.class)));
        btnViewAllEmployees.setOnClickListener(v -> Toast.makeText(requireContext(), "View All Employees", Toast.LENGTH_SHORT).show());
    }

    private void loadDashboardData() {
        loadEmployeeStats();
        loadDepartmentCount();
        loadShiftCount();
        loadEmployeeList();
    }

    private void loadEmployeeStats() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int total = (int) snapshot.getChildrenCount();
                tvTotalEmployees.setText(String.valueOf(total));

                attendanceRef.child(today).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot attSnapshot) {
                        int present = 0, late = 0;
                        for (DataSnapshot att : attSnapshot.getChildren()) {
                            String status = att.child("status").getValue(String.class);
                            if (status != null) {
                                if (status.contains("Present") || status.contains("Full Day")) present++;
                                else if (status.contains("Late") || status.contains("Half Day")) late++;
                            }
                        }
                        int absent = total - (present + late);
                        tvPresent.setText(String.valueOf(present));
                        tvLate.setText(String.valueOf(late));
                        tvAbsent.setText(String.valueOf(absent));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadDepartmentCount() {
        departmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                tvDepartmentCount.setText(count + (count == 1 ? " department" : " departments"));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadShiftCount() {
        shiftsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                tvShiftCount.setText(count + (count == 1 ? " shift" : " shifts"));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadEmployeeList() {
        employeesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                employeeList.clear();
                for (DataSnapshot emp : snapshot.getChildren()) {
                    DataSnapshot info = emp.child("info");
                    if (info.exists()) {
                        EmployeeModel model = info.getValue(EmployeeModel.class);
                        if (model != null) employeeList.add(model);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
