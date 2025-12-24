package com.sandhyyasofttech.attendsmart.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {

    private static final String PREF_NAME = "ServiceControlPrefs";
    private static final String KEY_EMAIL = "email";

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

    public String getUserEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }

    public void logout() {
        editor.clear();
        editor.commit();
    }
}
