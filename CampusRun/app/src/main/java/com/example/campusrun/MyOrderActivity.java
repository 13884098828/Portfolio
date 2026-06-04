package com.example.campusrun;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import adapter.TaskAdapter;
import bean.Task;

public class MyOrderActivity extends AppCompatActivity {

    public static final String TYPE = "type";
    public static final String TYPE_PUBLISH = "publish";
    public static final String TYPE_ACCEPT = "accept";

    private TextView tvTitle;
    private TextView tvEmpty;
    private RecyclerView recyclerView;
    private DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_order);

        tvTitle = findViewById(R.id.tv_order_title);
        tvEmpty = findViewById(R.id.tv_empty);
        recyclerView = findViewById(R.id.recycler_my_order);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dbManager = new DBManager(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void loadOrders() {
        SharedPreferences sp = getSharedPreferences("user", MODE_PRIVATE);
        String username = sp.getString("username", "");
        String type = getIntent().getStringExtra(TYPE);

        List<Task> tasks;
        if (TYPE_ACCEPT.equals(type)) {
            tvTitle.setText("\u6211\u7684\u63a5\u5355");
            tasks = dbManager.getMyAccept(username);
        } else {
            tvTitle.setText("\u6211\u7684\u53d1\u5e03");
            tasks = dbManager.getMyPublish(username);
        }

        tvEmpty.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setAdapter(new TaskAdapter(tasks, task -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("taskId", task.getId());
            startActivity(intent);
        }));
    }
}
