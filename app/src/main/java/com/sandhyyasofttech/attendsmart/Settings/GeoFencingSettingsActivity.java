package com.sandhyyasofttech.attendsmart.Settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Models.GeoFencingConfig;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GeoFencingSettingsActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_CODE = 101;

    private SwitchMaterial switchGeoFencing, switchTracking;
    private TextInputEditText etRadius, etAccuracy, etTrackingInterval;
    private Button btnSetCurrentLocation, btnSave, btnReset;
    private GoogleMap googleMap;
    private Circle geofenceCircle;

    private DatabaseReference geoFencingRef;
    private FusedLocationProviderClient fusedLocationClient;
    private String companyKey;

    private double selectedLat = 0;
    private double selectedLng = 0;
    private GeoFencingConfig currentConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geo_fencing_settings);

        initViews();
        setupFirebase();
        setupMap();
        loadCurrentConfig();
        setupListeners();
    }

    private void initViews() {
        switchGeoFencing = findViewById(R.id.switchGeoFencing);
        switchTracking = findViewById(R.id.switchTracking);
        etRadius = findViewById(R.id.etRadius);
        etAccuracy = findViewById(R.id.etAccuracy);
        etTrackingInterval = findViewById(R.id.etTrackingInterval);
        btnSetCurrentLocation = findViewById(R.id.btnSetCurrentLocation);
        btnSave = findViewById(R.id.btnSave);
        btnReset = findViewById(R.id.btnReset);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void setupFirebase() {
        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();
        geoFencingRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("companyInfo")
                .child("geoFencing");
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Request location permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        }

        // Set map click listener to select office location
        googleMap.setOnMapClickListener(latLng -> {
            selectedLat = latLng.latitude;
            selectedLng = latLng.longitude;
            updateMapMarker(latLng);
        });
    }

    private void loadCurrentConfig() {
        geoFencingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentConfig = snapshot.getValue(GeoFencingConfig.class);
                    if (currentConfig != null) {
                        populateFields(currentConfig);
                    }
                } else {
                    currentConfig = new GeoFencingConfig();
                    populateFields(currentConfig);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                toast("Failed to load config: " + error.getMessage());
            }
        });
    }

    private void populateFields(GeoFencingConfig config) {
        switchGeoFencing.setChecked(config.isEnabled());
        switchTracking.setChecked(config.isTrackingEnabled());
        etRadius.setText(String.valueOf(config.getRadiusMeters()));
        etAccuracy.setText(String.valueOf((int) config.getAccuracyThreshold()));
        etTrackingInterval.setText(String.valueOf(config.getTrackingInterval() / 60000)); // Convert to minutes

        selectedLat = config.getOfficeLat();
        selectedLng = config.getOfficeLng();

        if (selectedLat != 0 && selectedLng != 0 && googleMap != null) {
            LatLng officeLocation = new LatLng(selectedLat, selectedLng);
            updateMapMarker(officeLocation);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(officeLocation, 16));
        }
    }

    private void setupListeners() {
        btnSetCurrentLocation.setOnClickListener(v -> getCurrentLocation());
        btnSave.setOnClickListener(v -> saveConfiguration());
        btnReset.setOnClickListener(v -> resetToDefaults());

        switchGeoFencing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Enable/disable other controls based on main switch
            etRadius.setEnabled(isChecked);
            etAccuracy.setEnabled(isChecked);
            switchTracking.setEnabled(isChecked);
            etTrackingInterval.setEnabled(isChecked && switchTracking.isChecked());
            btnSetCurrentLocation.setEnabled(isChecked);
        });

        switchTracking.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etTrackingInterval.setEnabled(isChecked);
        });
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        selectedLat = location.getLatitude();
                        selectedLng = location.getLongitude();
                        LatLng currentLocation = new LatLng(selectedLat, selectedLng);
                        updateMapMarker(currentLocation);
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16));
                        toast("✓ Current location set as office location");
                    } else {
                        toast("Unable to get current location. Please try again.");
                    }
                })
                .addOnFailureListener(e -> {
                    toast("Error getting location: " + e.getMessage());
                });
    }

    private void updateMapMarker(LatLng location) {
        if (googleMap == null) return;

        googleMap.clear();

        // Add marker for office location
        googleMap.addMarker(new MarkerOptions()
                .position(location)
                .title("Office Location"));

        // Get radius value
        int radius = 100; // default
        try {
            String radiusText = etRadius.getText().toString().trim();
            if (!radiusText.isEmpty()) {
                radius = Integer.parseInt(radiusText);
            }
        } catch (NumberFormatException e) {
            radius = 100;
        }

        // Draw geofence circle
        if (geofenceCircle != null) {
            geofenceCircle.remove();
        }

        geofenceCircle = googleMap.addCircle(new CircleOptions()
                .center(location)
                .radius(radius)
                .strokeColor(0xFF2196F3)
                .fillColor(0x302196F3)
                .strokeWidth(2));
    }

    private void saveConfiguration() {
        // Validate inputs
        if (selectedLat == 0 && selectedLng == 0 && switchGeoFencing.isChecked()) {
            toast("Please set office location first");
            return;
        }

        String radiusText = etRadius.getText().toString().trim();
        String accuracyText = etAccuracy.getText().toString().trim();
        String intervalText = etTrackingInterval.getText().toString().trim();

        if (radiusText.isEmpty() || accuracyText.isEmpty() || intervalText.isEmpty()) {
            toast("Please fill all fields");
            return;
        }

        try {
            int radius = Integer.parseInt(radiusText);
            float accuracy = Float.parseFloat(accuracyText);
            long intervalMinutes = Long.parseLong(intervalText);

            // Validation
            if (radius < 10 || radius > 1000) {
                toast("Radius must be between 10 and 1000 meters");
                return;
            }

            if (accuracy < 5 || accuracy > 100) {
                toast("Accuracy threshold must be between 5 and 100 meters");
                return;
            }

            if (intervalMinutes < 1 || intervalMinutes > 60) {
                toast("Tracking interval must be between 1 and 60 minutes");
                return;
            }

            // Create config object
            Map<String, Object> configMap = new HashMap<>();
            configMap.put("enabled", switchGeoFencing.isChecked());
            configMap.put("officeLat", selectedLat);
            configMap.put("officeLng", selectedLng);
            configMap.put("radiusMeters", radius);
            configMap.put("accuracyThreshold", accuracy);
            configMap.put("trackingInterval", intervalMinutes * 60000); // Convert to milliseconds
            configMap.put("trackingEnabled", switchTracking.isChecked());
            configMap.put("updatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()).format(new Date()));
            configMap.put("updatedBy", new PrefManager(this).getUserEmail());

            // Save to Firebase
            geoFencingRef.setValue(configMap)
                    .addOnSuccessListener(aVoid -> {
                        toast("✓ Geo-fencing configuration saved successfully");
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        toast("Failed to save: " + e.getMessage());
                    });

        } catch (NumberFormatException e) {
            toast("Please enter valid numbers");
        }
    }

    private void resetToDefaults() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Reset to Defaults")
                .setMessage("This will reset all geo-fencing settings to default values. Continue?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    GeoFencingConfig defaultConfig = new GeoFencingConfig();
                    populateFields(defaultConfig);
                    selectedLat = 0;
                    selectedLng = 0;
                    if (googleMap != null) {
                        googleMap.clear();
                    }
                    toast("Reset to default values");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (googleMap != null) {
                    try {
                        googleMap.setMyLocationEnabled(true);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                toast("Location permission required for geo-fencing");
            }
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}