package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.sandhyyasofttech.attendsmart.Models.DocumentRequestModel;
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
    private ImageView ivEmployeeProfile;
    private TextView tvEmployeeName, tvEmployeeId, tvEmployeeMobile, tvDepartment, tvTotalDocs, tvDocumentStats;
    private MaterialButton btnRequestDocuments, btnExport, btnSendRequest;
    private View progressBar, emptyState;

    private DocumentAdapter documentAdapter;
    private List<DocumentModel> documentList = new ArrayList<>();
    private List<String> documentCategories = new ArrayList<>();

    private DatabaseReference documentsRef, requestsRef;
    private StorageReference storageRef;
    private String companyKey, employeeMobile, employeeName, employeeId, profileImage, department;
    private String adminName;

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
        loadCategories();
        loadDocuments();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvDocuments = findViewById(R.id.rvDocuments);
        ivEmployeeProfile = findViewById(R.id.ivEmployeeProfile);
        tvEmployeeName = findViewById(R.id.tvEmployeeName);
        tvEmployeeId = findViewById(R.id.tvEmployeeId);
        tvEmployeeMobile = findViewById(R.id.tvEmployeeMobile);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvTotalDocs = findViewById(R.id.tvTotalDocs);
        tvDocumentStats = findViewById(R.id.tvDocumentStats);
        btnRequestDocuments = findViewById(R.id.btnRequestDocuments);
        btnExport = findViewById(R.id.btnExport);
        btnSendRequest = findViewById(R.id.btnSendRequest);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);

        rvDocuments.setLayoutManager(new LinearLayoutManager(this));
        documentAdapter = new DocumentAdapter(documentList);
        rvDocuments.setAdapter(documentAdapter);

        if (btnRequestDocuments != null) {
            btnRequestDocuments.setOnClickListener(v -> showRequestDocumentDialog());
        }
        if (btnExport != null) {
            btnExport.setOnClickListener(v -> exportDocuments());
        }
        if (btnSendRequest != null) {
            btnSendRequest.setOnClickListener(v -> showRequestDocumentDialog());
        }

        // Initialize categories
        documentCategories.add("Select Document Type");
        documentCategories.add("Aadhaar Card");
        documentCategories.add("PAN Card");
        documentCategories.add("Passport");
        documentCategories.add("Driving License");
        documentCategories.add("Voter ID");
        documentCategories.add("Bank Passbook");
        documentCategories.add("Salary Slip");
        documentCategories.add("Offer Letter");
        documentCategories.add("Experience Letter");
        documentCategories.add("Education Certificate");
        documentCategories.add("Resume");
        documentCategories.add("Photo");
        documentCategories.add("Other Document");
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

        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();
        adminName = pref.getEmployeeName(); // Get admin name from preferences

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

        requestsRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("documentRequests")
                .child(employeeMobile);

        storageRef = FirebaseStorage.getInstance().getReference()
                .child("CompanyDocuments")
                .child(companyKey)
                .child(employeeMobile);
    }

    private void displayEmployeeInfo() {
        if (tvEmployeeName != null && !TextUtils.isEmpty(employeeName)) {
            tvEmployeeName.setText(employeeName);
        } else if (tvEmployeeName != null) {
            tvEmployeeName.setText("Unknown");
        }

        if (tvEmployeeId != null && !TextUtils.isEmpty(employeeId)) {
            tvEmployeeId.setText(employeeId);
        } else if (tvEmployeeId != null) {
            tvEmployeeId.setText("N/A");
        }

        if (tvEmployeeMobile != null && !TextUtils.isEmpty(employeeMobile)) {
            tvEmployeeMobile.setText(employeeMobile);
        } else if (tvEmployeeMobile != null) {
            tvEmployeeMobile.setText("No mobile");
        }

        if (tvDepartment != null) {
            if (!TextUtils.isEmpty(department)) {
                tvDepartment.setText(department);
            } else {
                tvDepartment.setText("Unassigned");
            }
        }

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

    private void loadCategories() {
        DatabaseReference categoriesRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("documentCategories");

        categoriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    documentCategories.clear();
                    documentCategories.add("Select Document Type");
                    for (DataSnapshot catSnap : snapshot.getChildren()) {
                        String category = catSnap.getValue(String.class);
                        if (category != null) {
                            documentCategories.add(category);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Use default categories
            }
        });
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
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            rvDocuments.setVisibility(View.GONE);
            if (tvTotalDocs != null) tvTotalDocs.setText("0");
            if (tvDocumentStats != null) tvDocumentStats.setText("No documents uploaded");
        } else {
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            rvDocuments.setVisibility(View.VISIBLE);
            if (tvTotalDocs != null) tvTotalDocs.setText(String.valueOf(documentList.size()));

            StringBuilder stats = new StringBuilder();
            Map<String, Integer> categoryCount = new HashMap<>();

            for (DocumentModel doc : documentList) {
                String category = doc.getDocType();
                if (category != null) {
                    categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
                }
            }

            int index = 0;
            for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
                if (index > 0) stats.append(" • ");
                stats.append(entry.getKey()).append(": ").append(entry.getValue());
                index++;
            }

            if (tvDocumentStats != null) {
                tvDocumentStats.setText(stats.length() > 0 ? stats.toString() : "Categories not available");
            }
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvDocuments.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showRequestDocumentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_request_document, null);
        builder.setView(dialogView);

        AutoCompleteTextView actvDocumentType = dialogView.findViewById(R.id.actvDocumentType);
        EditText etRequestMessage = dialogView.findViewById(R.id.etRequestMessage);
        MaterialButton btnSend = dialogView.findViewById(R.id.btnSendRequest);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, documentCategories);
        actvDocumentType.setAdapter(adapter);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnSend.setOnClickListener(v -> {
            String documentType = actvDocumentType.getText().toString().trim();
            String message = etRequestMessage.getText().toString().trim();

            if (TextUtils.isEmpty(documentType) || documentType.equals("Select Document Type")) {
                Toast.makeText(this, "Please select document type", Toast.LENGTH_SHORT).show();
                return;
            }

            sendDocumentRequest(documentType, message);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void sendDocumentRequest(String documentType, String message) {
        String requestId = requestsRef.push().getKey();
        if (requestId == null) return;

        long timestamp = System.currentTimeMillis();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DocumentRequestModel request = new DocumentRequestModel();
        request.setRequestId(requestId);
        request.setEmployeeMobile(employeeMobile);
        request.setEmployeeName(employeeName);
        request.setRequestedBy("Admin");
        request.setRequestedByName(adminName != null ? adminName : "Admin");
        request.setTimestamp(timestamp);
        request.setStatus("pending");
        request.setDocumentType(documentType);
        request.setDocumentName(documentType);
        request.setMessage(TextUtils.isEmpty(message) ? "Please upload " + documentType : message);

        requestsRef.child(requestId).setValue(request)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "✅ Request sent to " + employeeName, Toast.LENGTH_SHORT).show())
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
            if (doc.isVerified()) {
                exportData.append("   ✓ Verified\n");
            }
            if (doc.isRejected()) {
                exportData.append("   ✗ Rejected: ").append(doc.getRejectionReason()).append("\n");
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
        StringBuilder details = new StringBuilder();
        details.append("Type: ").append(document.getDocType()).append("\n");
        details.append("Uploaded: ").append(document.getUploadDate()).append("\n");
        details.append("Size: ").append(document.getFileSize()).append("\n");
        details.append("By: ").append(document.getUploadedByName() != null && !document.getUploadedByName().isEmpty() ?
                document.getUploadedByName() : "Employee").append("\n");

        if (document.isVerified()) {
            details.append("\n✓ Verified");
            if (document.getVerifiedBy() != null) {
                details.append(" by ").append(document.getVerifiedBy());
            }
        }

        if (document.isRejected()) {
            details.append("\n✗ Rejected");
            if (document.getRejectionReason() != null) {
                details.append("\nReason: ").append(document.getRejectionReason());
            }
        }

        if (document.getDescription() != null && !document.getDescription().isEmpty()) {
            details.append("\n\nNote: ").append(document.getDescription());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(document.getDocName())
                .setMessage(details.toString())
                .setPositiveButton("Open", (dialog, which) -> openDocument(document))
                .setNegativeButton("Close", null)
                .show();
    }

    private void verifyDocument(DocumentModel document) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Map<String, Object> updates = new HashMap<>();
        updates.put("verified", true);
        updates.put("verifiedBy", adminName != null ? adminName : "Admin");
        updates.put("verifiedDate", currentDate);
        updates.put("rejected", false);
        updates.put("rejectionReason", null);

        documentsRef.child(document.getDocId()).updateChildren(updates)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "✅ Document verified", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showRejectDialog(DocumentModel document) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reject_document, null);
        builder.setView(dialogView);

        TextView tvDocName = dialogView.findViewById(R.id.tvDocumentName);
        EditText etRejectionReason = dialogView.findViewById(R.id.etRejectionReason);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirmReject);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelReject);

        tvDocName.setText(document.getDocName());

        AlertDialog dialog = builder.create();
        dialog.show();

        btnConfirm.setOnClickListener(v -> {
            String reason = etRejectionReason.getText().toString().trim();
            if (TextUtils.isEmpty(reason)) {
                Toast.makeText(this, "Please provide rejection reason", Toast.LENGTH_SHORT).show();
                return;
            }

            rejectDocument(document, reason);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void rejectDocument(DocumentModel document, String reason) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        long timestamp = System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("rejected", true);
        updates.put("rejectedBy", adminName != null ? adminName : "Admin");
        updates.put("rejectedDate", currentDate);
        updates.put("rejectedTimestamp", timestamp);
        updates.put("rejectionReason", reason);
        updates.put("verified", false);

        documentsRef.child(document.getDocId()).updateChildren(updates)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "✅ Document rejected", Toast.LENGTH_SHORT).show())
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
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

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
            holder.ivRejected.setVisibility(document.isRejected() ? View.VISIBLE : View.GONE);

            if (document.isRejected() && document.getRejectionReason() != null) {
                holder.rejectionSection.setVisibility(View.VISIBLE);
                holder.tvRejectionReason.setText("Reason: " + document.getRejectionReason());
            } else {
                holder.rejectionSection.setVisibility(View.GONE);
            }

            // Button visibility
            if (document.isVerified()) {
                holder.btnVerify.setVisibility(View.GONE);
                holder.btnReject.setVisibility(View.VISIBLE);
            } else if (document.isRejected()) {
                holder.btnVerify.setVisibility(View.VISIBLE);
                holder.btnReject.setVisibility(View.GONE);
            } else {
                holder.btnVerify.setVisibility(View.VISIBLE);
                holder.btnReject.setVisibility(View.VISIBLE);
            }

            holder.btnView.setOnClickListener(v -> openDocument(document));
            holder.btnVerify.setOnClickListener(v -> verifyDocument(document));
            holder.btnReject.setOnClickListener(v -> showRejectDialog(document));
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
            ImageView ivDocIcon, ivVerified, ivRejected;
            TextView tvDocName, tvDocType, tvUploadDate, tvUploadedBy, tvRejectionReason;
            MaterialButton btnView, btnVerify, btnReject, btnDelete;
            LinearLayout rejectionSection;

            public DocumentViewHolder(@NonNull View itemView) {
                super(itemView);
                ivDocIcon = itemView.findViewById(R.id.ivDocIcon);
                ivVerified = itemView.findViewById(R.id.ivVerified);
                ivRejected = itemView.findViewById(R.id.ivRejected);
                tvDocName = itemView.findViewById(R.id.tvDocName);
                tvDocType = itemView.findViewById(R.id.tvDocType);
                tvUploadDate = itemView.findViewById(R.id.tvUploadDate);
                tvUploadedBy = itemView.findViewById(R.id.tvUploadedBy);
                tvRejectionReason = itemView.findViewById(R.id.tvRejectionReason);
                btnView = itemView.findViewById(R.id.btnView);
                btnVerify = itemView.findViewById(R.id.btnVerify);
                btnReject = itemView.findViewById(R.id.btnReject);
                btnDelete = itemView.findViewById(R.id.btnDelete);
                rejectionSection = itemView.findViewById(R.id.rejectionSection);
            }
        }
    }
}