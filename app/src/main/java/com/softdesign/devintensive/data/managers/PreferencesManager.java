package com.softdesign.devintensive.data.managers;

import android.content.SharedPreferences;

import com.softdesign.devintensive.utils.App;

import java.util.ArrayList;
import java.util.List;

import static com.softdesign.devintensive.utils.Constants.USER_BIO_KEY;
import static com.softdesign.devintensive.utils.Constants.USER_EMAIL_KEY;
import static com.softdesign.devintensive.utils.Constants.USER_GIT_KEY;
import static com.softdesign.devintensive.utils.Constants.USER_PHONE_KEY;
import static com.softdesign.devintensive.utils.Constants.USER_VK_KEY;

/**
 * Class to manage {@link android.content.SharedPreferences}
 *
 * @author Sergey Vorobyev
 */

public class PreferencesManager {

    private static final String[] USER_FIELDS = {
            USER_PHONE_KEY,
            USER_EMAIL_KEY,
            USER_VK_KEY,
            USER_GIT_KEY,USER_BIO_KEY
    };

    private SharedPreferences mPreferences;

    public PreferencesManager() {
        mPreferences = App.getSharedPreferences();
    }

    public void saveUserProfileData(List<String> data) {
        SharedPreferences.Editor editor = mPreferences.edit();
        for (int i = 0 ; i < USER_FIELDS.length; i++) {
            editor.putString(USER_FIELDS[i], data.get(i));
        }
        editor.apply();
    }

    public List<String> loadUserProfileData() {
        List<String> data = new ArrayList<>(5);
        for (String field : USER_FIELDS) {
            data.add(mPreferences.getString(field, null));
        }
        return data;
    }
}