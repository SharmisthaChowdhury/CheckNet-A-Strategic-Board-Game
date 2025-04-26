package com.example.bluetoothconnection;


import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.animation.ValueAnimator;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class PlayActivity extends AppCompatActivity {
    public static PlayActivity activeInstance;
    private ImageView[] boardSlots = new ImageView[24];
    private String[] boardState = new String[24];
    private ImageView[] whitePawns = new ImageView[9];
    private ImageView[] blackPawns = new ImageView[9];
    private SendReceive sendReceive;
    private boolean isPlayerTurn = true;
    private CountDownTimer turnTimer;
    private TextView timerTextView;
    private Button replayButton;
    private Button quitButton;
    private static final long TURN_DURATION = 20000;
    private static final String QUIT_MESSAGE = "QUIT";
    private int placedPawns = 0;          // Local player's count
    private int opponentPlacedPawns = 0;  // Opponent's count
    private final int MAX_PAWNS = 9;
    private boolean movementPhase = false;
    private boolean isWhitePlayer = true; // set this according to your game logic
    private int selectedPawnIndex = -1;
    // New variables in PlayActivity:
    //private boolean brokeMillLastTurn = false;
    private int lastMovedIndex = -1;
    private int lastMillPawnIndex = -1;
    private int consecutiveMillCount = 0;
    private final int NETMATE_THRESHOLD = 4;
    private ImageView selectedPlacementPawn = null;



    // private String myColor = "white"; // or "black", depending on who starts first

    private final int[][] adjacentPositions = {
            {1, 17, 2},       // 0 - Position 1: Adjacent to 2,18,3
            {0, 15},          // 1 - Position 2: Adjacent to 1,16
            {0, 3, 13, 11},   // 2 - Position 3: Adjacent to 1,4,14,12
            {2, 10},          // 3 - Position 4: Adjacent to 3,11
            {11, 12, 15, 21}, // 4 - Position 5: Adjacent to 12,13,16,22
            {7, 13},          // 5 - Position 6: Adjacent to 8,14
            {13, 12},         // 6 - Position 7: Adjacent to 14,13
            {8, 10, 5},       // 7 - Position 8: Adjacent to 9,11,6
            {9, 7},           // 8 - Position 9: Adjacent to 10,8
            {14, 8, 20},      // 9 - Position 10: Adjacent to 15,9,21
            {19, 22, 7, 3},   //10 - Position 11: Adjacent to 20,23,8,4
            {2, 4},           //11 - Position 12: Adjacent to 3,5
            {6, 14, 4},       //12 - Position 13: Adjacent to 7,15,5
            {5, 2, 6},        //13 - Position 14: Adjacent to 6,3,7
            {12, 9},          //14 - Position 15: Adjacent to 13,10
            {23 ,1,4},        //15 - Position 16: Adjacent to 24,2,5
            {18, 20, 23},     //16 - Position 17: Adjacent to 19,21,24
            {0, 22},          //17 - Position 18: Adjacent to 1,23
            {16, 22},         //18 - Position 19: Adjacent to 17,23
            {20, 10},         //19 - Position 20: Adjacent to 21,11
            {19, 21, 9, 16},  //20 - Position 21: Adjacent to 20,22,10,17
            {20, 4},          //21 - Position 22: Adjacent to 21,5
            {18, 17, 10},      //22 - Position 23: Adjacent to 19,18,11
            {16, 15}          //23 - Position 24: Adjacent to 17,16
    };
    private final int[][] millCombinations = {
            {23, 16, 18},    // Top horizontal row
            {21, 20, 19},    // Second row
            {14, 9, 8},    // Third row
            {6, 13, 5},  // Fourth row
            {11, 2, 3}, // Fifth row
            {1, 0, 17}, // Sixth row

            {15,4,12}, //middle left
            {16,20,9},// top middle
            {7,10,22},//middle right
            {13,2,0},//middle below

            {23, 15, 1},   // Left vertical column
            {21, 4, 11},  // Second column
            {14, 12, 6},  // Third column
            {8, 7, 5},    // Middle vertical
            {19, 10, 3}, // Right inner vertical
            {18, 22, 17},  // Right mid

    };
    private boolean removeMode = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        activeInstance = this;

        timerTextView = findViewById(R.id.timerTextView);
        replayButton = findViewById(R.id.replay);
        quitButton = findViewById(R.id.quit);

        sendReceive = BluetoothService.getInstance().getSendReceive();
        if (sendReceive == null) {
            Toast.makeText(this, "Bluetooth connection not established!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            setupBoard();
            setupPawns();
        } catch (Exception e) {
            Toast.makeText(this, "Setup error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        replayButton.setOnClickListener(v -> sendReplayRequest());
        quitButton.setOnClickListener(v -> showQuitConfirmationDialog());
        startTurnTimer();
    }

    private void setupBoard() {
        for (int i = 0; i < 24; i++) {
            int resId = getResources().getIdentifier("position" + (i + 1), "id", getPackageName());
            boardSlots[i] = findViewById(resId);

            // Allow dropping pawns during placement phase
            boardSlots[i].setOnClickListener(new BoardSlotClickPlacementListener(i));


            // Allow clicking during movement phase
            //  boardSlots[i].setOnClickListener(new BoardSlotClickListener(i));

            boardState[i] = "empty";
        }
    }
    private void switchToMovementListeners() {
        for (int i = 0; i < 24; i++) {
            int resId = getResources().getIdentifier("position" + (i + 1), "id", getPackageName());
            boardSlots[i] = findViewById(resId);
            // Allow clicking during movement phase
            boardSlots[i].setOnClickListener(new BoardSlotClickListener(i));
        }
    }



    private void setupPawns() {
        for (int i = 0; i < 9; i++) {
            int whiteId = getResources().getIdentifier("whitePawn" + (i + 1), "id", getPackageName());
            int blackId = getResources().getIdentifier("blackPawn" + (i + 1), "id", getPackageName());

            whitePawns[i] = findViewById(whiteId);
            blackPawns[i] = findViewById(blackId);

            if (isWhitePlayer) {
                whitePawns[i].setOnClickListener(new PawnClickListener());

            } else {
                blackPawns[i].setOnClickListener(new PawnClickListener());

            }
        }
    }


    private class PawnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (!movementPhase && isPlayerTurn && placedPawns < MAX_PAWNS) {
                selectedPlacementPawn = (ImageView) view;
                Toast.makeText(PlayActivity.this, "Pawn selected. Now tap a position to place it.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    // Placement phase
    private class BoardSlotClickPlacementListener implements View.OnClickListener {
        private final int index;

        BoardSlotClickPlacementListener(int index) {
            this.index = index;
        }

        @Override
        public void onClick(View v) {
            // ✅ Step 1: Check removeMode first
            if (removeMode) {
                String opponentColor = getMyColor().equals("white") ? "black" : "white";
                if (boardState[index].equals(opponentColor)) {
                    boardSlots[index].setImageDrawable(null);
                    boardState[index] = "empty";
                    removeMode = false;
                    sendReceive.write(("REMOVE:" + index).getBytes());
                    Toast.makeText(PlayActivity.this, "Removed opponent's pawn!", Toast.LENGTH_SHORT).show();

                    isPlayerTurn = false;
                    stopTurnTimer();
                    startTurnTimer();
                } else {
                    Toast.makeText(PlayActivity.this, "Select a valid opponent's pawn to remove.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            // ✅ Step 2: Normal placement phase logic
            if (movementPhase || !isPlayerTurn || selectedPlacementPawn == null) return;

            if (!boardState[index].equals("empty") || placedPawns >= MAX_PAWNS) {
                Toast.makeText(PlayActivity.this, "Invalid spot!", Toast.LENGTH_SHORT).show();
                return;
            }

            boardSlots[index].setImageDrawable(selectedPlacementPawn.getDrawable());
            selectedPlacementPawn.setVisibility(View.INVISIBLE);
            boardState[index] = getMyColor();
            placedPawns++;

            if (isMillFormed(index, getMyColor())) {
                Toast.makeText(PlayActivity.this, "Mill formed! Remove an opponent pawn!", Toast.LENGTH_SHORT).show();
                removeMode = true;
            }

            sendReceive.write(("PLACE:" + index + ":" + getMyColor()).getBytes());

            if (placedPawns == MAX_PAWNS && opponentPlacedPawns == MAX_PAWNS) {
                movementPhase = true;
                Toast.makeText(PlayActivity.this, "Movement phase begins!", Toast.LENGTH_SHORT).show();
                switchToMovementListeners(); // ✅ Switch here
            }

            isPlayerTurn = false;
            selectedPlacementPawn = null;
            stopTurnTimer();
            startTurnTimer();
        }
    }

    // Movement phase
    private class BoardSlotClickListener implements View.OnClickListener {
        private final int index;

        BoardSlotClickListener(int index) {
            this.index = index;
        }

        @Override
        public void onClick(View v) {
            if (removeMode) {
                String opponentColor = getMyColor().equals("white") ? "black" : "white";
                if (boardState[index].equals(opponentColor)) {
                    boardSlots[index].setImageDrawable(null);
                    boardState[index] = "empty";
                    removeMode = false;
                    sendReceive.write(("REMOVE:" + index).getBytes());
                    Toast.makeText(PlayActivity.this, "Removed opponent's pawn!", Toast.LENGTH_SHORT).show();

                    // 🥇 Win condition #1: Opponent has fewer than 3 pawns
                    int remaining = 0;
                    for (String state : boardState) {
                        if (state.equals(opponentColor)) remaining++;
                    }

                    if (remaining < 3) {
                        showWinnerEffect("You Win! Opponent has fewer than 3 pawns.");
                        sendReceive.write("WINNER".getBytes()); // Already used in some cases

                        disableBoardInteractions();
                        return;
                    }

                    // 🥇 Win condition #2: Checknet (opponent can't move)
                    if (isCheckNet(opponentColor)) {
                        Toast.makeText(PlayActivity.this, "CHECKNET! Opponent cannot move. You win!", Toast.LENGTH_LONG).show();
                        showWinnerEffect("You Win! Opponent cannot move.");
                        sendReceive.write("WINNER".getBytes()); // Already used in some cases

                        disableBoardInteractions();
                        return;
                    }

                    isPlayerTurn = false;
                    stopTurnTimer();
                    startTurnTimer();
                } else {
                    Toast.makeText(PlayActivity.this, "Select a valid opponent's pawn to remove.", Toast.LENGTH_SHORT).show();
                }
                return;
            }


            // ⬇ Movement phase
            if (!movementPhase || !isPlayerTurn) return;
            String myColor = getMyColor();

            if (selectedPawnIndex == -1) {
                // Select your pawn
                if (boardState[index].equals(myColor)) {
                    selectedPawnIndex = index;
                    boardSlots[index].setAlpha(0.5f); // highlight
                    highlightAdjacents(index);

                    // Optional: show adjacents
                    StringBuilder message = new StringBuilder("Adjacent positions: ");
                    for (int pos : adjacentPositions[index]) {
                        if (boardState[pos].equals("empty")) {
                            message.append(pos + 1).append(" "); // +1 to match position1...position24
                        }
                    }
                    Toast.makeText(PlayActivity.this, message.toString(), Toast.LENGTH_SHORT).show();
                }
            } else {
                clearHighlights();

                boolean wasInMillBeforeMove = isMillFormed(selectedPawnIndex, myColor);

                // Try to move to an empty adjacent slot
                if (boardState[index].equals("empty") && isAdjacent(selectedPawnIndex, index)) {
                    // brokeMillLastTurn = wasInMillBeforeMove;
                    lastMovedIndex = selectedPawnIndex;


                    // Move visually and update state
                    boardSlots[index].setImageDrawable(boardSlots[selectedPawnIndex].getDrawable());
                    boardSlots[selectedPawnIndex].setImageDrawable(null);
                    boardState[index] = myColor;
                    boardState[selectedPawnIndex] = "empty";
                    boardSlots[selectedPawnIndex].setAlpha(1f);

                    // ✅ Mill check after move
                    if (isMillFormed(index, myColor)) {
                        Toast.makeText(PlayActivity.this, "Mill formed! Remove an opponent pawn!", Toast.LENGTH_SHORT).show();
                        removeMode = true;

                        if (lastMillPawnIndex == lastMovedIndex) {
                            consecutiveMillCount++;
                        } else {
                            lastMillPawnIndex = lastMovedIndex;
                            consecutiveMillCount = 1;
                        }


                        if (consecutiveMillCount >= NETMATE_THRESHOLD) {
                            Toast.makeText(PlayActivity.this, "NETMATE! You've formed 4 mills with the same pawn!", Toast.LENGTH_LONG).show();
                            sendReceive.write("WINNER".getBytes());
                            boardSlots[index].setBackgroundResource(R.drawable.glow_background);
                            animateGlowEffect(boardSlots[index]);
                            showWinnerEffect("NETMATE! You Win!");
                            sendReceive.write("WINNER".getBytes()); // Already used in some cases

                            disableBoardInteractions();
                        }
                    }else {
                        consecutiveMillCount = 0;
                        lastMillPawnIndex = -1;
                    }


                    // 🔄 Send move info
                    sendReceive.write(("MOVE:" + selectedPawnIndex + ":" + index + ":" + myColor).getBytes());

                    selectedPawnIndex = -1;
                    isPlayerTurn = false;
                    stopTurnTimer();
                    startTurnTimer();
                } else {
                    // Invalid move
                    if (!boardState[index].equals("empty")) {
                        Toast.makeText(PlayActivity.this, "That spot is already occupied.", Toast.LENGTH_SHORT).show();
                    } else if (!isAdjacent(selectedPawnIndex, index)) {
                        Toast.makeText(PlayActivity.this, "You can only move to adjacent positions.", Toast.LENGTH_SHORT).show();
                    }

                    // Deselect
                    boardSlots[selectedPawnIndex].setAlpha(1f);
                    selectedPawnIndex = -1;
                }
            }
        }
    }


    private boolean isCheckNet(String playerColor) {
        for (int i = 0; i < boardSlots.length; i++) {
            if (boardState[i].equals(playerColor)) {
                for (int adj : adjacentPositions[i]) {
                    if (boardState[adj].equals("empty")) {
                        return false; // At least one move is possible
                    }
                }
            }
        }
        return true; // No possible moves
    }




    public void onOpponentMove(String msg) {
        runOnUiThread(() -> {
            if (msg.equals(QUIT_MESSAGE)) {
                Toast.makeText(this, "Your opponent has quit the game.", Toast.LENGTH_SHORT).show();
                finish();
            } else if (msg.equals("REPLAY_REQUEST")) {
                showReplayConfirmationDialog();
            } else if (msg.equals("REPLAY_ACCEPT")) {
                resetGame();
                Toast.makeText(this, "Opponent accepted the replay request.", Toast.LENGTH_SHORT).show();
            } else if (msg.equals("REPLAY_DECLINE")) {
                Toast.makeText(this, "Opponent declined the replay request.", Toast.LENGTH_SHORT).show();
                replayButton.setEnabled(true);
            } else if (msg.startsWith("PLACE:")) {
                String[] parts = msg.split(":");
                int index = Integer.parseInt(parts[1]);
                String color = parts[2];

                String opponentColor = color.equals("white") ? "black" : "white";

                boardSlots[index].setImageResource(
                        opponentColor.equals("white") ? R.drawable.white_pawn : R.drawable.black_pawn
                );
                boardState[index] = opponentColor;

                // ✅ With this updated version:
                if (opponentColor.equals("white") && opponentPlacedPawns < whitePawns.length) {
                    if (whitePawns[opponentPlacedPawns] != null) {
                        whitePawns[opponentPlacedPawns].setVisibility(View.INVISIBLE);
                    }
                    opponentPlacedPawns++;
                } else if (opponentColor.equals("black") && opponentPlacedPawns < blackPawns.length) {
                    if (blackPawns[opponentPlacedPawns] != null) {
                        blackPawns[opponentPlacedPawns].setVisibility(View.INVISIBLE);
                    }
                    opponentPlacedPawns++;
                }

                // Keep the rest:
                if (placedPawns == MAX_PAWNS && opponentPlacedPawns == MAX_PAWNS) {
                    movementPhase = true;
                    Toast.makeText(this, "Movement phase begins!", Toast.LENGTH_SHORT).show();
                    switchToMovementListeners(); // ✅ This must be here too!
                }

                isPlayerTurn = true;
                stopTurnTimer();
                startTurnTimer();
            } else if (msg.startsWith("MOVE:")) {
                String[] parts = msg.split(":");
                int from = Integer.parseInt(parts[1]);
                int to = Integer.parseInt(parts[2]);
                String color = parts[3];

                boardSlots[to].setImageResource(color.equals("white") ? R.drawable.black_pawn : R.drawable.white_pawn); // inverse
                boardSlots[from].setImageDrawable(null);
                boardState[from] = "empty";
                boardState[to] = color.equals("white") ? "black" : "white";

                //  Checknet check after opponent's move
                String myColor = getMyColor();
                if (isCheckNet(myColor)) {
                    Toast.makeText(this, "CHECKNET! You cannot move. Game over.", Toast.LENGTH_LONG).show();
                    // TODO: Handle game end (disable board, show dialog, etc.)

                    return;
                }

                isPlayerTurn = true;
                stopTurnTimer();
                startTurnTimer();
            }else if (msg.startsWith("REMOVE:")) {
                int index = Integer.parseInt(msg.split(":")[1]);
                boardSlots[index].setImageDrawable(null);
                boardState[index] = "empty";
                Toast.makeText(this, "Your pawn was removed!", Toast.LENGTH_SHORT).show();
                isPlayerTurn = true;
                stopTurnTimer();
                startTurnTimer();
            }else if (msg.equals("TIMEOUT")) {
                Toast.makeText(this, "Opponent missed their turn. Your turn!", Toast.LENGTH_SHORT).show();
                isPlayerTurn = true;
                stopTurnTimer();
                startTurnTimer();
            }else if (msg.equals("WINNER")) {
                showLoserEffect("You Lose !"); // ← show the losing dialog to the defeated player
            }




        });
    }


    private void sendReplayRequest() {
        replayButton.setEnabled(false);
        sendReceive.write("REPLAY_REQUEST".getBytes());
        Toast.makeText(this, "Replay request sent.", Toast.LENGTH_SHORT).show();
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
        for (int i = 0; i < boardState.length; i++) {
            boardState[i] = "empty";
            boardSlots[i].setImageDrawable(null);
        }

        for (ImageView pawn : isWhitePlayer ? whitePawns : blackPawns) {
            if (pawn != null) pawn.setVisibility(View.VISIBLE);
        }

        placedPawns = 0;
        isPlayerTurn = true;
        stopTurnTimer();
        startTurnTimer();
        replayButton.setEnabled(true);
    }
    private void highlightAdjacents(int from) {
        for (int pos : adjacentPositions[from]) {
            if (boardState[pos].equals("empty")) {
                boardSlots[pos].setAlpha(0.5f); // visually mark
            }
        }
    }

    private void clearHighlights() {
        for (ImageView slot : boardSlots) {
            slot.setAlpha(1f); // restore default
        }
    }
    // ✅ Add this below resetGame()
    private boolean isAdjacent(int from, int to) {
        for (int adj : adjacentPositions[from]) {
            if (adj == to) return true;
        }
        return false;
    }
    private String getMyColor() {
        return isWhitePlayer ? "white" : "black";
    }
    private boolean isMillFormed(int index, String color) {
        for (int[] mill : millCombinations) {
            if (mill[0] == index || mill[1] == index || mill[2] == index) {
                if (boardState[mill[0]].equals(color)
                        && boardState[mill[1]].equals(color)
                        && boardState[mill[2]].equals(color)) {
                    return true;
                }
            }
        }
        return false;
    }
    private void animateGlowEffect(View targetView) {
        ValueAnimator animator = ValueAnimator.ofFloat(1f, 1.5f, 1f); // Scale up then back
        animator.setDuration(1000);
        animator.setRepeatCount(2); // Repeat a few times
        animator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            targetView.setScaleX(scale);
            targetView.setScaleY(scale);
        });
        animator.start();

        // Optional: background glow color
        targetView.setBackgroundResource(R.drawable.glow_background);

        // Remove background after animation
        new Handler().postDelayed(() -> {
            targetView.setBackground(null);
        }, 3000);
    }
    private void showWinnerEffect(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.WinnerDialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_winner, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        TextView winnerText = dialogView.findViewById(R.id.winnerText);
        winnerText.setText(message);

        ImageView trophyImage = dialogView.findViewById(R.id.trophyImage);
        animateGlowEffect(trophyImage); // reuse your glowing method

        Button okButton = dialogView.findViewById(R.id.okButton);
        okButton.setOnClickListener(v -> {
            dialog.dismiss();
            disableBoardInteractions(); // ✅ Prevent further interaction
            finish(); // end the game
        });

        dialog.show();

        // Optional: Play victory sound
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.victory_sound);
        mediaPlayer.start();
    }
    private void showLoserEffect(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.LoserDialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_loser, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        TextView loserText = dialogView.findViewById(R.id.loserText);
        loserText.setText(message);

        ImageView sadFaceImage = dialogView.findViewById(R.id.sadFace);
        sadFaceImage.setImageResource(R.drawable.sad_face); // add this drawable in /res/drawable
        Button okButton = dialogView.findViewById(R.id.okButton);
        okButton.setOnClickListener(v -> {
            dialog.dismiss();
            disableBoardInteractions(); // ✅ Prevent further interaction
            finish(); // end the game
        });

        dialog.show();
    }


    private void disableBoardInteractions() {
        // Disable all board slot click and drag listeners
        for (ImageView slot : boardSlots) {
            if (slot != null) {
                slot.setOnClickListener(null);
                slot.setOnDragListener(null);
            }
        }

        // Disable all white pawn touch events
        for (ImageView pawn : whitePawns) {
            if (pawn != null) {
                pawn.setOnTouchListener(null);
            }
        }

        // Disable all black pawn touch events
        for (ImageView pawn : blackPawns) {
            if (pawn != null) {
                pawn.setOnTouchListener(null);
            }
        }

        // Disable any remaining turn logic
        isPlayerTurn = false;
        removeMode = false;
        selectedPawnIndex = -1;
    }



    private boolean contains(int[] arr, int val) {
        for (int i : arr) if (i == val) return true;
        return false;
    }



    private void startTurnTimer() {
        stopTurnTimer();

        if (!isPlayerTurn) {
            timerTextView.setText("");
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
                    Toast.makeText(PlayActivity.this, "Time's up! Opponent's turn.", Toast.LENGTH_SHORT).show();
                    isPlayerTurn = false;
                    stopTurnTimer(); // clear local timer
                    sendReceive.write("TIMEOUT".getBytes()); // notify opponent
                    startTurnTimer(); // give the turn to the other player
                }
            }
        }.start();

    }

    private void stopTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            timerTextView.setText("");
        }
    }

    private void showQuitConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Quit Game")
                .setMessage("Do you want to quit the game?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    sendQuitMessage();
                    Toast.makeText(this, "Exiting the game...", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void sendQuitMessage() {
        try {
            sendReceive.write(QUIT_MESSAGE.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to notify opponent", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTurnTimer();
        activeInstance = null;
    }
}