package com.example.bluetoothconnection;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.text.InputType;
import com.google.firebase.FirebaseApp;
import android.util.Log;  // Import Log class for logging

public class InternetGameActivity extends AppCompatActivity {

    Button createGameButton, joinGameButton;
    EditText roomIdEditText;  // Added EditText for entering room ID

    private DatabaseReference databaseReference; // Declare a reference to Firebase Database

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("InternetGameActivity", "onCreate() started"); // Log onCreate method start

        try {
            FirebaseApp.initializeApp(this);
            Log.d("InternetGameActivity", "Firebase Initialized");  // Log Firebase initialization
        } catch (Exception e) {
            Log.e("InternetGameActivity", "Firebase Initialization failed", e);
        }

        setContentView(R.layout.activity_internet_game);

        try {
            // Initialize Firebase Database reference
            FirebaseDatabase database = FirebaseDatabase.getInstance();  // Initialize Firebase Database
            databaseReference = database.getReference("rooms");  // Reference to "rooms" in the database
            Log.d("InternetGameActivity", "Firebase Database Initialized");  // Log Firebase Database initialization
        } catch (Exception e) {
            Log.e("InternetGameActivity", "Firebase Database Initialization failed", e);
        }

        createGameButton = findViewById(R.id.createGameButton);
        joinGameButton = findViewById(R.id.joinGameButton);

        // Check Internet availability
        if (!isInternetAvailable()) {
            promptForInternet();
        }

        createGameButton.setOnClickListener(v -> {
            Toast.makeText(this, "Internet mode coming soon!", Toast.LENGTH_SHORT).show();
        });

        joinGameButton.setOnClickListener(v -> {
            Toast.makeText(this, "Internet mode coming soon!", Toast.LENGTH_SHORT).show();
        });

        ImageButton backButton = findViewById(R.id.back);
        backButton.setOnClickListener(v -> finish());

    }

    private boolean isInternetAvailable() {
        Log.d("InternetGameActivity", "Checking Internet availability");
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void promptForInternet() {
        Log.d("InternetGameActivity", "Prompting for internet");
        new AlertDialog.Builder(this)
                .setTitle("Internet Not Enabled")
                .setMessage("Your device is not connected to the internet. Would you like to enable Wi-Fi?")
                .setPositiveButton("Enable Wi-Fi", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivityForResult(intent, 1);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(InternetGameActivity.this, "Internet is required to play the game.", Toast.LENGTH_LONG).show();
                    //finish();
                })
                .setCancelable(false)
                .show();
    }

    private void createGame() {
        Log.d("InternetGameActivity", "Creating Game");

        String roomId = databaseReference.push().getKey();

        if (roomId != null) {
            GameRoom gameRoom = new GameRoom("Player1", "waiting");
            databaseReference.child(roomId).setValue(gameRoom)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Room Created. Room ID: " + roomId, Toast.LENGTH_LONG).show();

                        // Start the PlayActivity (or whatever activity handles the game)
                        Intent intent = new Intent(this, PlayActivity.class);
                        intent.putExtra("roomId", roomId);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to create room", Toast.LENGTH_SHORT).show();
                        Log.e("InternetGameActivity", "Failed to create room", e);
                    });
        } else {
            Toast.makeText(this, "Failed to generate Room ID", Toast.LENGTH_SHORT).show();
            Log.e("InternetGameActivity", "Room ID is null");
        }
    }


    private void joinGame() {
        Log.d("InternetGameActivity", "Joining Game");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Room ID");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Join", (dialog, which) -> {
            String roomId = input.getText().toString().trim();
            if (!roomId.isEmpty()) {
                Log.d("InternetGameActivity", "Joining Room: " + roomId);
                DatabaseReference roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId); // Reference to the specific room

                roomRef.child("status").setValue("connected").addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Joined Game Room: " + roomId, Toast.LENGTH_SHORT).show();

                    // Launch your actual game activity here
                    Intent intent = new Intent(this, PlayActivity.class);
                    intent.putExtra("roomId", roomId);
                    startActivity(intent);

                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to join the room.", Toast.LENGTH_SHORT).show();
                    Log.e("InternetGameActivity", "Failed to join the room: " + e.getMessage());
                });
            } else {
                Toast.makeText(this, "Room ID cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // GameRoom class for Firebase data model
    public static class GameRoom {
        private String owner;
        private String status;

        public GameRoom() { }

        public GameRoom(String owner, String status) {
            this.owner = owner;
            this.status = status;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
