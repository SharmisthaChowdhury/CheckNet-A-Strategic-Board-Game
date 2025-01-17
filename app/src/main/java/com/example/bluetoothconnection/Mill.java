package com.example.bluetoothconnection;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class Mill extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.case_mill);

        // Initialize the back button
        ImageButton backButton = findViewById(R.id.back);  // Ensure this ID matches the back button ID in Mill layout

        // Set a click listener for the back button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();  // Close the current Mill activity and return to RulesActivity
            }
        });
        // Initialize the home button
        ImageButton homeButton = findViewById(R.id.home);  // Ensure this ID matches the home button ID in Netmate layout

        // Set a click listener for the home button
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to MainActivity (the home page)
                Intent intent = new Intent(Mill.this, MainActivity2.class);
                startActivity(intent);

                // Optionally, finish the current activity
                finish();
            }
        });
    }
}