package com.sandhyyasofttech.attendsmart.Activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PersonalDetailsActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etMobile, etDOB, etReligion, etAddress, etCity,
            etState, etPinCode, etAadhaarNumber, etPanNumber, etPassportNumber, etFatherName,
            etMotherName, etSpouseName, etEmergencyContactName, etEmergencyContact, etEmergencyRelation;
    private AutoCompleteTextView spGender, spMaritalStatus, spBloodGroup, spNationality;
    private MaterialButton btnSave;
    private String dob = "";

    private DatabaseReference dbRef;
    private String companyKey, employeeMobile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }


        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();
        employeeMobile = pref.getEmployeeMobile();

        if (companyKey == null) companyKey = getIntent().getStringExtra("companyKey");
        if (employeeMobile == null) employeeMobile = getIntent().getStringExtra("employeeMobile");

        if (companyKey == null || employeeMobile == null) {
            toast("⚠️ Please login first");
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupDropdowns();
        setupDobPicker();
        loadPersonalData();
        setupSaveButton();
    }

    private void initViews() {
        etName = findViewById(R.id.etName); etEmail = findViewById(R.id.etEmail); etMobile = findViewById(R.id.etMobile);
        etDOB = findViewById(R.id.etDOB); etReligion = findViewById(R.id.etReligion);
        spGender = findViewById(R.id.spGender); spMaritalStatus = findViewById(R.id.spMaritalStatus);
        spBloodGroup = findViewById(R.id.spBloodGroup); spNationality = findViewById(R.id.spNationality);
        etAddress = findViewById(R.id.etAddress); etCity = findViewById(R.id.etCity);
        etState = findViewById(R.id.etState); etPinCode = findViewById(R.id.etPinCode);
        etAadhaarNumber = findViewById(R.id.etAadhaarNumber); etPanNumber = findViewById(R.id.etPanNumber);
        etPassportNumber = findViewById(R.id.etPassportNumber); etFatherName = findViewById(R.id.etFatherName);
        etMotherName = findViewById(R.id.etMotherName); etSpouseName = findViewById(R.id.etSpouseName);
        etEmergencyContactName = findViewById(R.id.etEmergencyContactName);
        etEmergencyContact = findViewById(R.id.etEmergencyContact); etEmergencyRelation = findViewById(R.id.etEmergencyRelation);
        btnSave = findViewById(R.id.btnSave);
        dbRef = FirebaseDatabase.getInstance().getReference();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Personal Details");
            }
        }
    }

    private void setupDropdowns() {
        setupDropdown(spGender, new String[]{"Male", "Female", "Other"});
        setupDropdown(spMaritalStatus, new String[]{"Single", "Married", "Divorced", "Widowed"});
        setupDropdown(spBloodGroup, new String[]{"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"});
        setupDropdown(spNationality, new String[]{"Indian", "American", "British", "Canadian", "Australian", "Other"});
    }

    private void setupDropdown(AutoCompleteTextView dropdown, String[] options) {
        if (dropdown != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, options);
            dropdown.setAdapter(adapter);
            dropdown.setThreshold(1);
        }
    }

    private void setupDobPicker() {
        etDOB.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar dobCal = Calendar.getInstance();
                        dobCal.set(year, month, dayOfMonth);

                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                        dob = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dobCal.getTime());
                        etDOB.setText(sdf.format(dobCal.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            // DOB must be past date only
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    private void loadPersonalData() {
        dbRef.child("Companies").child(companyKey).child("employees").child(employeeMobile).child("info")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        setText(etName, snapshot.child("employeeName").getValue(String.class));
                        setText(etEmail, snapshot.child("employeeEmail").getValue(String.class));
                        setText(etMobile, snapshot.child("employeeMobile").getValue(String.class));
                        setText(etDOB, snapshot.child("dob").getValue(String.class));
                        setText(etReligion, snapshot.child("religion").getValue(String.class));
                        setDropdownText(spGender, snapshot.child("gender").getValue(String.class));
                        setDropdownText(spMaritalStatus, snapshot.child("maritalStatus").getValue(String.class));
                        setDropdownText(spBloodGroup, snapshot.child("bloodGroup").getValue(String.class));
                        setDropdownText(spNationality, snapshot.child("nationality").getValue(String.class));
                        setText(etAddress, snapshot.child("address").getValue(String.class));
                        setText(etCity, snapshot.child("city").getValue(String.class));
                        setText(etState, snapshot.child("state").getValue(String.class));
                        setText(etPinCode, snapshot.child("pinCode").getValue(String.class));
                        setText(etAadhaarNumber, snapshot.child("aadhaarNumber").getValue(String.class));
                        setText(etPanNumber, snapshot.child("panNumber").getValue(String.class));
                        setText(etPassportNumber, snapshot.child("passportNumber").getValue(String.class));
                        setText(etFatherName, snapshot.child("fatherName").getValue(String.class));
                        setText(etMotherName, snapshot.child("motherName").getValue(String.class));
                        setText(etSpouseName, snapshot.child("spouseName").getValue(String.class));
                        setText(etEmergencyContactName, snapshot.child("emergencyContactName").getValue(String.class));
                        setText(etEmergencyContact, snapshot.child("emergencyContact").getValue(String.class));
                        setText(etEmergencyRelation, snapshot.child("emergencyRelation").getValue(String.class));
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void setText(TextInputEditText editText, String value) {
        if (editText != null && value != null) editText.setText(value);
    }

    private void setDropdownText(AutoCompleteTextView dropdown, String value) {
        if (dropdown != null && value != null) dropdown.setText(value, false);
    }

    private void setupSaveButton() {
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> savePersonalData());
        }
    }

    // ✅ FIXED: UNIVERSAL getText() method - Works for ALL EditText types
    private String getText(android.widget.EditText editText) {
        if (editText != null && editText.getText() != null) {
            return editText.getText().toString().trim();
        }
        return "";
    }

    private void savePersonalData() {
        String name = getText(etName);
        if (name.isEmpty()) {
            toast("Name is required");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("employeeName", name);

        // ✅ SAFE: Check if text is not empty before adding to updates
        String emailText = getText(etEmail);
        if (!emailText.isEmpty()) updates.put("employeeEmail", emailText);

        String mobileText = getText(etMobile);
        if (!mobileText.isEmpty()) updates.put("employeeMobile", mobileText);

        String dobText = getText(etDOB);
        if (!dobText.isEmpty()) updates.put("dob", dobText);

        String religionText = getText(etReligion);
        if (!religionText.isEmpty()) updates.put("religion", religionText);

        String genderText = getText(spGender);
        if (!genderText.isEmpty()) updates.put("gender", genderText);

        String maritalText = getText(spMaritalStatus);
        if (!maritalText.isEmpty()) updates.put("maritalStatus", maritalText);

        String bloodText = getText(spBloodGroup);
        if (!bloodText.isEmpty()) updates.put("bloodGroup", bloodText);

        String nationalityText = getText(spNationality);
        if (!nationalityText.isEmpty()) updates.put("nationality", nationalityText);

        String addressText = getText(etAddress);
        if (!addressText.isEmpty()) updates.put("address", addressText);

        String cityText = getText(etCity);
        if (!cityText.isEmpty()) updates.put("city", cityText);

        String stateText = getText(etState);
        if (!stateText.isEmpty()) updates.put("state", stateText);

        String pinText = getText(etPinCode);
        if (!pinText.isEmpty()) updates.put("pinCode", pinText);

        String aadhaarText = getText(etAadhaarNumber);
        if (!aadhaarText.isEmpty()) updates.put("aadhaarNumber", aadhaarText);

        String panText = getText(etPanNumber);
        if (!panText.isEmpty()) updates.put("panNumber", panText);

        String passportText = getText(etPassportNumber);
        if (!passportText.isEmpty()) updates.put("passportNumber", passportText);

        String fatherText = getText(etFatherName);
        if (!fatherText.isEmpty()) updates.put("fatherName", fatherText);

        String motherText = getText(etMotherName);
        if (!motherText.isEmpty()) updates.put("motherName", motherText);

        String spouseText = getText(etSpouseName);
        if (!spouseText.isEmpty()) updates.put("spouseName", spouseText);

        String emergencyNameText = getText(etEmergencyContactName);
        if (!emergencyNameText.isEmpty()) updates.put("emergencyContactName", emergencyNameText);

        String emergencyContactText = getText(etEmergencyContact);
        if (!emergencyContactText.isEmpty()) updates.put("emergencyContact", emergencyContactText);

        String emergencyRelationText = getText(etEmergencyRelation);
        if (!emergencyRelationText.isEmpty()) updates.put("emergencyRelation", emergencyRelationText);

        dbRef.child("Companies").child(companyKey).child("employees").child(employeeMobile).child("info")
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    toast("✅ Profile saved!");
                    finish();
                })
                .addOnFailureListener(e -> toast("❌ Save failed"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
