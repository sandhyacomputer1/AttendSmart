package com.sandhyyasofttech.attendsmart.Utils;

import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Models.GeoFencingConfig;

import java.util.Locale;

public class GeoFencingHelper {

    private static final String TAG = "GeoFencingHelper";

    /**
     * Calculate distance between two geographic points using Haversine formula
     * via Android's Location.distanceBetween() method
     *
     * @param lat1 First point latitude
     * @param lng1 First point longitude
     * @param lat2 Second point latitude
     * @param lng2 Second point longitude
     * @return Distance in meters
     */
    public static float calculateDistance(double lat1, double lng1,
                                          double lat2, double lng2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0];
    }

    /**
     * Check if a location is inside the geofence
     *
     * @param currentLat Current latitude
     * @param currentLng Current longitude
     * @param officeLat Office latitude
     * @param officeLng Office longitude
     * @param radiusMeters Allowed radius in meters
     * @return true if inside geofence, false otherwise
     */
    public static boolean isInsideGeofence(double currentLat, double currentLng,
                                           double officeLat, double officeLng,
                                           int radiusMeters) {
        float distance = calculateDistance(currentLat, currentLng,
                officeLat, officeLng);

        Log.d(TAG, String.format("Distance from office: %.2f meters (Allowed: %d meters)",
                distance, radiusMeters));

        return distance <= radiusMeters;
    }

    /**
     * Check if GPS accuracy is acceptable
     *
     * @param accuracy Current GPS accuracy in meters
     * @param threshold Maximum acceptable accuracy
     * @return true if accuracy is good, false otherwise
     */
    public static boolean isAccuracyAcceptable(float accuracy, float threshold) {
        // If accuracy is 0 or negative, it's likely invalid GPS data
        if (accuracy <= 0) {
            Log.e(TAG, "Invalid accuracy: " + accuracy);
            return false;
        }

        // Check if accuracy is within threshold
        return accuracy <= threshold;
    }

    /**
     * Validate location data before processing
     *
     * @param lat Latitude
     * @param lng Longitude
     * @param accuracy GPS accuracy
     * @return true if valid, false otherwise
     */
    public static boolean isLocationValid(double lat, double lng, float accuracy) {
        // Check if coordinates are valid
        if (lat == 0.0 && lng == 0.0) {
            Log.e(TAG, "Invalid location: 0,0 coordinates");
            return false;
        }

        // Check if latitude is in valid range (-90 to 90)
        if (lat < -90 || lat > 90) {
            Log.e(TAG, "Invalid latitude: " + lat);
            return false;
        }

        // Check if longitude is in valid range (-180 to 180)
        if (lng < -180 || lng > 180) {
            Log.e(TAG, "Invalid longitude: " + lng);
            return false;
        }

        // Check if accuracy is reasonable (not zero or negative)
        if (accuracy <= 0 || accuracy > 500) {
            Log.e(TAG, "Invalid accuracy: " + accuracy);
            return false;
        }

        return true;
    }

    /**
     * Fetch geofencing configuration from Firebase
     *
     * @param companyKey Company identifier
     * @param callback Callback interface to return result
     */
    public static void fetchGeoFencingConfig(String companyKey,
                                             GeoFencingConfigCallback callback) {
        DatabaseReference configRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("companyInfo")
                .child("geoFencing");

        configRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    GeoFencingConfig config = snapshot.getValue(GeoFencingConfig.class);
                    if (config != null) {
                        callback.onSuccess(config);
                    } else {
                        callback.onFailure("Failed to parse geofencing config");
                    }
                } else {
                    // Return default config if not set
                    GeoFencingConfig defaultConfig = new GeoFencingConfig();
                    defaultConfig.setEnabled(false);
                    defaultConfig.setAccuracyThreshold(50);
                    defaultConfig.setRadiusMeters(100);
                    defaultConfig.setTrackingEnabled(false);
                    callback.onSuccess(defaultConfig);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    /**
     * Callback interface for geofencing config
     */
    public interface GeoFencingConfigCallback {
        void onSuccess(GeoFencingConfig config);
        void onFailure(String error);
    }

    /**
     * Format distance for display
     *
     * @param meters Distance in meters
     * @return Formatted string (e.g., "12.5 m" or "1.2 km")
     */
    public static String formatDistance(float meters) {
        if (meters < 1000) {
            return String.format(Locale.getDefault(), "%.0f m", meters);
        } else {
            return String.format(Locale.getDefault(), "%.2f km", meters / 1000);
        }
    }

    /**
     * Get user-friendly geofence status message
     *
     * @param isInside Whether user is inside geofence
     * @param distance Distance from office
     * @param radiusMeters Allowed radius
     *
     *
     * @return Status message
     */
    public static String getGeofenceStatusMessage(boolean isInside,
                                                  float distance,
                                                  int radiusMeters) {
        if (isInside) {
            return "✓ Inside office area (" + formatDistance(distance) + ")";
        } else {
            float overshoot = distance - radiusMeters;
            return "✗ Outside office area (+" + formatDistance(overshoot) + ")";
        }
    }
}