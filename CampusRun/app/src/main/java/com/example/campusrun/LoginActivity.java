package com.example.campusrun;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin;
    TextView tvRegister;
    DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        dbManager = new DBManager(this);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "\u8bf7\u8f93\u5165\u7528\u6237\u540d\u548c\u5bc6\u7801", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbManager.login(username, password)) {
                SharedPreferences sp = getSharedPreferences("user", MODE_PRIVATE);
                sp.edit().putString("username", username).apply();

                Toast.makeText(this, "\u767b\u5f55\u6210\u529f", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "\u7528\u6237\u540d\u6216\u5bc6\u7801\u9519\u8bef", Toast.LENGTH_SHORT).show();
            }
        });

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }
}
