package com.example.campusrun;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText etOldPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private Switch switchNotification;
    private SharedPreferences userPrefs;
    private SharedPreferences settingPrefs;
    private DBManager dbManager;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        userPrefs = getSharedPreferences("user", MODE_PRIVATE);
        settingPrefs = getSharedPreferences("settings", MODE_PRIVATE);
        username = userPrefs.getString("username", "");
        dbManager = new DBManager(this);

        TextView tvCurrentUser = findViewById(R.id.tvCurrentUser);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        switchNotification = findViewById(R.id.switchNotification);
        Button btnChangePassword = findViewById(R.id.btnChangePassword);
        Button btnLogout = findViewById(R.id.btnLogout);

        tvCurrentUser.setText("\u5f53\u524d\u8d26\u53f7\uff1a" + username);
        switchNotification.setChecked(settingPrefs.getBoolean("message_notification", true));

        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingPrefs.edit().putBoolean("message_notification", isChecked).apply();
            Toast.makeText(this,
                    isChecked ? "\u5df2\u5f00\u542f\u6d88\u606f\u901a\u77e5" : "\u5df2\u5173\u95ed\u6d88\u606f\u901a\u77e5",
                    Toast.LENGTH_SHORT).show();
        });

        btnChangePassword.setOnClickListener(v -> changePassword());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void changePassword() {
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "\u8bf7\u586b\u5199\u5b8c\u6574\u5bc6\u7801\u4fe1\u606f", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "\u65b0\u5bc6\u7801\u81f3\u5c11\u0036\u4f4d", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "\u4e24\u6b21\u65b0\u5bc6\u7801\u4e0d\u4e00\u81f4", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dbManager.changePassword(username, oldPassword, newPassword)) {
            Toast.makeText(this, "\u5bc6\u7801\u4fee\u6539\u6210\u529f\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55", Toast.LENGTH_SHORT).show();
            logout();
        } else {
            Toast.makeText(this, "\u539f\u5bc6\u7801\u9519\u8bef", Toast.LENGTH_SHORT).show();
        }
    }

    private void logout() {
        userPrefs.edit().clear().apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
