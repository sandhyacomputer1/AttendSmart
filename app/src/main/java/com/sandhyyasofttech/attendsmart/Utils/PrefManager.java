package com.sandhyyasofttech.attendsmart.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {

    private static final String PREF_NAME = "AttendSmartPrefs";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;

    public PrefManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // ==================== COMPANY INFO ====================

    public void setCompanyKey(String companyKey) {
        editor.putString("companyKey", companyKey);
        editor.apply();
    }

    public String getCompanyKey() {
        return prefs.getString("companyKey", "");
    }

    public void saveCompanyKey(String companyKey) {
        editor.putString("companyKey", companyKey);
        editor.apply();
    }

    public void setCompanyName(String companyName) {
        editor.putString("companyName", companyName);
        editor.apply();
    }

    public String getCompanyName() {
        return prefs.getString("companyName", "");
    }

    public void setCompanyEmail(String email) {
        editor.putString("companyEmail", email);
        editor.apply();
    }

    public String getCompanyEmail() {
        return prefs.getString("companyEmail", "");
    }

    public void setCompanyAddress(String address) {
        editor.putString("companyAddress", address);
        editor.apply();
    }

    public String getCompanyAddress() {
        return prefs.getString("companyAddress", "");
    }

    // ==================== EMPLOYEE INFO ====================

    public void setEmployeeMobile(String mobile) {
        editor.putString("employeeMobile", mobile);
        editor.apply();
    }

    public String getEmployeeMobile() {
        return prefs.getString("employeeMobile", "");
    }

    public void setEmployeeName(String name) {
        editor.putString("employeeName", name);
        editor.apply();
    }

    public String getEmployeeName() {
        return prefs.getString("employeeName", "");
    }

    public void setEmployeeId(String id) {
        editor.putString("employeeId", id);
        editor.apply();
    }

    public String getEmployeeId() {
        return prefs.getString("employeeId", "");
    }

    public void setEmployeeEmail(String email) {
        editor.putString("employeeEmail", email);
        editor.apply();
    }

    public String getEmployeeEmail() {
        return prefs.getString("employeeEmail", "");
    }

    public void setEmployeePhone(String phone) {
        editor.putString("employeePhone", phone);
        editor.apply();
    }

    public String getEmployeePhone() {
        return prefs.getString("employeePhone", "");
    }

    public void setEmployeeAddress(String address) {
        editor.putString("employeeAddress", address);
        editor.apply();
    }

    public String getEmployeeAddress() {
        return prefs.getString("employeeAddress", "");
    }

    public void setEmployeeGender(String gender) {
        editor.putString("employeeGender", gender);
        editor.apply();
    }

    public String getEmployeeGender() {
        return prefs.getString("employeeGender", "");
    }

    public void setEmployeeDOB(String dob) {
        editor.putString("employeeDOB", dob);
        editor.apply();
    }

    public String getEmployeeDOB() {
        return prefs.getString("employeeDOB", "");
    }

    public void setEmployeeJoiningDate(String date) {
        editor.putString("employeeJoiningDate", date);
        editor.apply();
    }

    public String getEmployeeJoiningDate() {
        return prefs.getString("employeeJoiningDate", "");
    }

    // ==================== USER INFO ====================

    public void setUserEmail(String email) {
        editor.putString("userEmail", email);
        editor.apply();
    }

    public String getUserEmail() {
        return prefs.getString("userEmail", "");
    }

    public void saveUserEmail(String email) {
        editor.putString("userEmail", email);
        editor.apply();
    }

    public void setUserType(String type) {
        editor.putString("userType", type);
        editor.apply();
    }

    public String getUserType() {
        return prefs.getString("userType", "");
    }

    public void saveUserType(String type) {
        editor.putString("userType", type);
        editor.apply();
    }

    public void setUserId(String id) {
        editor.putString("userId", id);
        editor.apply();
    }

    public String getUserId() {
        return prefs.getString("userId", "");
    }

    public void setUserName(String name) {
        editor.putString("userName", name);
        editor.apply();
    }

    public String getUserName() {
        return prefs.getString("userName", "");
    }

    // ==================== LOGIN/SESSION ====================

    public void setLoggedIn(boolean isLoggedIn) {
        editor.putBoolean("isLoggedIn", isLoggedIn);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean("isLoggedIn", false);
    }

    public void setRememberMe(boolean remember) {
        editor.putBoolean("rememberMe", remember);
        editor.apply();
    }

    public boolean isRememberMe() {
        return prefs.getBoolean("rememberMe", false);
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean("isFirstTimeLaunch", isFirstTime);
        editor.apply();
    }

    public boolean isFirstTimeLaunch() {
        return prefs.getBoolean("isFirstTimeLaunch", true);
    }

    // ==================== DEPARTMENT & DESIGNATION ====================

    public void setDepartment(String department) {
        editor.putString("department", department);
        editor.apply();
    }

    public String getDepartment() {
        return prefs.getString("department", "");
    }

    public void setDepartmentId(String id) {
        editor.putString("departmentId", id);
        editor.apply();
    }

    public String getDepartmentId() {
        return prefs.getString("departmentId", "");
    }

    public void setDesignation(String designation) {
        editor.putString("designation", designation);
        editor.apply();
    }

    public String getDesignation() {
        return prefs.getString("designation", "");
    }

    public void setDesignationId(String id) {
        editor.putString("designationId", id);
        editor.apply();
    }

    public String getDesignationId() {
        return prefs.getString("designationId", "");
    }

    // ==================== SHIFT INFO ====================

    public void setShiftId(String shiftId) {
        editor.putString("shiftId", shiftId);
        editor.apply();
    }

    public String getShiftId() {
        return prefs.getString("shiftId", "");
    }

    public void setShiftName(String shiftName) {
        editor.putString("shiftName", shiftName);
        editor.apply();
    }

    public String getShiftName() {
        return prefs.getString("shiftName", "");
    }

    public void setShiftStartTime(String startTime) {
        editor.putString("shiftStartTime_old", startTime);
        editor.apply();
    }

    public String getShiftStartTime_old() {
        return prefs.getString("shiftStartTime_old", "");
    }

    public void setShiftEndTime(String endTime) {
        editor.putString("shiftEndTime", endTime);
        editor.apply();
    }

    public String getShiftEndTime() {
        return prefs.getString("shiftEndTime", "");
    }

    // ==================== PROFILE IMAGE ====================

    public void setProfileImageUrl(String url) {
        editor.putString("profileImageUrl", url);
        editor.apply();
    }

    public String getProfileImageUrl() {
        return prefs.getString("profileImageUrl", "");
    }

    public void setProfileImagePath(String path) {
        editor.putString("profileImagePath", path);
        editor.apply();
    }

    public String getProfileImagePath() {
        return prefs.getString("profileImagePath", "");
    }

    // ==================== SALARY INFO ====================

    public void setSalary(String salary) {
        editor.putString("salary", salary);
        editor.apply();
    }

    public String getSalary() {
        return prefs.getString("salary", "");
    }

    public void setSalaryType(String type) {
        editor.putString("salaryType", type);
        editor.apply();
    }

    public String getSalaryType() {
        return prefs.getString("salaryType", "");
    }

    // ==================== LOCATION INFO ====================

    public void setLatitude(double latitude) {
        editor.putString("latitude", String.valueOf(latitude));
        editor.apply();
    }

    public double getLatitude() {
        String lat = prefs.getString("latitude", "0.0");
        return Double.parseDouble(lat);
    }

    public void setLongitude(double longitude) {
        editor.putString("longitude", String.valueOf(longitude));
        editor.apply();
    }

    public double getLongitude() {
        String lng = prefs.getString("longitude", "0.0");
        return Double.parseDouble(lng);
    }

    public void setLocationAddress(String address) {
        editor.putString("locationAddress", address);
        editor.apply();
    }

    public String getLocationAddress() {
        return prefs.getString("locationAddress", "");
    }

    // ==================== ATTENDANCE INFO ====================

    public void setLastAttendanceDate(String date) {
        editor.putString("lastAttendanceDate", date);
        editor.apply();
    }

    public String getLastAttendanceDate() {
        return prefs.getString("lastAttendanceDate", "");
    }

    public void setTodayPunchInTime(String time) {
        editor.putString("todayPunchInTime", time);
        editor.apply();
    }

    public String getTodayPunchInTime() {
        return prefs.getString("todayPunchInTime", "");
    }

    public void setTodayPunchOutTime(String time) {
        editor.putString("todayPunchOutTime", time);
        editor.apply();
    }

    public String getTodayPunchOutTime() {
        return prefs.getString("todayPunchOutTime", "");
    }

    public void setIsPunchedIn(boolean isPunchedIn) {
        editor.putBoolean("isPunchedIn", isPunchedIn);
        editor.apply();
    }

    public boolean isPunchedIn() {
        return prefs.getBoolean("isPunchedIn", false);
    }

    // ==================== NOTIFICATION SETTINGS (NEW) ====================

    public void setNotificationsEnabled(boolean enabled) {
        editor.putBoolean("notificationsEnabled", enabled);
        editor.apply();
    }

    public boolean getNotificationsEnabled() {
        return prefs.getBoolean("notificationsEnabled", false);
    }



    public String getShiftStartTime() {
        return prefs.getString("shiftStartTime", null);
    }

    public void setReminderTime(String time) {
        editor.putString("reminderTime", time);
        editor.apply();
    }

    public String getReminderTime() {
        return prefs.getString("reminderTime", "5"); // Default 5 minutes
    }

    // ==================== APP SETTINGS ====================

    public void setLanguage(String language) {
        editor.putString("language", language);
        editor.apply();
    }

    public String getLanguage() {
        return prefs.getString("language", "en");
    }

    public void setTheme(String theme) {
        editor.putString("theme", theme);
        editor.apply();
    }

    public String getTheme() {
        return prefs.getString("theme", "light");
    }

    public void setBiometricEnabled(boolean enabled) {
        editor.putBoolean("biometricEnabled", enabled);
        editor.apply();
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean("biometricEnabled", false);
    }

    // ==================== FCM TOKEN ====================

    public void setFCMToken(String token) {
        editor.putString("fcmToken", token);
        editor.apply();
    }

    public String getFCMToken() {
        return prefs.getString("fcmToken", "");
    }

    // ==================== UTILITY METHODS ====================

    public void clearAll() {
        editor.clear();
        editor.apply();
    }

    public void clearSession() {
        editor.remove("isLoggedIn");
        editor.remove("isPunchedIn");
        editor.remove("todayPunchInTime");
        editor.remove("todayPunchOutTime");
        editor.apply();
    }

    public void logout() {
        String companyKey = getCompanyKey();
        String companyName = getCompanyName();
        boolean rememberMe = isRememberMe();
        String savedEmail = getUserEmail();

        editor.clear();

        // Restore if remember me is checked
        if (rememberMe) {
            editor.putString("companyKey", companyKey);
            editor.putString("companyName", companyName);
            editor.putString("userEmail", savedEmail);
            editor.putBoolean("rememberMe", true);
        }

        editor.apply();
    }

    // Save multiple values at once
    public void saveEmployeeData(String name, String id, String email, String mobile,
                                 String dept, String designation) {
        editor.putString("employeeName", name);
        editor.putString("employeeId", id);
        editor.putString("employeeEmail", email);
        editor.putString("employeeMobile", mobile);
        editor.putString("department", dept);
        editor.putString("designation", designation);
        editor.apply();
    }
}