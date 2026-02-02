package com.sandhyyasofttech.attendsmart.Activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import java.util.List;
import java.util.Locale;

public class EmployeeDocumentActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private RecyclerView rvDocuments;
    private TextView tvEmpty, tvTotalDocs, tvPendingRequests;
    private FloatingActionButton fabAddDocument;
    private ProgressDialog progressDialog;
    private MaterialCardView cardPendingRequests;
    private View emptyState, progressBar;

    // Adapter
    private DocumentAdapter documentAdapter;
    private List<DocumentModel> documentList = new ArrayList<>();

    // Firebase
    private DatabaseReference documentsRef, requestsRef;
    private StorageReference storageRef;

    // Document Categories
    private List<String> documentCategories = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;

    // Permissions
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private static final int REQUEST_PICK_DOCUMENT = 1002;
    private static final int REQUEST_CAMERA = 1003;

    // User info
    private String companyKey, employeeMobile, employeeName;
    private Uri selectedFileUri;
    private String selectedCategory = "";
    private String selectedDocumentName = "";

    // Request tracking
    private int pendingRequestsCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_documents);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }

        initViews();
        loadSession();
        setupToolbar();
        loadCategories();
        loadDocuments();
        loadPendingRequests();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvDocuments = findViewById(R.id.rvDocuments);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvTotalDocs = findViewById(R.id.tvTotalDocs);
        fabAddDocument = findViewById(R.id.fabAddDocument);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);

        // These are optional - only if you added the pending requests card
        tvPendingRequests = findViewById(R.id.tvPendingRequests);
        cardPendingRequests = findViewById(R.id.cardPendingRequests);

        // Setup RecyclerView
        rvDocuments.setLayoutManager(new LinearLayoutManager(this));
        documentAdapter = new DocumentAdapter(documentList);
        rvDocuments.setAdapter(documentAdapter);

        // Setup FAB
        fabAddDocument.setOnClickListener(v -> showCategorySelectionDialog());

        // Setup Progress Dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // Setup Pending Requests Card Click (only if card exists)
        if (cardPendingRequests != null) {
            cardPendingRequests.setOnClickListener(v -> showPendingRequestsDialog());
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }

        // Initialize categories list
        documentCategories.add("Select Document Type");
        documentCategories.add("Aadhaar Card");
        documentCategories.add("PAN Card");
        documentCategories.add("Passport");
        documentCategories.add("Driving License");
        documentCategories.add("Voter ID");
        documentCategories.add("Ration Card");
        documentCategories.add("Bank Passbook");
        documentCategories.add("Salary Slip");
        documentCategories.add("Offer Letter");
        documentCategories.add("Experience Letter");
        documentCategories.add("Education Certificate");
        documentCategories.add("Resume");
        documentCategories.add("Photo");
        documentCategories.add("Other Document");
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Documents");
        }

        // Set navigation icon color to white
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }


    private void loadSession() {
        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();
        employeeMobile = pref.getEmployeeMobile();
        employeeName = pref.getEmployeeName();

        if (TextUtils.isEmpty(companyKey) || TextUtils.isEmpty(employeeMobile)) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase references
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

    private void loadCategories() {
        DatabaseReference categoriesRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("documentCategories");

        categoriesRef.addValueEventListener(new ValueEventListener() {
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
                // Use default categories if Firebase load fails
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
                        DocumentModel document = docSnap.getValue(DocumentModel.class);
                        if (document != null) {
                            if (document.getDocId() == null || document.getDocId().isEmpty()) {
                                document.setDocId(docSnap.getKey());
                            }
                            documentList.add(document);
                        }
                    }
                }

                documentAdapter.notifyDataSetChanged();
                showLoading(false);
                updateEmptyState();
                updateStats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(EmployeeDocumentActivity.this,
                        "Failed to load documents: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPendingRequests() {
        if (requestsRef == null) return;

        requestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingRequestsCount = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot requestSnap : snapshot.getChildren()) {
                        String status = requestSnap.child("status").getValue(String.class);
                        if ("pending".equals(status)) {
                            pendingRequestsCount++;
                        }
                    }
                }

                updateRequestsUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("EmployeeDocActivity", "Failed to load requests: " + error.getMessage());
            }
        });
    }

    private void updateRequestsUI() {
        // Only update if the views exist in your layout
        if (cardPendingRequests != null && tvPendingRequests != null) {
            if (pendingRequestsCount > 0) {
                cardPendingRequests.setVisibility(View.VISIBLE);
                tvPendingRequests.setText(pendingRequestsCount + " document request" +
                        (pendingRequestsCount > 1 ? "s" : "") + " from admin");
            } else {
                cardPendingRequests.setVisibility(View.GONE);
            }
        }
    }

    private void showPendingRequestsDialog() {
        requestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder message = new StringBuilder();
                message.append("Admin has requested the following documents:\n\n");

                int count = 0;
                for (DataSnapshot requestSnap : snapshot.getChildren()) {
                    String status = requestSnap.child("status").getValue(String.class);
                    if ("pending".equals(status)) {
                        count++;
                        String docType = requestSnap.child("documentType").getValue(String.class);
                        String msg = requestSnap.child("message").getValue(String.class);
                        Long timestamp = requestSnap.child("timestamp").getValue(Long.class);

                        message.append(count).append(". ");
                        if (docType != null && !docType.isEmpty()) {
                            message.append(docType);
                        } else {
                            message.append("Document");
                        }

                        if (msg != null && !msg.isEmpty()) {
                            message.append("\n   Note: ").append(msg);
                        }

                        if (timestamp != null) {
                            String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                    .format(new Date(timestamp));
                            message.append("\n   Requested on: ").append(date);
                        }
                        message.append("\n\n");
                    }
                }

                if (count == 0) {
                    message.append("No pending requests.");
                }

                new MaterialAlertDialogBuilder(EmployeeDocumentActivity.this)
                        .setTitle("📋 Pending Document Requests")
                        .setMessage(message.toString())
                        .setPositiveButton("Upload Now", (dialog, which) -> showCategorySelectionDialog())
                        .setNegativeButton("Later", null)
                        .show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EmployeeDocumentActivity.this,
                        "Failed to load requests", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCategorySelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_document_type, null);
        builder.setView(dialogView);

        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        EditText etCustomName = dialogView.findViewById(R.id.etCustomName);
        MaterialButton btnNext = dialogView.findViewById(R.id.btnNext);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Setup spinner
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, documentCategories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedCategory = documentCategories.get(position);
                    etCustomName.setText(selectedCategory);
                    etCustomName.setEnabled(false);
                } else {
                    selectedCategory = "";
                    etCustomName.setText("");
                    etCustomName.setEnabled(true);
                    etCustomName.setHint("Enter document name");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCategory = "";
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        btnNext.setOnClickListener(v -> {
            selectedDocumentName = etCustomName.getText().toString().trim();

            if (TextUtils.isEmpty(selectedCategory) && TextUtils.isEmpty(selectedDocumentName)) {
                Toast.makeText(this, "Please select or enter document name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(selectedDocumentName)) {
                selectedDocumentName = selectedCategory;
            }

            dialog.dismiss();
            showUploadOptionsDialog();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void showUploadOptionsDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Choose File", "Cancel"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Upload " + selectedDocumentName)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: checkCameraPermission(); break;
                        case 1: checkStoragePermissionForGallery(); break;
                        case 2: checkStoragePermissionForFile(); break;
                        case 3: dialog.dismiss(); break;
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // Permission methods
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        } else {
            openCamera();
        }
    }

    private void checkStoragePermissionForGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        } else {
            openGallery();
        }
    }

    private void checkStoragePermissionForFile() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        } else {
            openFilePicker();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAMERA);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_DOCUMENT);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Document"), REQUEST_PICK_DOCUMENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CAMERA) {
                selectedFileUri = data.getData();
                if (selectedFileUri == null) {
                    Toast.makeText(this, "Could not get image", Toast.LENGTH_SHORT).show();
                    return;
                }
                showDocumentInfoDialog();
            } else if (requestCode == REQUEST_PICK_DOCUMENT) {
                selectedFileUri = data.getData();
                showDocumentInfoDialog();
            }
        }
    }

    private void showDocumentInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_document_info, null);
        builder.setView(dialogView);

        TextView tvDocName = dialogView.findViewById(R.id.tvDocName);
        EditText etDocDescription = dialogView.findViewById(R.id.etDocDescription);
        MaterialButton btnUpload = dialogView.findViewById(R.id.btnUpload);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        ImageView ivFilePreview = dialogView.findViewById(R.id.ivFilePreview);
        TextView tvFileInfo = dialogView.findViewById(R.id.tvFileInfo);

        tvDocName.setText(selectedDocumentName);

        if (isImageFile(selectedFileUri)) {
            Glide.with(this)
                    .load(selectedFileUri)
                    .placeholder(R.drawable.ic_image)
                    .into(ivFilePreview);
        } else {
            int iconRes = getIconForFileType(getFileExtension(selectedFileUri));
            ivFilePreview.setImageResource(iconRes);
        }

        String fileName = getFileName(selectedFileUri);
        String fileSize = getFileSize(selectedFileUri);
        tvFileInfo.setText(fileName + "\n" + fileSize);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnUpload.setOnClickListener(v -> {
            String description = etDocDescription.getText().toString().trim();
            uploadDocument(selectedDocumentName, selectedCategory, description);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void uploadDocument(String docName, String docType, String description) {
        if (selectedFileUri == null) {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Uploading " + docName + "...");
        progressDialog.show();

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = docName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp;
        StorageReference fileRef = storageRef.child(fileName);

        fileRef.putFile(selectedFileUri)
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressDialog.setMessage("Uploading: " + (int) progress + "%");
                })
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveDocumentToDatabase(docName, docType, description, uri.toString(), fileName);
                    });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveDocumentToDatabase(String docName, String docType, String description,
                                        String downloadUrl, String fileName) {
        String docId = documentsRef.push().getKey();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        long timestamp = System.currentTimeMillis();

        DocumentModel document = new DocumentModel();
        document.setDocId(docId);
        document.setDocName(docName);
        document.setDocType(docType);
        document.setDescription(description);
        document.setFileUrl(downloadUrl);
        document.setFileName(fileName);
        document.setUploadedBy(employeeMobile);
        document.setUploadedByName(employeeName);
        document.setUploadDate(currentDate);
        document.setUploadTimestamp(timestamp);
        document.setFileSize(getFileSize(selectedFileUri));
        document.setFileType(getFileExtension(selectedFileUri));
        document.setVerified(false);
        document.setRejected(false);

        documentsRef.child(docId).setValue(document)
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "✅ Document uploaded successfully", Toast.LENGTH_SHORT).show();

                    // Mark corresponding requests as fulfilled
                    markRequestsAsFulfilled(docType);

                    selectedFileUri = null;
                    selectedCategory = "";
                    selectedDocumentName = "";
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "❌ Failed to save", Toast.LENGTH_SHORT).show();
                });
    }

    private void markRequestsAsFulfilled(String documentType) {
        requestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot requestSnap : snapshot.getChildren()) {
                    String status = requestSnap.child("status").getValue(String.class);
                    String reqDocType = requestSnap.child("documentType").getValue(String.class);

                    if ("pending".equals(status) && documentType.equals(reqDocType)) {
                        requestSnap.getRef().child("status").setValue("fulfilled");
                        requestSnap.getRef().child("fulfilledDate")
                                .setValue(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Silently handle error
            }
        });
    }

    // Helper methods

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String getFileExtension(Uri uri) {
        String fileName = getFileName(uri);
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot != -1 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    private boolean isImageFile(Uri uri) {
        String extension = getFileExtension(uri);
        return extension.equals("jpg") || extension.equals("jpeg") ||
                extension.equals("png") || extension.equals("gif") ||
                extension.equals("bmp") || extension.equals("webp");
    }

    private String getFileSize(Uri uri) {
        try {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE);
                if (sizeIndex != -1) {
                    long sizeBytes = cursor.getLong(sizeIndex);
                    return formatFileSize(sizeBytes);
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    private String formatFileSize(long sizeBytes) {
        if (sizeBytes <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(sizeBytes) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.1f %s",
                sizeBytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private int getIconForFileType(String fileExtension) {
        if (fileExtension == null || fileExtension.isEmpty()) return R.drawable.document;
        switch (fileExtension.toLowerCase()) {
            case "pdf": return R.drawable.ic_pdf;
            case "jpg": case "jpeg": case "png": case "gif": case "bmp": case "webp": return R.drawable.ic_image;
            case "doc": case "docx": return R.drawable.ic_word;
            case "xls": case "xlsx": case "csv": return R.drawable.ic_excel;
            case "txt": case "rtf": return R.drawable.ic_text;
            case "zip": case "rar": case "7z": return R.drawable.ic_zip;
            case "ppt": case "pptx": return R.drawable.ic_powerpoint;
            default: return R.drawable.document;
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        rvDocuments.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState() {
        if (documentList.isEmpty()) {
            if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            rvDocuments.setVisibility(View.GONE);
        } else {
            if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            rvDocuments.setVisibility(View.VISIBLE);
        }
    }

    private void updateStats() {
        if (tvTotalDocs != null) {
            tvTotalDocs.setText(String.valueOf(documentList.size()));
        }
    }

    // ==================== DOCUMENT ADAPTER ====================
    private class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

        private List<DocumentModel> documents;

        public DocumentAdapter(List<DocumentModel> documents) {
            this.documents = documents;
        }

        @NonNull
        @Override
        public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_document, parent, false);
            return new DocumentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
            DocumentModel document = documents.get(position);
            if (document == null) return;

            holder.tvDocName.setText(document.getDocName());
            holder.tvDocType.setText(document.getDocType());
            holder.tvUploadDate.setText(document.getUploadDate());
            holder.tvFileSize.setText(document.getFileSize());

            int iconRes = getIconForFileType(document.getFileType());
            holder.ivDocIcon.setImageResource(iconRes);

            if (document.getFileType() != null &&
                    (document.getFileType().equals("jpg") || document.getFileType().equals("jpeg") ||
                            document.getFileType().equals("png"))) {
                Glide.with(EmployeeDocumentActivity.this)
                        .load(document.getFileUrl())
                        .placeholder(R.drawable.ic_image)
                        .thumbnail(0.1f)
                        .into(holder.ivDocIcon);
            }

            // Status indicators
            holder.ivVerified.setVisibility(document.isVerified() ? View.VISIBLE : View.GONE);
            holder.ivRejected.setVisibility(document.isRejected() ? View.VISIBLE : View.GONE);

            // Only set ivRequested if it exists in layout
            if (holder.ivRequested != null) {
                holder.ivRequested.setVisibility(document.isRequested() ? View.VISIBLE : View.GONE);
            }

            // Verification section
            if (document.isVerified()) {
                holder.verificationSection.setVisibility(View.VISIBLE);
                String verifiedBy = document.getVerifiedBy() != null ? document.getVerifiedBy() : "Admin";
                String verifiedDate = document.getVerifiedDate() != null ? document.getVerifiedDate() : "Recently";
                holder.tvVerificationInfo.setText("✓ Verified by " + verifiedBy + " on " + verifiedDate);
            } else {
                holder.verificationSection.setVisibility(View.GONE);
            }

            // Rejection section (only if it exists in layout)
            if (holder.rejectionSection != null && holder.tvRejectionReason != null) {
                if (document.isRejected()) {
                    holder.rejectionSection.setVisibility(View.VISIBLE);
                    String reason = document.getRejectionReason();
                    if (reason != null && !reason.isEmpty()) {
                        holder.tvRejectionReason.setText("Reason: " + reason + "\nPlease reupload this document.");
                    } else {
                        holder.tvRejectionReason.setText("This document was rejected. Please reupload.");
                    }
                } else {
                    holder.rejectionSection.setVisibility(View.GONE);
                }
            }

            // Request section (only if it exists in layout)
            if (holder.requestSection != null && holder.tvRequestInfo != null) {
                if (document.isRequested()) {
                    holder.requestSection.setVisibility(View.VISIBLE);
                    String requestedBy = document.getRequestedBy() != null ? document.getRequestedBy() : "Admin";
                    holder.tvRequestInfo.setText("Requested by " + requestedBy);
                } else {
                    holder.requestSection.setVisibility(View.GONE);
                }
            }

            holder.itemView.setOnClickListener(v -> openDocument(document));
            holder.btnView.setOnClickListener(v -> showViewOptions(document));
            holder.btnDelete.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(EmployeeDocumentActivity.this)
                        .setTitle("Delete Document")
                        .setMessage("Delete " + document.getDocName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> deleteDocument(document))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return documents.size();
        }

        class DocumentViewHolder extends RecyclerView.ViewHolder {
            ImageView ivDocIcon, ivVerified, ivRejected, ivRequested;
            TextView tvDocName, tvDocType, tvUploadDate, tvFileSize;
            TextView tvVerificationInfo, tvRejectionReason, tvRequestInfo;
            MaterialButton btnDelete, btnView;
            LinearLayout verificationSection, rejectionSection, requestSection;

            public DocumentViewHolder(@NonNull View itemView) {
                super(itemView);
                ivDocIcon = itemView.findViewById(R.id.ivDocIcon);
                ivVerified = itemView.findViewById(R.id.ivVerified);

                // Optional views - might not exist in layout
                ivRejected = itemView.findViewById(R.id.ivRejected);
                ivRequested = itemView.findViewById(R.id.ivRequested);

                tvDocName = itemView.findViewById(R.id.tvDocName);
                tvDocType = itemView.findViewById(R.id.tvDocType);
                tvUploadDate = itemView.findViewById(R.id.tvUploadDate);
                tvFileSize = itemView.findViewById(R.id.tvFileSize);
                tvVerificationInfo = itemView.findViewById(R.id.tvVerificationInfo);

                // Optional views
                tvRejectionReason = itemView.findViewById(R.id.tvRejectionReason);
                tvRequestInfo = itemView.findViewById(R.id.tvRequestInfo);

                btnDelete = itemView.findViewById(R.id.btnDelete);
                btnView = itemView.findViewById(R.id.btnView);
                verificationSection = itemView.findViewById(R.id.verificationSection);

                // Optional views
                rejectionSection = itemView.findViewById(R.id.rejectionSection);
                requestSection = itemView.findViewById(R.id.requestSection);
            }
        }
    }

    private void showViewOptions(DocumentModel document) {
        String[] options = {"Open Document", "Share Document", "View Details", "Update Document"};

        new MaterialAlertDialogBuilder(this)
                .setTitle(document.getDocName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: openDocument(document); break;
                        case 1: shareDocument(document); break;
                        case 2: showDocumentDetails(document); break;
                        case 3: updateDocument(document); break;
                    }
                })
                .show();
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
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "doc": case "docx": return "application/msword";
            case "xls": case "xlsx": return "application/vnd.ms-excel";
            case "txt": return "text/plain";
            default: return "*/*";
        }
    }

    private void shareDocument(DocumentModel document) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            String mimeType = getMimeType(document.getFileType());
            shareIntent.setType(mimeType != null ? mimeType : "*/*");
            shareIntent.putExtra(Intent.EXTRA_TEXT, document.getFileUrl());
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, document.getDocName());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share " + document.getDocName()));
        } catch (Exception e) {
            Toast.makeText(this, "Cannot share document", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDocumentDetails(DocumentModel document) {
        StringBuilder details = new StringBuilder();
        details.append("Type: ").append(document.getDocType()).append("\n\n");
        details.append("Uploaded: ").append(document.getUploadDate()).append("\n\n");
        details.append("Size: ").append(document.getFileSize()).append("\n\n");

        if (document.isVerified()) {
            details.append("Status: ✓ Verified\n");
            if (document.getVerifiedBy() != null) {
                details.append("Verified by: ").append(document.getVerifiedBy()).append("\n");
            }
            if (document.getVerifiedDate() != null) {
                details.append("Verified on: ").append(document.getVerifiedDate()).append("\n");
            }
        } else if (document.isRejected()) {
            details.append("Status: ✗ Rejected\n");
            if (document.getRejectionReason() != null) {
                details.append("Reason: ").append(document.getRejectionReason()).append("\n");
            }
            details.append("\nPlease reupload this document.");
        } else {
            details.append("Status: Pending verification\n");
        }

        if (document.getDescription() != null && !document.getDescription().isEmpty()) {
            details.append("\nDescription: ").append(document.getDescription());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(document.getDocName())
                .setMessage(details.toString())
                .setPositiveButton("Open", (dialog, which) -> openDocument(document))
                .setNegativeButton("Close", null)
                .show();
    }

    private void updateDocument(DocumentModel document) {
        selectedDocumentName = document.getDocName();
        selectedCategory = document.getDocType();
        showUploadOptionsDialog();
    }

    private void deleteDocument(DocumentModel document) {
        progressDialog.setMessage("Deleting...");
        progressDialog.show();

        StorageReference fileRef = storageRef.child(document.getFileName());
        fileRef.delete()
                .addOnSuccessListener(unused -> {
                    documentsRef.child(document.getDocId()).removeValue()
                            .addOnSuccessListener(unused1 -> {
                                progressDialog.dismiss();
                                Toast.makeText(this, "✅ Deleted successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(this, "❌ Failed to delete", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "❌ Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}