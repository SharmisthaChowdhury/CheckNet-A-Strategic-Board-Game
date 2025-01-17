package com.example.bluetoothconnection;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class PlayActivity extends AppCompatActivity {
    public static PlayActivity activeInstance;
    private RadioButton[] buttons = new RadioButton[24];
    private boolean[] gameState = new boolean[24];
    private SendReceive sendReceive;
    private boolean isPlayerTurn = true;
    private CountDownTimer turnTimer;
    private TextView timerTextView;
    private Button replayButton;
    private Button quitButton; // Quit button
    private static final long TURN_DURATION = 20000; // 20 seconds
    private static final String QUIT_MESSAGE = "QUIT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        activeInstance = this;

        // Initialize UI components
        timerTextView = findViewById(R.id.timerTextView);
        replayButton = findViewById(R.id.replay);
        quitButton = findViewById(R.id.quit); // Initialize the Quit button

        sendReceive = BluetoothService.getInstance().getSendReceive();
        if (sendReceive == null) {
            Toast.makeText(this, "Bluetooth connection not established!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeButtons();
        replayButton.setOnClickListener(v -> sendReplayRequest());
        quitButton.setOnClickListener(v -> showQuitConfirmationDialog()); // Set Quit button click listener
        startTurnTimer(); // Start the timer for the first player's turn
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTurnTimer(); // Stop the timer if the activity is destroyed
        activeInstance = null;
    }

    private void initializeButtons() {
        for (int i = 0; i < 24; i++) {
            String buttonID = "radioButton" + (i + 1);
            int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
            buttons[i] = findViewById(resID);
            int finalI = i;
            buttons[i].setOnClickListener(v -> handleButtonClick(finalI));
        }
    }

    private void handleButtonClick(int index) {
        if (!isPlayerTurn) {
            Toast.makeText(this, "Wait for your opponent's move!", Toast.LENGTH_SHORT).show();
            return;
        }

        stopTurnTimer(); // Stop the timer since the player made a move
        updateButtonState(index, true);
        gameState[index] = true;
        sendGameState();
        isPlayerTurn = false; // Pass the turn to the opponent
        startTurnTimer(); // Start the opponent's timer
    }

    private void updateButtonState(int index, boolean isLocal) {
        buttons[index].setEnabled(false);
        if (isLocal) {
            buttons[index].setBackgroundColor(getResources().getColor(android.R.color.black)); // Black for local player
        } else {
            buttons[index].setBackgroundColor(getResources().getColor(android.R.color.white)); // Orange for opponent
        }
    }

    private void sendGameState() {
        try {
            sendReceive.write(serializeGameState());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to send game state", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] serializeGameState() {
        StringBuilder gameStateStr = new StringBuilder();
        for (boolean state : gameState) {
            gameStateStr.append(state ? "1" : "0");
        }
        return gameStateStr.toString().getBytes();
    }

    public void onOpponentMove(String receivedState) {
        runOnUiThread(() -> {
            if (receivedState.equals(QUIT_MESSAGE)) {
                // Handle opponent quitting the game
                Toast.makeText(this, "Your opponent has quit the game.", Toast.LENGTH_SHORT).show();
                finish(); // Close the game window
                return;
            } else if (receivedState.equals("REPLAY_REQUEST")) {
                showReplayConfirmationDialog();
                return;
            } else if (receivedState.equals("REPLAY_ACCEPT")) {
                Toast.makeText(this, "Opponent accepted the replay request. Starting a new game!", Toast.LENGTH_SHORT).show();
                resetGame();
                return;
            } else if (receivedState.equals("REPLAY_DECLINE")) {
                Toast.makeText(this, "Opponent declined the replay request.", Toast.LENGTH_SHORT).show();
                replayButton.setEnabled(true);
                return;
            }

            stopTurnTimer(); // Stop the opponent's timer

            for (int i = 0; i < receivedState.length(); i++) {
                boolean isPressed = receivedState.charAt(i) == '1';
                if (isPressed && !gameState[i]) {
                    updateButtonState(i, false);
                    gameState[i] = true;
                }
            }

            isPlayerTurn = true; // Pass the turn to the player
            startTurnTimer(); // Start the player's timer
        });
    }

    private void sendReplayRequest() {
        replayButton.setEnabled(false); // Disable replay button to prevent duplicate requests
        sendReceive.write("REPLAY_REQUEST".getBytes());
        Toast.makeText(this, "Replay request sent. Waiting for opponent's response.", Toast.LENGTH_SHORT).show();
    }

    private void sendReplayResponse(boolean accepted) {
        if (accepted) {
            sendReceive.write("REPLAY_ACCEPT".getBytes());
            resetGame();
        } else {
            sendReceive.write("REPLAY_DECLINE".getBytes());
        }
    }

    private void showReplayConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Replay Request")
                .setMessage("Your opponent wants to start a new game. Do you accept?")
                .setPositiveButton("Accept", (dialog, which) -> sendReplayResponse(true))
                .setNegativeButton("Decline", (dialog, which) -> sendReplayResponse(false))
                .setCancelable(false)
                .show();
    }

    private void resetGame() {
        for (int i = 0; i < gameState.length; i++) {
            gameState[i] = false; // Reset game state
        }

        for (RadioButton button : buttons) {
            button.setEnabled(true);
            button.setBackgroundResource(android.R.color.transparent); // Clear background
            button.setChecked(false); // Unselect the button
        }

        isPlayerTurn = true;
        stopTurnTimer();
        startTurnTimer();

        Toast.makeText(this, "Game reset. New game started!", Toast.LENGTH_SHORT).show();
        replayButton.setEnabled(true);
    }

    private void startTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel(); // Cancel any existing timer
        }

        if (!isPlayerTurn) {
            timerTextView.setText(""); // Clear timer display for the inactive player
            return;
        }

        turnTimer = new CountDownTimer(TURN_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerTextView.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                if (isPlayerTurn) {
                    timerTextView.setText("0"); // Display 0 when time's up
                    Toast.makeText(PlayActivity.this, "Time's up! Opponent's turn.", Toast.LENGTH_SHORT).show();
                    isPlayerTurn = false; // Pass turn to the opponent
                    sendGameState(); // Notify opponent about the timeout
                    startTurnTimer(); // Start the opponent's timer
                }
            }
        };
        turnTimer.start();
    }

    private void stopTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            timerTextView.setText(""); // Clear the timer display
        }
    }

    private void showQuitConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit Game");
        builder.setMessage("Do you want to quit the game?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            sendQuitMessage();
            Toast.makeText(this, "Exiting the game...", Toast.LENGTH_SHORT).show();
            finish();
        });
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void sendQuitMessage() {
        try {
            sendReceive.write(QUIT_MESSAGE.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to notify opponent", Toast.LENGTH_SHORT).show();
        }
    }
}