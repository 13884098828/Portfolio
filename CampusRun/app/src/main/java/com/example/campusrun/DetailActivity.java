package com.example.campusrun;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import bean.Task;

public class DetailActivity extends AppCompatActivity {

    TextView tvTitle, tvRoute, tvPrice, tvState, tvPeople, tvTime, tvRemark;
    Button btnPrimary, btnSecondary;
    DBManager dbManager;

    int taskId;
    String currentUser;
    Task task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        SharedPreferences sp = getSharedPreferences("user", MODE_PRIVATE);
        currentUser = sp.getString("username", "");
        dbManager = new DBManager(this);

        tvTitle = findViewById(R.id.tv_detail_title);
        tvRoute = findViewById(R.id.tv_detail_route);
        tvPrice = findViewById(R.id.tv_detail_price);
        tvState = findViewById(R.id.tv_detail_state);
        tvPeople = findViewById(R.id.tv_detail_people);
        tvTime = findViewById(R.id.tv_detail_time);
        tvRemark = findViewById(R.id.tv_detail_remark);
        btnPrimary = findViewById(R.id.btn_primary);
        btnSecondary = findViewById(R.id.btn_secondary);

        taskId = getIntent().getIntExtra("taskId", -1);
        loadTask();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (taskId != -1) loadTask();
    }

    private void loadTask() {
        task = dbManager.getTaskById(taskId);
        if (task == null) {
            Toast.makeText(this, "\u8ba2\u5355\u4e0d\u5b58\u5728", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitle.setText(task.getTitle());
        tvState.setText("\u72b6\u6001\uff1a" + task.getState());
        tvPrice.setText("\u4f63\u91d1\uff1a\u00a5" + DBManager.money(task.getPrice()));
        tvRoute.setText("\u53d6\u8d27\uff1a" + safe(task.getPickupAddress()) +
                "\n\u9001\u8fbe\uff1a" + safe(task.getDeliveryAddress()) +
                "\n\u7535\u8bdd\uff1a" + safe(task.getPhone()));
        tvPeople.setText("\u53d1\u5e03\u4eba\uff1a" + safe(task.getPublisher()) +
                "\n\u63a5\u5355\u4eba\uff1a" + emptyAsNone(task.getAcceptUser()));
        tvTime.setText("\u53d1\u5e03\u65f6\u95f4\uff1a" + emptyAsNone(task.getCreateTime()) +
                "\n\u63a5\u5355\u65f6\u95f4\uff1a" + emptyAsNone(task.getAcceptTime()) +
                "\n\u9001\u8fbe\u65f6\u95f4\uff1a" + emptyAsNone(task.getFinishTime()) +
                "\n\u7ed3\u7b97\u65f6\u95f4\uff1a" + emptyAsNone(task.getSettleTime()));
        tvRemark.setText("\u5907\u6ce8\uff1a" + emptyAsNone(task.getRemark()));

        refreshActions();
    }

    private void refreshActions() {
        btnPrimary.setVisibility(View.GONE);
        btnSecondary.setVisibility(View.GONE);
        btnPrimary.setOnClickListener(null);
        btnSecondary.setOnClickListener(null);

        boolean isPublisher = currentUser.equals(task.getPublisher());
        boolean isRunner = currentUser.equals(task.getAcceptUser());

        if (DBManager.STATE_WAITING.equals(task.getState())) {
            if (isPublisher) {
                btnPrimary.setVisibility(View.VISIBLE);
                btnPrimary.setText("\u5220\u9664\u8ba2\u5355");
                btnPrimary.setOnClickListener(v -> {
                    if (dbManager.deleteWaitingTask(taskId, currentUser)) {
                        Toast.makeText(this, "\u5df2\u5220\u9664\uff0c\u4f63\u91d1\u5df2\u9000\u56de", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } else {
                btnPrimary.setVisibility(View.VISIBLE);
                btnPrimary.setText("\u7acb\u5373\u63a5\u5355");
                btnPrimary.setOnClickListener(v -> {
                    if (dbManager.acceptTask(taskId, currentUser)) {
                        Toast.makeText(this, "\u63a5\u5355\u6210\u529f", Toast.LENGTH_SHORT).show();
                        loadTask();
                    } else {
                        Toast.makeText(this, "\u63a5\u5355\u5931\u8d25", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else if (DBManager.STATE_ACCEPTED.equals(task.getState()) && isRunner) {
            btnPrimary.setVisibility(View.VISIBLE);
            btnPrimary.setText("\u6807\u8bb0\u5df2\u9001\u8fbe");
            btnPrimary.setOnClickListener(v -> {
                if (dbManager.markDelivered(taskId, currentUser)) {
                    Toast.makeText(this, "\u5df2\u901a\u77e5\u53d1\u5e03\u8005\u786e\u8ba4", Toast.LENGTH_SHORT).show();
                    loadTask();
                }
            });
        } else if (DBManager.STATE_FINISHED.equals(task.getState()) && isPublisher) {
            btnPrimary.setVisibility(View.VISIBLE);
            btnPrimary.setText("\u786e\u8ba4\u5b8c\u6210\u5e76\u7ed3\u7b97");
            btnPrimary.setOnClickListener(v -> {
                if (dbManager.confirmFinish(taskId, currentUser)) {
                    Toast.makeText(this, "\u7ed3\u7b97\u6210\u529f", Toast.LENGTH_SHORT).show();
                    loadTask();
                }
            });
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String emptyAsNone(String value) {
        return value == null || value.isEmpty() ? "\u6682\u65e0" : value;
    }
}
