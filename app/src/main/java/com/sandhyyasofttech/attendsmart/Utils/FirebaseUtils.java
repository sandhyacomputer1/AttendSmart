package com.sandhyyasofttech.attendsmart.Utils;

import com.google.firebase.database.DataSnapshot;
import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
import android.util.Log;

public class FirebaseUtils {

    private static final String TAG = "FirebaseUtils";

    public static EmployeeModel safeParseEmployee(DataSnapshot snapshot) {
        try {
            if (!snapshot.exists()) return null;

            EmployeeModel model = new EmployeeModel();
            
            // ✅ SAFE conversion for ALL fields
            model.setEmployeeId(safeToString(snapshot.child("employeeId")));
            model.setEmployeeName(safeToString(snapshot.child("employeeName")));
            model.setEmployeeMobile(safeToString(snapshot.child("employeeMobile")));
            model.setEmployeeRole(safeToString(snapshot.child("employeeRole")));
            model.setEmployeeEmail(safeToString(snapshot.child("employeeEmail")));
            model.setEmployeeDepartment(safeToString(snapshot.child("employeeDepartment")));
            model.setEmployeeStatus(safeToString(snapshot.child("employeeStatus")));
            model.setEmployeeShift(safeToString(snapshot.child("employeeShift")));
            model.setCreatedAt(safeToString(snapshot.child("createdAt")));
            model.setWeeklyHoliday(safeToString(snapshot.child("weeklyHoliday")));
            model.setJoinDate(safeToString(snapshot.child("joinDate")));

            // Validate essential data
            if (model.getEmployeeName() == null || model.getEmployeeName().trim().isEmpty()) {
                return null;
            }
            
            return model;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse employee: " + e.getMessage());
            return null;
        }
    }

    private static String safeToString(DataSnapshot dataSnap) {
        if (!dataSnap.exists()) return null;
        
        Object value = dataSnap.getValue();
        if (value == null) return null;
        
        // ✅ Handle ALL number types → String
        if (value instanceof Long) return ((Long) value).toString();
        if (value instanceof Number) return String.valueOf(value);
        
        return value.toString();
    }
}
