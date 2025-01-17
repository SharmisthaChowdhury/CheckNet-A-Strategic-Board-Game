package com.example.bluetoothconnection;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class RulesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rules);

        // Initialize the Checknet button
        Button checknetButton = findViewById(R.id.case2);
        checknetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RulesActivity.this, Checknet.class);
                startActivity(intent);
            }
        });

        // Initialize the Mill button
        Button millButton = findViewById(R.id.case1);
        millButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RulesActivity.this, Mill.class);
                startActivity(intent);
            }
        });

        // Initialize the Netmate button
        Button netmateButton = findViewById(R.id.case3);
        netmateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RulesActivity.this, Netmate.class);
                startActivity(intent);
            }
        });

        // Initialize the back button (ImageButton)
        ImageButton backButton = findViewById(R.id.back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close RulesActivity and return to the previous activity
            }
        });
    }
}