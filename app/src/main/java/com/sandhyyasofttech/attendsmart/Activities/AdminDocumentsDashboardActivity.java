package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Models.DocumentModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminDocumentsDashboardActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private RecyclerView rvEmployees;
    private SearchView searchView;
    private ChipGroup chipGroupFilter;
    private TextView tvTotalEmployees, tvTotalDocuments, tvStatsInfo;
    private View emptyState, progressBar;

    // Adapter
    private EmployeeDocumentsAdapter adapter;
    private List<EmployeeDocumentSummary> employeeList = new ArrayList<>();
    private List<EmployeeDocumentSummary> filteredList = new ArrayList<>();

    // Firebase
    private DatabaseReference companyRef;
    private String companyKey;

    // Counters
    private Map<String, Integer> categoryCountMap = new HashMap<>();
    private int totalDocumentsCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_documents_dashboard);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }

        initViews();
        loadSession();
        setupToolbar();
        setupSearch();
        setupFilterChips();
        loadEmployeesWithDocuments();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvEmployees = findViewById(R.id.rvEmployees);
        searchView = findViewById(R.id.searchView);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
        tvTotalDocuments = findViewById(R.id.tvTotalDocuments);
        tvStatsInfo = findViewById(R.id.tvStatsInfo);
        emptyState = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.progressBar);

        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmployeeDocumentsAdapter();
        rvEmployees.setAdapter(adapter);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Documents Dashboard");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadSession() {
        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();

        if (TextUtils.isEmpty(companyKey)) {
            Toast.makeText(this, "Company session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        companyRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey);
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterEmployees(newText);
                return true;
            }
        });
    }

    private void setupFilterChips() {
        // Clear existing chips
        chipGroupFilter.removeAllViews();

        // Create filter chips for document categories
        String[] categories = {"All", "Aadhaar Card", "PAN Card", "Passport", "Driving License",
                "Education Certificate", "Experience Letter", "Pending", "Completed"};

        for (String category : categories) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.red);
            chip.setTextColor(getResources().getColor(R.color.white));

            if (category.equals("All")) {
                chip.setChecked(true);
            }

            chip.setOnClickListener(v -> {
                // Uncheck all other chips
                for (int i = 0; i < chipGroupFilter.getChildCount(); i++) {
                    Chip otherChip = (Chip) chipGroupFilter.getChildAt(i);
                    if (otherChip != chip) {
                        otherChip.setChecked(false);
                    }
                }
                chip.setChecked(true);
                filterByCategory(category);
            });

            chipGroupFilter.addView(chip);
        }
    }

    private void loadEmployeesWithDocuments() {
        showLoading(true);

        // Clear previous data
        employeeList.clear();
        filteredList.clear();
        categoryCountMap.clear();
        totalDocumentsCount = 0;

        // Load employees
        companyRef.child("employees").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot employeesSnapshot) {
                if (!employeesSnapshot.exists()) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        emptyState.setVisibility(View.VISIBLE);
                        tvStatsInfo.setText("No employees found");
                    });
                    return;
                }

                final AtomicInteger pendingEmployees = new AtomicInteger(0);

                // Count only mobile number nodes (skip "info" if it exists)
                for (DataSnapshot empSnap : employeesSnapshot.getChildren()) {
                    String key = empSnap.getKey();
                    // Count only if it looks like a mobile number (all digits) or has info child
                    if (key.matches("\\d+") || empSnap.child("info").exists()) {
                        pendingEmployees.incrementAndGet();
                    }
                }

                if (pendingEmployees.get() == 0) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        emptyState.setVisibility(View.VISIBLE);
                        tvStatsInfo.setText("No employees found");
                    });
                    return;
                }

                for (DataSnapshot empSnap : employeesSnapshot.getChildren()) {
                    String empKey = empSnap.getKey();

                    // Skip if not a mobile number and doesn't have info
                    if (!empKey.matches("\\d+") && !empSnap.child("info").exists()) {
                        continue;
                    }

                    DataSnapshot infoNode = empSnap.child("info");
                    String employeeName, employeeId, employeeDepartment, profileImage;

                    if (infoNode.exists()) {
                        // Get from info node
                        employeeName = infoNode.child("employeeName").getValue(String.class);
                        employeeId = infoNode.child("employeeMobile").getValue(String.class);
                        employeeDepartment = infoNode.child("employeeDepartment").getValue(String.class);
                        profileImage = infoNode.child("profileImage").getValue(String.class);
                    } else {
                        // Get directly from employee node
                        employeeName = empSnap.child("employeeName").getValue(String.class);
                        employeeId = empSnap.child("employeeMobile").getValue(String.class);
                        employeeDepartment = empSnap.child("employeeDepartment").getValue(String.class);
                        profileImage = empSnap.child("profileImage").getValue(String.class);
                    }

                    loadEmployeeDocuments(empKey, employeeName, employeeId,
                            employeeDepartment, profileImage, pendingEmployees);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(AdminDocumentsDashboardActivity.this,
                            "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    private void loadEmployeeDocuments(String employeeMobile, String employeeName,
                                       String employeeId, String employeeDepartment,
                                       String profileImage, AtomicInteger pendingEmployees) {

        companyRef.child("employeeDocuments").child(employeeMobile)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot documentsSnapshot) {
                        List<DocumentModel> empDocuments = new ArrayList<>();
                        Map<String, Integer> docCategoryCount = new HashMap<>();

                        if (documentsSnapshot.exists()) {
                            Log.d("DocLoad", "Found " + documentsSnapshot.getChildrenCount() +
                                    " documents for " + employeeMobile);

                            for (DataSnapshot docSnap : documentsSnapshot.getChildren()) {
                                DocumentModel doc = docSnap.getValue(DocumentModel.class);
                                if (doc != null) {
                                    empDocuments.add(doc);

                                    // Count by category
                                    String category = doc.getDocType();
                                    if (category != null && !category.isEmpty()) {
                                        int count = docCategoryCount.getOrDefault(category, 0);
                                        docCategoryCount.put(category, count + 1);

                                        // Update global category counts
                                        synchronized (categoryCountMap) {
                                            int globalCount = categoryCountMap.getOrDefault(category, 0);
                                            categoryCountMap.put(category, globalCount + 1);
                                        }
                                    }
                                    totalDocumentsCount++;
                                }
                            }
                        } else {
                            Log.d("DocLoad", "No documents found for " + employeeMobile);
                        }

                        // Create employee summary
                        EmployeeDocumentSummary summary = new EmployeeDocumentSummary();
                        summary.setEmployeeId(employeeId != null ? employeeId : employeeMobile);
                        summary.setEmployeeName(employeeName != null ? employeeName : "Unknown Employee");
                        summary.setEmployeeMobile(employeeMobile);
                        summary.setDepartment(employeeDepartment != null ? employeeDepartment : "Not Assigned");
                        summary.setProfilePic(profileImage != null ? profileImage : "");
                        summary.setTotalDocuments(empDocuments.size());
                        summary.setDocumentCategories(docCategoryCount);
                        summary.setDocuments(empDocuments);

                        // Add to lists
                        synchronized (employeeList) {
                            employeeList.add(summary);
                        }

                        // Check if all employees processed
                        if (pendingEmployees.decrementAndGet() == 0) {
                            runOnUiThread(() -> {
                                filteredList.addAll(employeeList);
                                updateUI();
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("AdminDocs", "Error loading docs for " + employeeMobile + ": " + error.getMessage());

                        // Create empty summary for this employee
                        EmployeeDocumentSummary summary = new EmployeeDocumentSummary();
                        summary.setEmployeeId(employeeId != null ? employeeId : employeeMobile);
                        summary.setEmployeeName(employeeName != null ? employeeName : "Unknown Employee");
                        summary.setEmployeeMobile(employeeMobile);
                        summary.setDepartment(employeeDepartment != null ? employeeDepartment : "Not Assigned");
                        summary.setProfilePic(profileImage != null ? profileImage : "");
                        summary.setTotalDocuments(0);
                        summary.setDocumentCategories(new HashMap<>());
                        summary.setDocuments(new ArrayList<>());

                        synchronized (employeeList) {
                            employeeList.add(summary);
                        }

                        if (pendingEmployees.decrementAndGet() == 0) {
                            runOnUiThread(() -> {
                                filteredList.addAll(employeeList);
                                updateUI();
                            });
                        }
                    }
                });
    }

    private void updateUI() {
        tvTotalEmployees.setText(String.valueOf(employeeList.size()));
        tvTotalDocuments.setText(String.valueOf(totalDocumentsCount));

        // Update stats info
        StringBuilder stats = new StringBuilder("Document Summary:\n");
        if (categoryCountMap.isEmpty()) {
            stats.append("No documents uploaded yet");
        } else {
            for (Map.Entry<String, Integer> entry : categoryCountMap.entrySet()) {
                stats.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        tvStatsInfo.setText(stats.toString());

        adapter.updateList(new ArrayList<>(filteredList));
        showLoading(false);

        if (employeeList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvEmployees.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvEmployees.setVisibility(View.VISIBLE);
        }
    }

    private void filterEmployees(String query) {
        if (TextUtils.isEmpty(query)) {
            filteredList.clear();
            filteredList.addAll(employeeList);
        } else {
            filteredList.clear();
            String lowerQuery = query.toLowerCase();
            for (EmployeeDocumentSummary employee : employeeList) {
                String empName = employee.getEmployeeName() != null ? employee.getEmployeeName().toLowerCase() : "";
                String empId = employee.getEmployeeId() != null ? employee.getEmployeeId().toLowerCase() : "";
                String dept = employee.getDepartment() != null ? employee.getDepartment().toLowerCase() : "";
                String mobile = employee.getEmployeeMobile() != null ? employee.getEmployeeMobile() : "";

                if (empName.contains(lowerQuery) ||
                        empId.contains(lowerQuery) ||
                        dept.contains(lowerQuery) ||
                        mobile.contains(query)) {
                    filteredList.add(employee);
                }
            }
        }

        adapter.updateList(new ArrayList<>(filteredList));
    }

    private void filterByCategory(String category) {
        if (category.equals("All")) {
            filteredList.clear();
            filteredList.addAll(employeeList);
        } else if (category.equals("Pending")) {
            filteredList.clear();
            for (EmployeeDocumentSummary employee : employeeList) {
                if (employee.getTotalDocuments() == 0) {
                    filteredList.add(employee);
                }
            }
        } else if (category.equals("Completed")) {
            filteredList.clear();
            for (EmployeeDocumentSummary employee : employeeList) {
                if (employee.getTotalDocuments() > 0) {
                    filteredList.add(employee);
                }
            }
        } else {
            filteredList.clear();
            for (EmployeeDocumentSummary employee : employeeList) {
                if (employee.getDocumentCategories().containsKey(category)) {
                    filteredList.add(employee);
                }
            }
        }

        adapter.updateList(new ArrayList<>(filteredList));
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvEmployees.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void openEmployeeDocuments(EmployeeDocumentSummary employee) {
        Intent intent = new Intent(this, AdminEmployeeDocumentsActivity.class);
        intent.putExtra("employeeMobile", employee.getEmployeeMobile());
        intent.putExtra("employeeName", employee.getEmployeeName());
        intent.putExtra("employeeId", employee.getEmployeeId());
        intent.putExtra("profileImage", employee.getProfilePic());
        intent.putExtra("department", employee.getDepartment());
        startActivity(intent);
    }

    // ==================== MODEL CLASSES ====================

    public static class EmployeeDocumentSummary {
        private String employeeId;
        private String employeeName;
        private String employeeMobile;
        private String department;
        private String profilePic;
        private int totalDocuments;
        private Map<String, Integer> documentCategories = new HashMap<>();
        private List<DocumentModel> documents = new ArrayList<>();

        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

        public String getEmployeeMobile() { return employeeMobile; }
        public void setEmployeeMobile(String employeeMobile) { this.employeeMobile = employeeMobile; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getProfilePic() { return profilePic; }
        public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

        public int getTotalDocuments() { return totalDocuments; }
        public void setTotalDocuments(int totalDocuments) { this.totalDocuments = totalDocuments; }

        public Map<String, Integer> getDocumentCategories() { return documentCategories; }
        public void setDocumentCategories(Map<String, Integer> documentCategories) {
            this.documentCategories = documentCategories;
        }

        public List<DocumentModel> getDocuments() { return documents; }
        public void setDocuments(List<DocumentModel> documents) {
            this.documents = documents;
        }
    }

    // ==================== ADAPTER ====================

    private class EmployeeDocumentsAdapter extends RecyclerView.Adapter<EmployeeDocumentsAdapter.EmployeeViewHolder> {

        private List<EmployeeDocumentSummary> employeeList;

        public EmployeeDocumentsAdapter() {
            this.employeeList = new ArrayList<>();
        }

        public void updateList(List<EmployeeDocumentSummary> newList) {
            this.employeeList = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_employee_documents, parent, false);
            return new EmployeeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
            EmployeeDocumentSummary employee = employeeList.get(position);

            holder.tvEmployeeName.setText(employee.getEmployeeName() != null ?
                    employee.getEmployeeName() : "Unknown");
            holder.tvEmployeeId.setText("ID: " + (employee.getEmployeeId() != null ?
                    employee.getEmployeeId() : employee.getEmployeeMobile()));
            holder.tvDepartment.setText(employee.getDepartment() != null ?
                    employee.getDepartment() : "Not Assigned");
            holder.tvDocumentCount.setText(String.valueOf(employee.getTotalDocuments()));

            // Load profile picture
            if (employee.getProfilePic() != null && !employee.getProfilePic().isEmpty()) {
                Glide.with(AdminDocumentsDashboardActivity.this)
                        .load(employee.getProfilePic())
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(holder.ivProfile);
            } else {
                holder.ivProfile.setImageResource(R.drawable.ic_person);
            }

            // Show document categories
            holder.tvDocCategories.setText(getCategorySummary(employee.getDocumentCategories()));

            // Set click listener
            holder.itemView.setOnClickListener(v -> {
                openEmployeeDocuments(employee);
            });

            // Set document status
            if (employee.getTotalDocuments() == 0) {
                holder.tvStatus.setText("Pending");
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            } else if (employee.getTotalDocuments() >= 3) {
                holder.tvStatus.setText("Complete");
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            } else {
                holder.tvStatus.setText("Partial");
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            }
        }

        private String getCategorySummary(Map<String, Integer> categories) {
            if (categories == null || categories.isEmpty()) return "No documents";

            StringBuilder summary = new StringBuilder();
            int count = 0;
            for (Map.Entry<String, Integer> entry : categories.entrySet()) {
                if (count < 3) {
                    summary.append(entry.getKey()).append(" (").append(entry.getValue()).append("), ");
                    count++;
                }
            }

            if (summary.length() > 0) {
                summary.setLength(summary.length() - 2);
                if (categories.size() > 3) {
                    summary.append(" +").append(categories.size() - 3).append(" more");
                }
            }

            return summary.toString();
        }

        @Override
        public int getItemCount() {
            return employeeList.size();
        }

        class EmployeeViewHolder extends RecyclerView.ViewHolder {
            ImageView ivProfile;
            TextView tvEmployeeName, tvEmployeeId, tvDepartment;
            TextView tvDocumentCount, tvDocCategories, tvStatus;

            public EmployeeViewHolder(@NonNull View itemView) {
                super(itemView);
                ivProfile = itemView.findViewById(R.id.ivProfile);
                tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
                tvEmployeeId = itemView.findViewById(R.id.tvEmployeeId);
                tvDepartment = itemView.findViewById(R.id.tvDepartment);
                tvDocumentCount = itemView.findViewById(R.id.tvDocumentCount);
                tvDocCategories = itemView.findViewById(R.id.tvDocCategories);
                tvStatus = itemView.findViewById(R.id.tvStatus);
            }
        }
    }
}