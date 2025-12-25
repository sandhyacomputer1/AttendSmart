package com.sandhyyasofttech.attendsmart.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {

    private static final String PREF_NAME = "ServiceControlPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_COMPANY_KEY = "companyKey";
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    public PrefManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveUserEmail(String email) {
        editor.putString(KEY_EMAIL, email);
        editor.commit();
    }
    public String getEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }
    public String getUserEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }

    public void logout() {
        editor.clear();
        editor.commit();
    }
    public void saveUserType(String userType) {
        editor.putString(KEY_USER_TYPE, userType);
        editor.commit();
    }

    public String getUserType() {
        return sharedPreferences.getString(KEY_USER_TYPE, null);
    }

    public void saveCompanyKey(String companyKey) {
        editor.putString(KEY_COMPANY_KEY, companyKey);
        editor.commit();
    }

    public String getCompanyKey() {
        return sharedPreferences.getString(KEY_COMPANY_KEY, null);
    }
}
