//package com.sandhyyasofttech.attendsmart.Activities;
//
//import android.content.Intent;
//import android.net.Uri;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bumptech.glide.Glide;
//import com.google.android.material.appbar.MaterialToolbar;
//import com.google.android.material.button.MaterialButton;
//import com.google.android.material.dialog.MaterialAlertDialogBuilder;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;
//import com.sandhyyasofttech.attendsmart.Models.DocumentModel;
//import com.sandhyyasofttech.attendsmart.R;
//import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//
//public class AdminEmployeeDocumentsActivity extends AppCompatActivity {
//
//    private MaterialToolbar toolbar;
//    private RecyclerView rvDocuments;
//
//    // Employee Info Views
//    private ImageView ivEmployeeProfile;
//    private TextView tvEmployeeName;
//    private TextView tvEmployeeId;
//    private TextView tvEmployeeMobile;
//    private TextView tvDepartment;
//    private TextView tvTotalDocs;
//    private TextView tvDocumentStats;
//
//    // Action Buttons
//    private MaterialButton btnRequestDocuments, btnExport, btnSendRequest;
//
//    // Other Views
//    private View progressBar, emptyState;
//
//    private DocumentAdapter documentAdapter;
//    private List<DocumentModel> documentList = new ArrayList<>();
//
//    private DatabaseReference documentsRef;
//    private StorageReference storageRef;
//    private String companyKey, employeeMobile, employeeName, employeeId, profileImage, department;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_admin_employee_documents);
//
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
//        }
//
//        initViews();
//        getIntentData();
//        setupToolbar();
//        loadDocuments();
//    }
//
//    private void initViews() {
//        // Initialize toolbar
//        toolbar = findViewById(R.id.toolbar);
//
//        // Initialize RecyclerView
//        rvDocuments = findViewById(R.id.rvDocuments);
//
//        // Initialize Employee Info Views
//        ivEmployeeProfile = findViewById(R.id.ivEmployeeProfile);
//        tvEmployeeName = findViewById(R.id.tvEmployeeName);
//        tvEmployeeId = findViewById(R.id.tvEmployeeId);
//        tvEmployeeMobile = findViewById(R.id.tvEmployeeMobile);
//        tvDepartment = findViewById(R.id.tvDepartment);
//        tvTotalDocs = findViewById(R.id.tvTotalDocs);
//        tvDocumentStats = findViewById(R.id.tvDocumentStats);
//
//        // Initialize buttons
//        btnRequestDocuments = findViewById(R.id.btnRequestDocuments);
//        btnExport = findViewById(R.id.btnExport);
//        btnSendRequest = findViewById(R.id.btnSendRequest);
//
//        // Initialize other views
//        progressBar = findViewById(R.id.progressBar);
//        emptyState = findViewById(R.id.emptyState);
//
//        // Setup RecyclerView
//        rvDocuments.setLayoutManager(new LinearLayoutManager(this));
//        documentAdapter = new DocumentAdapter(documentList);
//        rvDocuments.setAdapter(documentAdapter);
//
//        // Set click listeners
//        if (btnRequestDocuments != null) {
//            btnRequestDocuments.setOnClickListener(v -> requestDocuments());
//        }
//
//        if (btnExport != null) {
//            btnExport.setOnClickListener(v -> exportDocuments());
//        }
//
//        if (btnSendRequest != null) {
//            btnSendRequest.setOnClickListener(v -> requestDocuments());
//        }
//    }
//
//    private void getIntentData() {
//        Intent intent = getIntent();
//        employeeMobile = intent.getStringExtra("employeeMobile");
//        employeeName = intent.getStringExtra("employeeName");
//        employeeId = intent.getStringExtra("employeeId");
//        profileImage = intent.getStringExtra("profileImage");
//        department = intent.getStringExtra("department");
//
//        if (TextUtils.isEmpty(employeeMobile)) {
//            Toast.makeText(this, "Employee data missing", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//
//        // Display employee info in separate TextViews
//        displayEmployeeInfo();
//
//        // Get company key from session
//        companyKey = new PrefManager(this).getCompanyKey();
//        if (TextUtils.isEmpty(companyKey)) {
//            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//
//        // Initialize Firebase references
//        documentsRef = FirebaseDatabase.getInstance()
//                .getReference("Companies")
//                .child(companyKey)
//                .child("employeeDocuments")
//                .child(employeeMobile);
//
//        storageRef = FirebaseStorage.getInstance().getReference()
//                .child("CompanyDocuments")
//                .child(companyKey)
//                .child(employeeMobile);
//    }
//
//    private void displayEmployeeInfo() {
//        // Set Employee Name
//        if (tvEmployeeName != null && !TextUtils.isEmpty(employeeName)) {
//            tvEmployeeName.setText(employeeName);
//        }
//
//        // Set Employee ID
//        if (tvEmployeeId != null && !TextUtils.isEmpty(employeeId)) {
//            tvEmployeeId.setText(employeeId);
//        }
//
//        // Set Employee Mobile
//        if (tvEmployeeMobile != null && !TextUtils.isEmpty(employeeMobile)) {
//            tvEmployeeMobile.setText(employeeMobile);
//        }
//
//        // Set Department
//        if (tvDepartment != null) {
//            if (!TextUtils.isEmpty(department)) {
//                tvDepartment.setText(department);
//            } else {
//                tvDepartment.setText("Not Assigned");
//            }
//        }
//
//        // Load Profile Image
//        if (ivEmployeeProfile != null) {
//            if (!TextUtils.isEmpty(profileImage)) {
//                Glide.with(this)
//                        .load(profileImage)
//                        .placeholder(R.drawable.ic_person)
//                        .error(R.drawable.ic_person)
//                        .circleCrop()
//                        .into(ivEmployeeProfile);
//            } else {
//                ivEmployeeProfile.setImageResource(R.drawable.ic_person);
//            }
//        }
//    }
//
//    private void setupToolbar() {
//        setSupportActionBar(toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setTitle("Employee Documents");
//        }
//        toolbar.setNavigationOnClickListener(v -> onBackPressed());
//    }
//
//    private void loadDocuments() {
//        showLoading(true);
//
//        documentsRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                documentList.clear();
//
//                if (snapshot.exists()) {
//                    for (DataSnapshot docSnap : snapshot.getChildren()) {
//                        DocumentModel doc = docSnap.getValue(DocumentModel.class);
//                        if (doc != null) {
//                            if (doc.getDocId() == null || doc.getDocId().isEmpty()) {
//                                doc.setDocId(docSnap.getKey());
//                            }
//                            documentList.add(doc);
//                        }
//                    }
//                }
//
//                documentAdapter.notifyDataSetChanged();
//                showLoading(false);
//                updateStats();
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                showLoading(false);
//                Toast.makeText(AdminEmployeeDocumentsActivity.this,
//                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void updateStats() {
//        if (documentList.isEmpty()) {
//            // Show empty state
//            if (emptyState != null) {
//                emptyState.setVisibility(View.VISIBLE);
//            }
//            rvDocuments.setVisibility(View.GONE);
//
//            // Update stats
//            if (tvTotalDocs != null) {
//                tvTotalDocs.setText("0");
//            }
//
//            if (tvDocumentStats != null) {
//                tvDocumentStats.setText("No documents uploaded yet");
//            }
//        } else {
//            // Hide empty state
//            if (emptyState != null) {
//                emptyState.setVisibility(View.GONE);
//            }
//            rvDocuments.setVisibility(View.VISIBLE);
//
//            // Update total count
//            if (tvTotalDocs != null) {
//                tvTotalDocs.setText(String.valueOf(documentList.size()));
//            }
//
//            // Build category breakdown
//            StringBuilder stats = new StringBuilder();
//            Map<String, Integer> categoryCount = new HashMap<>();
//
//            for (DocumentModel doc : documentList) {
//                String category = doc.getDocType();
//                if (category != null) {
//                    int count = categoryCount.getOrDefault(category, 0);
//                    categoryCount.put(category, count + 1);
//                }
//            }
//
//            // Format the stats nicely
//            int index = 0;
//            for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
//                if (index > 0) {
//                    stats.append("\n");
//                }
//                stats.append("• ").append(entry.getKey()).append(": ").append(entry.getValue());
//                index++;
//            }
//
//            if (tvDocumentStats != null) {
//                if (stats.length() > 0) {
//                    tvDocumentStats.setText(stats.toString());
//                } else {
//                    tvDocumentStats.setText("No category information");
//                }
//            }
//        }
//    }
//
//    private void showLoading(boolean show) {
//        if (progressBar != null) {
//            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
//        }
//        rvDocuments.setVisibility(show ? View.GONE : View.VISIBLE);
//    }
//
//    private void requestDocuments() {
//        new MaterialAlertDialogBuilder(this)
//                .setTitle("Request Documents")
//                .setMessage("Send document request to " + employeeName + "?")
//                .setPositiveButton("Send Request", (dialog, which) -> {
//                    sendDocumentRequest();
//                })
//                .setNegativeButton("Cancel", null)
//                .show();
//    }
//
//    private void sendDocumentRequest() {
//        DatabaseReference requestsRef = FirebaseDatabase.getInstance()
//                .getReference("Companies")
//                .child(companyKey)
//                .child("documentRequests")
//                .child(employeeMobile)
//                .push();
//
//        String requestId = requestsRef.getKey();
//        long timestamp = System.currentTimeMillis();
//
//        Map<String, Object> request = new HashMap<>();
//        request.put("requestId", requestId);
//        request.put("employeeMobile", employeeMobile);
//        request.put("employeeName", employeeName);
//        request.put("requestedBy", "Admin");
//        request.put("timestamp", timestamp);
//        request.put("status", "pending");
//        request.put("message", "Please upload your required documents");
//
//        requestsRef.setValue(request)
//                .addOnSuccessListener(unused -> {
//                    Toast.makeText(this, "✅ Request sent to employee", Toast.LENGTH_SHORT).show();
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(this, "❌ Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                });
//    }
//
//    private void exportDocuments() {
//        if (documentList.isEmpty()) {
//            Toast.makeText(this, "No documents to export", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        StringBuilder exportData = new StringBuilder();
//        exportData.append("═══════════════════════════════════\n");
//        exportData.append("    EMPLOYEE DOCUMENTS REPORT\n");
//        exportData.append("═══════════════════════════════════\n\n");
//
//        exportData.append("EMPLOYEE DETAILS:\n");
//        exportData.append("─────────────────────────────────\n");
//        exportData.append("Name: ").append(employeeName).append("\n");
//        exportData.append("ID: ").append(employeeId).append("\n");
//        exportData.append("Mobile: ").append(employeeMobile).append("\n");
//        exportData.append("Department: ").append(department != null ? department : "N/A").append("\n");
//        exportData.append("Report Date: ").append(new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date())).append("\n\n");
//
//        exportData.append("DOCUMENT SUMMARY:\n");
//        exportData.append("─────────────────────────────────\n");
//        exportData.append("Total Documents: ").append(documentList.size()).append("\n\n");
//
//        // Category breakdown
//        Map<String, Integer> categoryCount = new HashMap<>();
//        for (DocumentModel doc : documentList) {
//            String category = doc.getDocType();
//            if (category != null) {
//                int count = categoryCount.getOrDefault(category, 0);
//                categoryCount.put(category, count + 1);
//            }
//        }
//
//        exportData.append("Category Breakdown:\n");
//        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
//            exportData.append("  • ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
//        }
//
//        exportData.append("\n═══════════════════════════════════\n");
//        exportData.append("    DOCUMENT LIST\n");
//        exportData.append("═══════════════════════════════════\n\n");
//
//        int docNumber = 1;
//        for (DocumentModel doc : documentList) {
//            exportData.append(docNumber++).append(". ").append(doc.getDocName()).append("\n");
//            exportData.append("   Type: ").append(doc.getDocType()).append("\n");
//            exportData.append("   Uploaded: ").append(doc.getUploadDate()).append("\n");
//            exportData.append("   Size: ").append(doc.getFileSize()).append("\n");
//            if (doc.getDescription() != null && !doc.getDescription().isEmpty()) {
//                exportData.append("   Description: ").append(doc.getDescription()).append("\n");
//            }
//            if (doc.isVerified()) {
//                exportData.append("   ✓ Verified").append("\n");
//            }
//            exportData.append("\n");
//        }
//
//        exportData.append("═══════════════════════════════════\n");
//        exportData.append("End of Report\n");
//        exportData.append("═══════════════════════════════════\n");
//
//        Intent shareIntent = new Intent(Intent.ACTION_SEND);
//        shareIntent.setType("text/plain");
//        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Documents Report - " + employeeName);
//        shareIntent.putExtra(Intent.EXTRA_TEXT, exportData.toString());
//        startActivity(Intent.createChooser(shareIntent, "Export Documents"));
//    }
//
//    private void openDocument(DocumentModel document) {
//        try {
//            Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.setData(Uri.parse(document.getFileUrl()));
//            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//            String mimeType = getMimeType(document.getFileType());
//            if (mimeType != null) {
//                intent.setDataAndType(Uri.parse(document.getFileUrl()), mimeType);
//            }
//
//            startActivity(Intent.createChooser(intent, "Open with"));
//        } catch (Exception e) {
//            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private String getMimeType(String fileType) {
//        if (fileType == null) return null;
//
//        switch (fileType.toLowerCase()) {
//            case "pdf":
//                return "application/pdf";
//            case "jpg":
//            case "jpeg":
//                return "image/jpeg";
//            case "png":
//                return "image/png";
//            case "gif":
//                return "image/gif";
//            default:
//                return "*/*";
//        }
//    }
//
//    private void showDocumentDetails(DocumentModel document) {
//        new MaterialAlertDialogBuilder(this)
//                .setTitle(document.getDocName())
//                .setMessage(
//                        "Type: " + document.getDocType() + "\n" +
//                                "Uploaded: " + document.getUploadDate() + "\n" +
//                                "Size: " + document.getFileSize() + "\n" +
//                                "Uploaded by: " + (document.getUploadedByName() != null && !document.getUploadedByName().isEmpty() ?
//                                document.getUploadedByName() : "Employee") + "\n" +
//                                (document.getDescription() != null && !document.getDescription().isEmpty() ?
//                                        "Description: " + document.getDescription() : "")
//                )
//                .setPositiveButton("Open", (dialog, which) -> openDocument(document))
//                .setNegativeButton("Close", null)
//                .show();
//    }
//
//    private void verifyDocument(DocumentModel document) {
//        Map<String, Object> updates = new HashMap<>();
//        updates.put("verified", true);
//        updates.put("verifiedBy", "Admin");
//        updates.put("verifiedDate", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
//
//        documentsRef.child(document.getDocId()).updateChildren(updates)
//                .addOnSuccessListener(unused -> {
//                    Toast.makeText(this, "✅ Document verified successfully", Toast.LENGTH_SHORT).show();
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(this, "❌ Failed to verify: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                });
//    }
//
//    private void deleteDocument(DocumentModel document) {
//        new MaterialAlertDialogBuilder(this)
//                .setTitle("Delete Document")
//                .setMessage("Are you sure you want to delete " + document.getDocName() + "?")
//                .setPositiveButton("Delete", (dialog, which) -> {
//                    performDelete(document);
//                })
//                .setNegativeButton("Cancel", null)
//                .show();
//    }
//
//    private void performDelete(DocumentModel document) {
//        if (progressBar != null) {
//            progressBar.setVisibility(View.VISIBLE);
//        }
//
//        if (document.getFileName() != null) {
//            StorageReference fileRef = storageRef.child(document.getFileName());
//            fileRef.delete()
//                    .addOnSuccessListener(unused -> {
//                        deleteFromDatabase(document);
//                    })
//                    .addOnFailureListener(e -> {
//                        deleteFromDatabase(document);
//                    });
//        } else {
//            deleteFromDatabase(document);
//        }
//    }
//
//    private void deleteFromDatabase(DocumentModel document) {
//        documentsRef.child(document.getDocId()).removeValue()
//                .addOnSuccessListener(unused -> {
//                    if (progressBar != null) {
//                        progressBar.setVisibility(View.GONE);
//                    }
//                    Toast.makeText(this, "✅ Document deleted successfully", Toast.LENGTH_SHORT).show();
//                })
//                .addOnFailureListener(e -> {
//                    if (progressBar != null) {
//                        progressBar.setVisibility(View.GONE);
//                    }
//                    Toast.makeText(this, "❌ Failed to delete document info", Toast.LENGTH_SHORT).show();
//                });
//    }
//
//    // ==================== ADAPTER ====================
//
//    private class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {
//
//        private List<DocumentModel> documents;
//
//        public DocumentAdapter(List<DocumentModel> documents) {
//            this.documents = documents;
//        }
//
//        @NonNull
//        @Override
//        public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//            View view = LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.item_document_admin, parent, false);
//            return new DocumentViewHolder(view);
//        }
//
//        @Override
//        public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
//            DocumentModel document = documents.get(position);
//
//            holder.tvDocName.setText(document.getDocName());
//            holder.tvDocType.setText(document.getDocType());
//            holder.tvUploadDate.setText("Uploaded: " + document.getUploadDate());
//            holder.tvUploadedBy.setText("By: " + (document.getUploadedByName() != null &&
//                    !document.getUploadedByName().isEmpty() ? document.getUploadedByName() : "Employee"));
//
//            int iconRes = getIconForFileType(document.getFileType());
//            holder.ivDocIcon.setImageResource(iconRes);
//
//            if (document.getFileType() != null &&
//                    (document.getFileType().equals("jpg") || document.getFileType().equals("jpeg") ||
//                            document.getFileType().equals("png") || document.getFileType().equals("gif"))) {
//                Glide.with(AdminEmployeeDocumentsActivity.this)
//                        .load(document.getFileUrl())
//                        .placeholder(R.drawable.ic_image)
//                        .thumbnail(0.1f)
//                        .into(holder.ivDocIcon);
//            }
//
//            if (document.isVerified()) {
//                holder.ivVerified.setVisibility(View.VISIBLE);
//            } else {
//                holder.ivVerified.setVisibility(View.GONE);
//            }
//
//            holder.btnView.setOnClickListener(v -> openDocument(document));
//            holder.btnVerify.setOnClickListener(v -> verifyDocument(document));
//            holder.btnDelete.setOnClickListener(v -> deleteDocument(document));
//
//            holder.itemView.setOnClickListener(v -> showDocumentDetails(document));
//        }
//
//        private int getIconForFileType(String fileType) {
//            if (fileType == null || fileType.isEmpty()) {
//                return R.drawable.document;
//            }
//
//            switch (fileType.toLowerCase()) {
//                case "pdf":
//                    return R.drawable.ic_pdf;
//                case "jpg":
//                case "jpeg":
//                case "png":
//                case "gif":
//                case "bmp":
//                case "webp":
//                    return R.drawable.ic_image;
//                case "doc":
//                case "docx":
//                    return R.drawable.ic_word;
//                case "xls":
//                case "xlsx":
//                case "csv":
//                    return R.drawable.ic_excel;
//                case "txt":
//                case "rtf":
//                    return R.drawable.ic_text;
//                case "zip":
//                case "rar":
//                case "7z":
//                    return R.drawable.ic_zip;
//                case "ppt":
//                case "pptx":
//                    return R.drawable.ic_powerpoint;
//                default:
//                    return R.drawable.document;
//            }
//        }
//
//        @Override
//        public int getItemCount() {
//            return documents.size();
//        }
//
//        class DocumentViewHolder extends RecyclerView.ViewHolder {
//            ImageView ivDocIcon, ivVerified;
//            TextView tvDocName, tvDocType, tvUploadDate, tvUploadedBy;
//            MaterialButton btnView, btnVerify, btnDelete;
//
//            public DocumentViewHolder(@NonNull View itemView) {
//                super(itemView);
//                ivDocIcon = itemView.findViewById(R.id.ivDocIcon);
//                ivVerified = itemView.findViewById(R.id.ivVerified);
//                tvDocName = itemView.findViewById(R.id.tvDocName);
//                tvDocType = itemView.findViewById(R.id.tvDocType);
//                tvUploadDate = itemView.findViewById(R.id.tvUploadDate);
//                tvUploadedBy = itemView.findViewById(R.id.tvUploadedBy);
//                btnView = itemView.findViewById(R.id.btnView);
//                btnVerify = itemView.findViewById(R.id.btnVerify);
//                btnDelete = itemView.findViewById(R.id.btnDelete);
//            }
//        }
//    }
//}




package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sandhyyasofttech.attendsmart.Models.DocumentModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminEmployeeDocumentsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView rvDocuments;

    // Employee Info Views - Compact Layout
    private ImageView ivEmployeeProfile;
    private TextView tvEmployeeName;
    private TextView tvEmployeeId;
    private TextView tvEmployeeMobile;
    private TextView tvDepartment;
    private TextView tvTotalDocs;
    private TextView tvDocumentStats;

    // Action Buttons
    private MaterialButton btnRequestDocuments, btnExport, btnSendRequest;

    // Other Views
    private View progressBar, emptyState;

    private DocumentAdapter documentAdapter;
    private List<DocumentModel> documentList = new ArrayList<>();

    private DatabaseReference documentsRef;
    private StorageReference storageRef;
    private String companyKey, employeeMobile, employeeName, employeeId, profileImage, department;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_employee_documents);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }

        initViews();
        getIntentData();
        setupToolbar();
        loadDocuments();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvDocuments = findViewById(R.id.rvDocuments);

        // Employee Info Views
        ivEmployeeProfile = findViewById(R.id.ivEmployeeProfile);
        tvEmployeeName = findViewById(R.id.tvEmployeeName);
        tvEmployeeId = findViewById(R.id.tvEmployeeId);
        tvEmployeeMobile = findViewById(R.id.tvEmployeeMobile);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvTotalDocs = findViewById(R.id.tvTotalDocs);
        tvDocumentStats = findViewById(R.id.tvDocumentStats);

        // Buttons
        btnRequestDocuments = findViewById(R.id.btnRequestDocuments);
        btnExport = findViewById(R.id.btnExport);
        btnSendRequest = findViewById(R.id.btnSendRequest);

        // Other views
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);

        // Setup RecyclerView
        rvDocuments.setLayoutManager(new LinearLayoutManager(this));
        documentAdapter = new DocumentAdapter(documentList);
        rvDocuments.setAdapter(documentAdapter);

        // Set click listeners
        if (btnRequestDocuments != null) {
            btnRequestDocuments.setOnClickListener(v -> requestDocuments());
        }

        if (btnExport != null) {
            btnExport.setOnClickListener(v -> exportDocuments());
        }

        if (btnSendRequest != null) {
            btnSendRequest.setOnClickListener(v -> requestDocuments());
        }
    }

    private void getIntentData() {
        Intent intent = getIntent();
        employeeMobile = intent.getStringExtra("employeeMobile");
        employeeName = intent.getStringExtra("employeeName");
        employeeId = intent.getStringExtra("employeeId");
        profileImage = intent.getStringExtra("profileImage");
        department = intent.getStringExtra("department");

        if (TextUtils.isEmpty(employeeMobile)) {
            Toast.makeText(this, "Employee data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        displayEmployeeInfo();

        companyKey = new PrefManager(this).getCompanyKey();
        if (TextUtils.isEmpty(companyKey)) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        documentsRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employeeDocuments")
                .child(employeeMobile);

        storageRef = FirebaseStorage.getInstance().getReference()
                .child("CompanyDocuments")
                .child(companyKey)
                .child(employeeMobile);
    }

    private void displayEmployeeInfo() {
        // Employee Name
        if (tvEmployeeName != null && !TextUtils.isEmpty(employeeName)) {
            tvEmployeeName.setText(employeeName);
        } else if (tvEmployeeName != null) {
            tvEmployeeName.setText("Unknown");
        }

        // Employee ID
        if (tvEmployeeId != null && !TextUtils.isEmpty(employeeId)) {
            tvEmployeeId.setText(employeeId);
        } else if (tvEmployeeId != null) {
            tvEmployeeId.setText("N/A");
        }

        // Mobile Number
        if (tvEmployeeMobile != null && !TextUtils.isEmpty(employeeMobile)) {
            tvEmployeeMobile.setText(employeeMobile);
        } else if (tvEmployeeMobile != null) {
            tvEmployeeMobile.setText("No mobile");
        }

        // Department
        if (tvDepartment != null) {
            if (!TextUtils.isEmpty(department)) {
                tvDepartment.setText(department);
            } else {
                tvDepartment.setText("Unassigned");
            }
        }

        // Profile Image
        if (ivEmployeeProfile != null) {
            if (!TextUtils.isEmpty(profileImage)) {
                Glide.with(this)
                        .load(profileImage)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivEmployeeProfile);
            } else {
                ivEmployeeProfile.setImageResource(R.drawable.ic_person);
            }
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Employee Documents");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadDocuments() {
        showLoading(true);

        documentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                documentList.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot docSnap : snapshot.getChildren()) {
                        DocumentModel doc = docSnap.getValue(DocumentModel.class);
                        if (doc != null) {
                            if (doc.getDocId() == null || doc.getDocId().isEmpty()) {
                                doc.setDocId(docSnap.getKey());
                            }
                            documentList.add(doc);
                        }
                    }
                }

                documentAdapter.notifyDataSetChanged();
                showLoading(false);
                updateStats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(AdminEmployeeDocumentsActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStats() {
        if (documentList.isEmpty()) {
            if (emptyState != null) {
                emptyState.setVisibility(View.VISIBLE);
            }
            rvDocuments.setVisibility(View.GONE);

            if (tvTotalDocs != null) {
                tvTotalDocs.setText("0");
            }

            if (tvDocumentStats != null) {
                tvDocumentStats.setText("No documents uploaded");
            }
        } else {
            if (emptyState != null) {
                emptyState.setVisibility(View.GONE);
            }
            rvDocuments.setVisibility(View.VISIBLE);

            // Update total count
            if (tvTotalDocs != null) {
                tvTotalDocs.setText(String.valueOf(documentList.size()));
            }

            // Build compact category breakdown with bullet points
            StringBuilder stats = new StringBuilder();
            Map<String, Integer> categoryCount = new HashMap<>();

            for (DocumentModel doc : documentList) {
                String category = doc.getDocType();
                if (category != null) {
                    int count = categoryCount.getOrDefault(category, 0);
                    categoryCount.put(category, count + 1);
                }
            }

            // Format as compact horizontal list
            int index = 0;
            for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
                if (index > 0) {
                    stats.append(" • ");
                }
                stats.append(entry.getKey()).append(": ").append(entry.getValue());
                index++;
            }

            if (tvDocumentStats != null) {
                if (stats.length() > 0) {
                    tvDocumentStats.setText(stats.toString());
                } else {
                    tvDocumentStats.setText("Categories not available");
                }
            }
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        rvDocuments.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void requestDocuments() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Request Documents")
                .setMessage("Send document request to " + employeeName + "?")
                .setPositiveButton("Send", (dialog, which) -> sendDocumentRequest())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendDocumentRequest() {
        DatabaseReference requestsRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("documentRequests")
                .child(employeeMobile)
                .push();

        String requestId = requestsRef.getKey();
        long timestamp = System.currentTimeMillis();

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("employeeMobile", employeeMobile);
        request.put("employeeName", employeeName);
        request.put("requestedBy", "Admin");
        request.put("timestamp", timestamp);
        request.put("status", "pending");
        request.put("message", "Please upload your required documents");

        requestsRef.setValue(request)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "✅ Request sent", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void exportDocuments() {
        if (documentList.isEmpty()) {
            Toast.makeText(this, "No documents to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder exportData = new StringBuilder();
        exportData.append("══════════════════════════════\n");
        exportData.append("   EMPLOYEE DOCUMENTS REPORT\n");
        exportData.append("══════════════════════════════\n\n");

        exportData.append("EMPLOYEE DETAILS:\n");
        exportData.append("Name: ").append(employeeName).append("\n");
        exportData.append("ID: ").append(employeeId).append("\n");
        exportData.append("Mobile: ").append(employeeMobile).append("\n");
        exportData.append("Department: ").append(department != null ? department : "N/A").append("\n");
        exportData.append("Report Date: ").append(
                new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date())
        ).append("\n\n");

        exportData.append("SUMMARY:\n");
        exportData.append("Total Documents: ").append(documentList.size()).append("\n\n");

        // Category breakdown
        Map<String, Integer> categoryCount = new HashMap<>();
        for (DocumentModel doc : documentList) {
            String category = doc.getDocType();
            if (category != null) {
                categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
            }
        }

        exportData.append("Categories:\n");
        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            exportData.append("  • ").append(entry.getKey()).append(": ")
                    .append(entry.getValue()).append("\n");
        }

        exportData.append("\n══════════════════════════════\n");
        exportData.append("   DOCUMENT LIST\n");
        exportData.append("══════════════════════════════\n\n");

        int docNumber = 1;
        for (DocumentModel doc : documentList) {
            exportData.append(docNumber++).append(". ").append(doc.getDocName()).append("\n");
            exportData.append("   Type: ").append(doc.getDocType()).append("\n");
            exportData.append("   Uploaded: ").append(doc.getUploadDate()).append("\n");
            exportData.append("   Size: ").append(doc.getFileSize()).append("\n");
            if (doc.getDescription() != null && !doc.getDescription().isEmpty()) {
                exportData.append("   Note: ").append(doc.getDescription()).append("\n");
            }
            if (doc.isVerified()) {
                exportData.append("   ✓ Verified\n");
            }
            exportData.append("\n");
        }

        exportData.append("══════════════════════════════\n");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Documents Report - " + employeeName);
        shareIntent.putExtra(Intent.EXTRA_TEXT, exportData.toString());
        startActivity(Intent.createChooser(shareIntent, "Export Documents"));
    }

    private void openDocument(DocumentModel document) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(document.getFileUrl()));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String mimeType = getMimeType(document.getFileType());
            if (mimeType != null) {
                intent.setDataAndType(Uri.parse(document.getFileUrl()), mimeType);
            }

            startActivity(Intent.createChooser(intent, "Open with"));
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(String fileType) {
        if (fileType == null) return null;

        switch (fileType.toLowerCase()) {
            case "pdf": return "application/pdf";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            default: return "*/*";
        }
    }

    private void showDocumentDetails(DocumentModel document) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(document.getDocName())
                .setMessage(
                        "Type: " + document.getDocType() + "\n" +
                                "Uploaded: " + document.getUploadDate() + "\n" +
                                "Size: " + document.getFileSize() + "\n" +
                                "By: " + (document.getUploadedByName() != null && !document.getUploadedByName().isEmpty() ?
                                document.getUploadedByName() : "Employee") + "\n" +
                                (document.getDescription() != null && !document.getDescription().isEmpty() ?
                                        "Note: " + document.getDescription() : "")
                )
                .setPositiveButton("Open", (dialog, which) -> openDocument(document))
                .setNegativeButton("Close", null)
                .show();
    }

    private void verifyDocument(DocumentModel document) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("verified", true);
        updates.put("verifiedBy", "Admin");
        updates.put("verifiedDate", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));

        documentsRef.child(document.getDocId()).updateChildren(updates)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "✅ Verified", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteDocument(DocumentModel document) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Document")
                .setMessage("Delete " + document.getDocName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> performDelete(document))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete(DocumentModel document) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        if (document.getFileName() != null) {
            StorageReference fileRef = storageRef.child(document.getFileName());
            fileRef.delete()
                    .addOnSuccessListener(unused -> deleteFromDatabase(document))
                    .addOnFailureListener(e -> deleteFromDatabase(document));
        } else {
            deleteFromDatabase(document);
        }
    }

    private void deleteFromDatabase(DocumentModel document) {
        documentsRef.child(document.getDocId()).removeValue()
                .addOnSuccessListener(unused -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "✅ Deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "❌ Failed", Toast.LENGTH_SHORT).show();
                });
    }

    // ==================== ADAPTER ====================

    private class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

        private List<DocumentModel> documents;

        public DocumentAdapter(List<DocumentModel> documents) {
            this.documents = documents;
        }

        @NonNull
        @Override
        public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_document_admin, parent, false);
            return new DocumentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
            DocumentModel document = documents.get(position);

            holder.tvDocName.setText(document.getDocName());
            holder.tvDocType.setText(document.getDocType());
            holder.tvUploadDate.setText("Uploaded: " + document.getUploadDate());
            holder.tvUploadedBy.setText("By: " + (document.getUploadedByName() != null &&
                    !document.getUploadedByName().isEmpty() ? document.getUploadedByName() : "Employee"));

            int iconRes = getIconForFileType(document.getFileType());
            holder.ivDocIcon.setImageResource(iconRes);

            if (document.getFileType() != null &&
                    (document.getFileType().equals("jpg") || document.getFileType().equals("jpeg") ||
                            document.getFileType().equals("png") || document.getFileType().equals("gif"))) {
                Glide.with(AdminEmployeeDocumentsActivity.this)
                        .load(document.getFileUrl())
                        .placeholder(R.drawable.ic_image)
                        .thumbnail(0.1f)
                        .into(holder.ivDocIcon);
            }

            holder.ivVerified.setVisibility(document.isVerified() ? View.VISIBLE : View.GONE);

            holder.btnView.setOnClickListener(v -> openDocument(document));
            holder.btnVerify.setOnClickListener(v -> verifyDocument(document));
            holder.btnDelete.setOnClickListener(v -> deleteDocument(document));
            holder.itemView.setOnClickListener(v -> showDocumentDetails(document));
        }

        private int getIconForFileType(String fileType) {
            if (fileType == null || fileType.isEmpty()) return R.drawable.document;

            switch (fileType.toLowerCase()) {
                case "pdf": return R.drawable.ic_pdf;
                case "jpg":
                case "jpeg":
                case "png":
                case "gif":
                case "bmp":
                case "webp": return R.drawable.ic_image;
                case "doc":
                case "docx": return R.drawable.ic_word;
                case "xls":
                case "xlsx":
                case "csv": return R.drawable.ic_excel;
                case "txt":
                case "rtf": return R.drawable.ic_text;
                case "zip":
                case "rar":
                case "7z": return R.drawable.ic_zip;
                case "ppt":
                case "pptx": return R.drawable.ic_powerpoint;
                default: return R.drawable.document;
            }
        }

        @Override
        public int getItemCount() {
            return documents.size();
        }

        class DocumentViewHolder extends RecyclerView.ViewHolder {
            ImageView ivDocIcon, ivVerified;
            TextView tvDocName, tvDocType, tvUploadDate, tvUploadedBy;
            MaterialButton btnView, btnVerify, btnDelete;

            public DocumentViewHolder(@NonNull View itemView) {
                super(itemView);
                ivDocIcon = itemView.findViewById(R.id.ivDocIcon);
                ivVerified = itemView.findViewById(R.id.ivVerified);
                tvDocName = itemView.findViewById(R.id.tvDocName);
                tvDocType = itemView.findViewById(R.id.tvDocType);
                tvUploadDate = itemView.findViewById(R.id.tvUploadDate);
                tvUploadedBy = itemView.findViewById(R.id.tvUploadedBy);
                btnView = itemView.findViewById(R.id.btnView);
                btnVerify = itemView.findViewById(R.id.btnVerify);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}