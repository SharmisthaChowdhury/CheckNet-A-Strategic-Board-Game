package com.example.bluetoothconnection;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class Netmate extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.case_netmate);

        // Initialize the back button
        ImageButton backButton = findViewById(R.id.back);  // Ensure this ID matches the back button ID in Netmate layout

        // Set a click listener for the back button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();  // Close the current Netmate activity and return to the previous activity
            }
        });

        // Initialize the home button
        ImageButton homeButton = findViewById(R.id.home);  // Ensure this ID matches the home button ID in Netmate layout

        // Set a click listener for the home button
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to MainActivity (the home page)
                Intent intent = new Intent(Netmate.this, MainActivity2.class);
                startActivity(intent);

                // Optionally, finish the current activity
                finish();
            }
        });
    }
}