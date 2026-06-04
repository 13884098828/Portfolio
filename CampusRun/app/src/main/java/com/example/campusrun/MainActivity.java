package com.example.campusrun;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import fragment.HomeFragment;
import fragment.OrderFragment;
import fragment.MineFragment;
import fragment.PublishFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        replaceFragment(new HomeFragment());

        bottomNavigationView.setOnItemSelectedListener(item -> {

            if (item.getItemId() == R.id.nav_home) {
                replaceFragment(new HomeFragment());
            }

            else if (item.getItemId() == R.id.nav_publish) {
                replaceFragment(new PublishFragment());
            }

            else if (item.getItemId() == R.id.nav_order) {
                replaceFragment(new OrderFragment());
            }

            else if (item.getItemId() == R.id.nav_mine) {
                replaceFragment(new MineFragment());
            }

            return true;
        });
    }

    private void replaceFragment(Fragment fragment) {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}