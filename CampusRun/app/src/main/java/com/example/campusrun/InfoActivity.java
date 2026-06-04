package com.example.campusrun;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class InfoActivity extends AppCompatActivity {

    public static final String TITLE = "title";
    public static final String CONTENT = "content";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        TextView tvTitle = findViewById(R.id.tvInfoTitle);
        TextView tvContent = findViewById(R.id.tvInfoContent);
        tvTitle.setText(getIntent().getStringExtra(TITLE));
        tvContent.setText(getIntent().getStringExtra(CONTENT));
    }
}
