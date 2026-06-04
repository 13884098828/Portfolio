package com.example.campusrun;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnRegister;
    DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        dbManager = new DBManager(this);

        btnRegister.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "\u8bf7\u8f93\u5165\u5b8c\u6574\u4fe1\u606f", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbManager.register(username, password)) {
                Toast.makeText(this, "\u6ce8\u518c\u6210\u529f", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "\u7528\u6237\u540d\u5df2\u5b58\u5728", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
