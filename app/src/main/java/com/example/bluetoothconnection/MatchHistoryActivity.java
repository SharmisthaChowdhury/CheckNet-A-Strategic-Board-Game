package com.example.bluetoothconnection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MatchHistoryActivity extends AppCompatActivity {

    private TextView gamesPlayedText, gamesWonText, gamesLostText, winStreakText;
    private Button resetButton;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_history);

        preferences = getSharedPreferences("MatchStats", MODE_PRIVATE);

        gamesPlayedText = findViewById(R.id.gamesPlayed);
        gamesWonText = findViewById(R.id.gamesWon);
        gamesLostText = findViewById(R.id.gamesLost);
        winStreakText = findViewById(R.id.winStreak);
        resetButton = findViewById(R.id.resetStats);

        loadStatistics();

        resetButton.setOnClickListener(v -> resetStatistics());

        Button okButton = findViewById(R.id.okButton);
        okButton.setOnClickListener(v -> {
            Intent intent = new Intent(MatchHistoryActivity.this, MainActivity2.class); // Replace with your Home activity class
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish(); // Optional: close this activity
        });

    }

    private void loadStatistics() {
        int played = preferences.getInt("gamesPlayed", 0);
        int won = preferences.getInt("gamesWon", 0);
        int lost = preferences.getInt("gamesLost", 0);
        int streak = preferences.getInt("winStreak", 0);

        gamesPlayedText.setText("Games Played: " + played);
        gamesWonText.setText("Games Won: " + won);
        gamesLostText.setText("Games Lost: " + lost);
        winStreakText.setText("Current Win Streak: " + streak);
    }

    private void resetStatistics() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
        loadStatistics(); // Reload UI
        Toast.makeText(this, "Statistics Reset", Toast.LENGTH_SHORT).show();
    }
}