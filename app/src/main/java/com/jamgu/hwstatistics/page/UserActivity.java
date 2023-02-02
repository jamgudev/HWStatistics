package com.jamgu.hwstatistics.page;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.jamgu.hwstatistics.R;

public class UserActivity extends AppCompatActivity {

    private Button vBtnNewUserRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        initView();
        initBtnEvent();
    }

    private void initView() {
        vBtnNewUserRegister = findViewById(R.id.vBtnNewUserRegister);
    }

    private void initBtnEvent() {
        vBtnNewUserRegister.setOnClickListener(view -> {
            Intent intent = new Intent(UserActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}