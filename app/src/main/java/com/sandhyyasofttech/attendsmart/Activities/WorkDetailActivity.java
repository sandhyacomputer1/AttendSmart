package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.sandhyyasofttech.attendsmart.R;

public class WorkDetailActivity extends AppCompatActivity {

    private TextInputEditText etWorkDetail;
    private MaterialButton btnSave;

    private String workType;
    private String existingData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_detaill);
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }


        String title = getIntent().getStringExtra("title");
        String hint = getIntent().getStringExtra("hint");
        workType = getIntent().getStringExtra("type");
        existingData = getIntent().getStringExtra("existingData");

        // Toolbar Setup
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(title);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etWorkDetail = findViewById(R.id.etWorkDetail);
        btnSave = findViewById(R.id.btnSave);

        etWorkDetail.setHint(hint);

        // Load existing data
        if (!TextUtils.isEmpty(existingData)) {
            etWorkDetail.setText(existingData);
            etWorkDetail.setSelection(existingData.length());
        }

        btnSave.setOnClickListener(v -> saveWork());
    }

    private void saveWork() {
        String workData = etWorkDetail.getText().toString().trim();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("workData", workData);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}