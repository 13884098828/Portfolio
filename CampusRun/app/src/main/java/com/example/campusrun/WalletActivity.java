package com.example.campusrun;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import adapter.WalletRecordAdapter;

public class WalletActivity extends AppCompatActivity {

    private TextView tvBalance;
    private TextView tvFrozen;
    private RecyclerView recyclerWallet;
    private DBManager dbManager;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        SharedPreferences sp = getSharedPreferences("user", MODE_PRIVATE);
        username = sp.getString("username", "");
        dbManager = new DBManager(this);

        tvBalance = findViewById(R.id.tvBalance);
        tvFrozen = findViewById(R.id.tvFrozen);
        Button btnRecharge = findViewById(R.id.btnRecharge);
        recyclerWallet = findViewById(R.id.recyclerWallet);
        recyclerWallet.setLayoutManager(new LinearLayoutManager(this));

        btnRecharge.setOnClickListener(v -> {
            if (dbManager.recharge(username, 50)) {
                Toast.makeText(this, "\u5145\u503c\u6210\u529f", Toast.LENGTH_SHORT).show();
                loadWallet();
            }
        });

        loadWallet();
    }

    private void loadWallet() {
        tvBalance.setText("\u53ef\u7528\u4f59\u989d\uff1a\u00a5" +
                DBManager.money(dbManager.getUserBalance(username)));
        tvFrozen.setText("\u51bb\u7ed3\u4f63\u91d1\uff1a\u00a5" +
                DBManager.money(dbManager.getFrozenAmount(username)));
        recyclerWallet.setAdapter(new WalletRecordAdapter(dbManager.getWalletRecords(username)));
    }
}
