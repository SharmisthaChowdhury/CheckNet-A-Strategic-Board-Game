package com.example.bluetoothconnection;


import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.animation.ValueAnimator;
import android.graphics.Color;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

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

    private int lastMillPawnIndex = -1;
    private ImageView selectedPlacementPawn = null;

    private int netmateReformCount = 0;
    private final int NETMATE_REQUIRED_COUNT = 4;
    private boolean millWasBroken = false;
    private Map<String, Integer> scoreBoard = new HashMap<>();

    private TextView chatMessages;      // To display chat history
    private EditText chatInput; // For typing messages
    private ImageButton sendButton;// Send button



    private int timeoutCount = 0;
    private boolean warningPlayed = false;
    private MediaPlayer warningPlayer;
    private static final int MAX_TIMEOUTS = 3;


    // private String myColor = "white"; // or "black", depending on who starts first

    private final int[][] adjacentPositions = {
            {17 , 2},       // 0 - Position 1: Adjacent to 2,18,3
            {14 , 17},          // 1 - Position 2: Adjacent to 1,16
            {10 , 0 , 18},   // 2 - Position 3: Adjacent to 1,4,14,12
            {17 , 11 , 15 , 13},          // 3 - Position 4: Adjacent to 3,11
            {21 , 15 , 14 , 12}, // 4 - Position 5: Adjacent to 15,13,16,22
            {13 , 7},          // 5 - Position 6: Adjacent to 8,14
            {13  , 12},         // 6 - Position 7: Adjacent to 14,13
            {5 , 8 , 10},       // 7 - Position 8: Adjacent to 9,11,6
            {22 , 7},           // 8 - Position 9: Adjacent to 10,8
            {12 , 22},      // 9 - Position 10: Adjacent to 15,9,21
            {7 , 19 , 11 , 2},   //10 - Position 11: Adjacent to 20,23,8,4
            {3 , 10},           //11 - Position 12: Adjacent to 3,5
            {6 , 9 , 4},       //12 - Position 13: Adjacent to 7,15,5
            {6 , 5 , 3},        //13 - Position 14: Adjacent to 6,3,7
            {1 , 4 , 23},          //14 - Position 15: Adjacent to 13,10
            {3 , 4},        //15 - Position 16: Adjacent to 24,2,5
            {20 , 23 , 18},     //16 - Position 17: Adjacent to 19,21,24
            {1 , 0 , 3},          //17 - Position 18: Adjacent to 1,23
            {16 , 2},         //18 - Position 19: Adjacent to 17,23
            {20 , 10},         //19 - Position 20: Adjacent to 21,11
            {21 , 16 , 19 , 22},  //20 - Position 21: Adjacent to 20,22,10,17
            {4 , 20},          //21 - Position 22: Adjacent to 21,5
            {20 , 9 , 8},      //22 - Position 23: Adjacent to 19,18,11
            {14 , 16}          //23 - Position 24: Adjacent to 17,16
    };
    private final int[][] millCombinations = {
            {23, 16, 18},    // Top horizontal row
            {23 , 14 , 1},
            {1 , 17 , 0},
            {0,2,18},

            {21, 20 , 19} , // inner outer second square
            {21 , 4 , 15} ,
            {15 , 3 , 11} ,
            {11 , 10 , 19} ,

            {9 , 22 , 8 } ,
            {9 , 12 , 6} ,
            {6 , 13 , 5} ,
            {5 , 7 , 8} ,

            {16 , 20 , 22} ,
            {7 , 10 , 2} ,
            {13 , 3 , 17},
            {14 , 4 , 12}

    };

    private boolean removeMode = false;
    private boolean isFirstPlayer;
    private int opponentTimeoutCount =0;

    private SoundPool soundPool;
    private int ticticSoundId;
    private boolean soundLoaded = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        isFirstPlayer = getIntent().getBooleanExtra("isFirstPlayer", false);
        ImageButton btnReactions = findViewById(R.id.btnReactions);
        btnReactions.setOnClickListener(v -> showEmojiPopup(v));

        if (isFirstPlayer) {
            isPlayerTurn = true;
            startTurnTimer(); // ⏱ Timer starts only for first player
        }

        activeInstance = this;
        // Initialize scores here
        scoreBoard.put("You", 0);
        scoreBoard.put("Opponent", 0);

        timerTextView = findViewById(R.id.timerTextView);
        replayButton = findViewById(R.id.replay);
        quitButton = findViewById(R.id.quit);

        boolean isFirstPlayer = getIntent().getBooleanExtra("isFirstPlayer", false);
        isPlayerTurn = isFirstPlayer;

        if (isFirstPlayer) {
            startTurnTimer();
        }

        sendReceive = BluetoothService.getInstance().getSendReceive();
        if (sendReceive == null) {
            Toast.makeText(this, "Bluetooth connection not established!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 🔽 Chat UI initialization
        chatMessages = findViewById(R.id.chatMessages);
        chatInput = findViewById(R.id.chatInput);
        sendButton = findViewById(R.id.sendButton);
        TextView messageView = findViewById(R.id.chatMessages);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        sendButton.setOnClickListener(v -> {
            String message = chatInput.getText().toString().trim();

            if (!message.isEmpty()) {
                // Apply animation
                messageView.startAnimation(slideIn);

                // Append message to chat
                chatMessages.append("Me: " + message + "\n");

                // Send via Bluetooth (or local logic)
                sendChatMessage(message);

                // Vibrate briefly (adds tactile feel)
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }

                // Clear chat input
                chatInput.setText("");

                // Scroll to latest message
                final int scrollAmount = chatMessages.getLayout().getLineTop(chatMessages.getLineCount()) - chatMessages.getHeight();
                if (scrollAmount > 0)
                    chatMessages.scrollTo(0, scrollAmount);
                else
                    chatMessages.scrollTo(0, 0);
            }
        });

        soundPool = new SoundPool.Builder().setMaxStreams(1).build();
        ticticSoundId = soundPool.load(this, R.raw.tictic, 1);
        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status == 0) soundLoaded = true;
        });


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
                    if (isMillFormed(index, opponentColor)) {
                        // Check if there is any opponent pawn NOT in a mill
                        boolean hasRemovablePawn = false;
                        for (int i = 0; i < boardState.length; i++) {
                            if (boardState[i].equals(opponentColor) && !isMillFormed(i, opponentColor)) {
                                hasRemovablePawn = true;
                                break;
                            }
                        }

                        if (hasRemovablePawn) {
                            Toast.makeText(PlayActivity.this, "Cannot remove a pawn in a mill unless no other options.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

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

                playMillSound(); // 🔊
                animateMill(index, getMyColor()); // ✨
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

                    // Prevent removing from a mill unless no other option
                    if (isMillFormed(index, opponentColor)) {
                        boolean hasRemovablePawn = false;
                        for (int i = 0; i < boardState.length; i++) {
                            if (boardState[i].equals(opponentColor) && !isMillFormed(i, opponentColor)) {
                                hasRemovablePawn = true;
                                break;
                            }
                        }

                        if (hasRemovablePawn) {
                            Toast.makeText(PlayActivity.this, "You can't remove a pawn in a mill unless no other options.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    // Proceed to remove the pawn
                    boardSlots[index].setImageDrawable(null);
                    boardState[index] = "empty";
                    removeMode = false;
                    sendReceive.write(("REMOVE:" + index).getBytes());
                    Toast.makeText(PlayActivity.this, "Removed opponent's pawn!", Toast.LENGTH_SHORT).show();

                    // Win condition 1: Fewer than 3 pawns
                    int remaining = 0;
                    for (String state : boardState) {
                        if (state.equals(opponentColor)) remaining++;
                    }
                    if (remaining < 3) {
                        updateMatchStatistics(true);
                        sendReceive.write("WINNER".getBytes());
                        disableBoardInteractions();
                        scoreBoard.put("You", scoreBoard.get("You") + 1);
                        showGameEndDialog("You Win! Opponent has less than 3 pawns.",
                                true, "You", "Opponent", 1, 0, "⚪ ⚪ ⚪");
                        return;
                    }

                    // Win condition 2: Checknet
                    if (isCheckNet(opponentColor)) {
                        Toast.makeText(PlayActivity.this, "CHECKNET! Opponent cannot move. You win!", Toast.LENGTH_LONG).show();
                        updateMatchStatistics(true);
                        showGameEndDialog("You Win! Opponent cannot move.",
                                true, "You", "Opponent", 1, 0, "⚪ ⚪ ⚪");
                        sendReceive.write("WINNER".getBytes());
                        disableBoardInteractions();
                        scoreBoard.put("You", scoreBoard.get("You") + 1);
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


            // Movement phase
            if (!movementPhase || !isPlayerTurn) return;
            String myColor = getMyColor();

            if (selectedPawnIndex == -1) {
                // Select your pawn
                if (boardState[index].equals(myColor)) {
                    selectedPawnIndex = index;
                    boardSlots[index].setAlpha(0.5f);
                    highlightAdjacents(index);

                    StringBuilder message = new StringBuilder("Adjacent positions: ");
                    for (int pos : adjacentPositions[index]) {
                        if (boardState[pos].equals("empty")) {
                            message.append(pos + 1).append(" ");
                        }
                    }
                    Toast.makeText(PlayActivity.this, message.toString(), Toast.LENGTH_SHORT).show();
                }
            } else {
                clearHighlights();

                // Store mill state before move
                boolean wasInMillBeforeMove = isMillFormed(selectedPawnIndex, myColor);

                // Try to move
                if (boardState[index].equals("empty") && isAdjacent(selectedPawnIndex, index)) {

                    if (wasInMillBeforeMove) {
                        millWasBroken = true;
                        lastMillPawnIndex = selectedPawnIndex;
                    }

                    boardSlots[index].setImageDrawable(boardSlots[selectedPawnIndex].getDrawable());
                    boardSlots[selectedPawnIndex].setImageDrawable(null);
                    boardState[index] = myColor;
                    boardState[selectedPawnIndex] = "empty";
                    boardSlots[selectedPawnIndex].setAlpha(1f);
                    if (isMillFormed(index, myColor)) {
                        Toast.makeText(PlayActivity.this, "Mill formed! Remove an opponent pawn!", Toast.LENGTH_SHORT).show();
                        removeMode = true;

                        playMillSound(); // 🔊
                        animateMill(index, myColor); // ✨


                        // Netmate: reforming mill with same pawn
                        if (millWasBroken && lastMillPawnIndex == index) {
                            netmateReformCount++;
                            if (netmateReformCount >= NETMATE_REQUIRED_COUNT) {
                                Toast.makeText(PlayActivity.this, "NETMATE! You've formed 4 mills with the same pawn!", Toast.LENGTH_LONG).show();
                                boardSlots[index].setBackgroundResource(R.drawable.glow_background);
                                animateGlowEffect(boardSlots[index]);
                                updateMatchStatistics(true);  // ✅ Player won
                                showGameEndDialog("NETMATE! You Win!",true, "You",
                                        "Opponent",   // player 2 name
                                        1,                    // player 1 score
                                        0,                    // player 2 score
                                        "⚪ ⚪ ⚪" );
                                sendReceive.write("WINNER".getBytes());
                                scoreBoard.put("You", scoreBoard.get("You") + 1);
                                showGameEndDialog(
                                        "You Win!",           // or "You Lose!"
                                        true,                 // or false
                                        "You",           // player 1 name
                                        "Opponent",           // player 2 name
                                        1,                    // player 1 score
                                        0,                    // player 2 score
                                        "⚪ ⚪ ⚪"               // optional pawn count
                                );
                                disableBoardInteractions();
                            }
                        } else {
                            netmateReformCount = 0;
                        }

                        millWasBroken = false;
                    } else {
                        netmateReformCount = 0;
                        lastMillPawnIndex = -1;
                    }

                    sendReceive.write(("MOVE:" + selectedPawnIndex + ":" + index + ":" + myColor).getBytes());

                    selectedPawnIndex = -1;
                    isPlayerTurn = false;
                    stopTurnTimer();
                    startTurnTimer();
                } else {
                    if (!boardState[index].equals("empty")) {
                        Toast.makeText(PlayActivity.this, "That spot is already occupied.", Toast.LENGTH_SHORT).show();
                    } else if (!isAdjacent(selectedPawnIndex, index)) {
                        Toast.makeText(PlayActivity.this, "You can only move to adjacent positions.", Toast.LENGTH_SHORT).show();
                    }

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
                return;
            }

            switch (msg) {
                case "REPLAY_REQUEST":
                    showReplayConfirmationDialog();
                    return;

                case "REPLAY_ACCEPT":
                    resetGame();
                    Toast.makeText(this, "Opponent accepted the replay request.", Toast.LENGTH_SHORT).show();
                    return;

                case "REPLAY_DECLINE":
                    Toast.makeText(this, "Opponent declined the replay request.", Toast.LENGTH_SHORT).show();
                    replayButton.setEnabled(true);
                    return;
            }

            if (msg.startsWith("PLACE:")) {
                String[] parts = msg.split(":");
                int index = Integer.parseInt(parts[1]);
                String color = parts[2];

                String opponentColor = color.equals("white") ? "black" : "white";

                boardSlots[index].setImageResource(
                        opponentColor.equals("white") ? R.drawable.white_pawn : R.drawable.black_pawn
                );
                boardState[index] = opponentColor;

                if (opponentColor.equals("white") && opponentPlacedPawns < whitePawns.length) {
                    if (whitePawns[opponentPlacedPawns] != null)
                        whitePawns[opponentPlacedPawns].setVisibility(View.INVISIBLE);
                    opponentPlacedPawns++;
                } else if (opponentColor.equals("black") && opponentPlacedPawns < blackPawns.length) {
                    if (blackPawns[opponentPlacedPawns] != null)
                        blackPawns[opponentPlacedPawns].setVisibility(View.INVISIBLE);
                    opponentPlacedPawns++;
                }

                if (placedPawns == MAX_PAWNS && opponentPlacedPawns == MAX_PAWNS) {
                    movementPhase = true;
                    Toast.makeText(this, "Movement phase begins!", Toast.LENGTH_SHORT).show();
                    switchToMovementListeners();
                }

                isPlayerTurn = true;
                stopTurnTimer();
                startTurnTimer();

            } else if (msg.startsWith("MOVE:")) {
                String[] parts = msg.split(":");
                int from = Integer.parseInt(parts[1]);
                int to = Integer.parseInt(parts[2]);
                String color = parts[3];

                boardSlots[to].setImageResource(
                        color.equals("white") ? R.drawable.black_pawn : R.drawable.white_pawn
                ); // inverted
                boardSlots[from].setImageDrawable(null);
                boardState[from] = "empty";
                boardState[to] = color.equals("white") ? "black" : "white";

                String myColor = getMyColor();
                if (isCheckNet(myColor)) {
                    Toast.makeText(this, "CHECKNET! You cannot move. Game over.", Toast.LENGTH_LONG).show();
                    // TODO: handle loss
                    return;
                }

                isPlayerTurn = true;
                stopTurnTimer();
                startTurnTimer();

            } else if (msg.startsWith("REMOVE:")) {
                int index = Integer.parseInt(msg.split(":")[1]);
                boardSlots[index].setImageDrawable(null);
                boardState[index] = "empty";

                Toast.makeText(this, "Your pawn was removed!", Toast.LENGTH_SHORT).show();
                isPlayerTurn = true;
                stopTurnTimer();
                startTurnTimer();

            } else if (msg.startsWith("TIMEOUT:")) {
                opponentTimeoutCount = Integer.parseInt(msg.split(":")[1]);

                if (opponentTimeoutCount >= MAX_TIMEOUTS) {
                    stopTurnTimer();
                    disableBoardInteractions();

                    // You win
                    scoreBoard.put("You", scoreBoard.get("You") + 1);

                    showGameEndDialog("Opponent lost by timeout!", true, "You", "Opponent", 1, 0, "⚪ ⚪ ⚪");
                    updateMatchStatistics(true); // You win
                } else {
                    Toast.makeText(this, "Opponent missed their turn. Your turn!", Toast.LENGTH_SHORT).show();
                    isPlayerTurn = true;
                    stopTurnTimer();
                    startTurnTimer();
                }
            }
            else if (msg.equals("WINNER")) {
                stopTurnTimer();
                updateMatchStatistics(false); // You lose
                disableBoardInteractions();
                showGameEndDialog("You lost the game!", false,
                        "Opponent", "You", 0, 1, "⚫ ⚫ ⚫"); // Customize as needed
            }else if (msg.startsWith("EMOJI:")) {
                String type = msg.split(":")[1];
                runOnUiThread(() -> showEmoji(type));
            } else {
                // Fallback: treat message as chat
                chatMessages.append("Opponent: " + msg + "\n");
                int scrollAmount = chatMessages.getLayout().getLineTop(chatMessages.getLineCount()) - chatMessages.getHeight();
                chatMessages.scrollTo(0, Math.max(scrollAmount, 0));
            }
        });
    }


    private void scrollChatToBottom() {
        final int scrollAmount = chatMessages.getLayout().getLineTop(chatMessages.getLineCount()) - chatMessages.getHeight();
        if (scrollAmount > 0)
            chatMessages.scrollTo(0, scrollAmount);
        else
            chatMessages.scrollTo(0, 0);
    }
    private void sendChatMessage(String message) {
        sendReceive.write(message.getBytes());
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

    private void showGameEndDialog(String resultMessage, boolean isWinner,
                                   String player1NameStr, String player2NameStr,
                                   int player1Score, int player2Score, String pawnCountText) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                isWinner ? R.style.WinnerDialogTheme : R.style.LoserDialogTheme);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_scoreboard, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        // Result image
        ImageView resultImage = dialogView.findViewById(R.id.resultImage);
        resultImage.setImageResource(isWinner ? R.drawable.trophy1 : R.drawable.sad_face);
        applyBounceAnimation(resultImage);

        // Result text
        TextView resultText = dialogView.findViewById(R.id.resultText);
        resultText.setText(resultMessage);
        applyFadeInAnimation(resultText);

        // Player Name & Score (assign "You" and "Opponent" based on isWinner)
        TextView player1Name = dialogView.findViewById(R.id.player1Name);
        TextView player1ScoreText = dialogView.findViewById(R.id.player1Score);
        TextView player2Name = dialogView.findViewById(R.id.player2Name);
        TextView player2ScoreText = dialogView.findViewById(R.id.player2Score);

        if (isWinner) {
            player1Name.setText("You");
            player1ScoreText.setText(String.valueOf(player1Score));

            player2Name.setText("Opponent");
            player2ScoreText.setText(String.valueOf(player2Score));
        } else {
            player1Name.setText("Opponent");
            player1ScoreText.setText(String.valueOf(player2Score)); // opponent score is 1

            player2Name.setText("You");
            player2ScoreText.setText(String.valueOf(player1Score)); // your score is 0
        }

        applySlideInFromLeftAnimation(player1Name);
        applySlideInFromLeftAnimation(player1ScoreText);
        applySlideInFromRightAnimation(player2Name);
        applySlideInFromRightAnimation(player2ScoreText);

        // Pawn Count
        TextView pawnCount = dialogView.findViewById(R.id.pawnCountText);
        pawnCount.setText(pawnCountText);
        applyFadeInAnimation(pawnCount);

        // OK Button
        Button okButton = dialogView.findViewById(R.id.okButton);
        applyPopInAnimation(okButton);
        okButton.setOnClickListener(v -> {
            dialog.dismiss();
            disableBoardInteractions();
            finish(); // End activity
        });

        // Share Button
        Button shareButton = dialogView.findViewById(R.id.shareButton);
        String shareMessage;
        if (isWinner) {
            shareMessage = "🎯 Game Result:\nYou: " + player1Score +
                    "\nOpponent: " + player2Score + "\nResult: " + resultMessage;
        } else {
            shareMessage = "🎯 Game Result:\nOpponent: " + player2Score +
                    "\nYou: " + player1Score + "\nResult: " + resultMessage;
        }
        shareButton.setOnClickListener(v -> shareGameResult(shareMessage));

        // View History Button
        Button viewHistoryButton = dialogView.findViewById(R.id.viewHistoryButton);
        viewHistoryButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(PlayActivity.this, MatchHistoryActivity.class);
            startActivity(intent);
        });

        dialog.show();

        // Play sound
        MediaPlayer mediaPlayer = MediaPlayer.create(this,
                isWinner ? R.raw.victory_sound : R.raw.defeat_sound);
        mediaPlayer.start();
    }

    private void shareGameResult(String message) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Nine Men's Morris Game Result");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message + "\nPlayed on: MyGame 🎮");

        try {
            startActivity(Intent.createChooser(shareIntent, "Share your game result via"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app found to share result!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMatchStatistics(boolean isWinner) {
        SharedPreferences preferences = getSharedPreferences("MatchStats", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        int gamesPlayed = preferences.getInt("gamesPlayed", 0) + 1;
        int gamesWon = preferences.getInt("gamesWon", 0);
        int gamesLost = preferences.getInt("gamesLost", 0);
        int winStreak = preferences.getInt("winStreak", 0);

        if (isWinner) {
            gamesWon++;
            winStreak++;
        } else {
            gamesLost++;
            winStreak = 0;
        }

        editor.putInt("gamesPlayed", gamesPlayed);
        editor.putInt("gamesWon", gamesWon);
        editor.putInt("gamesLost", gamesLost);
        editor.putInt("winStreak", winStreak);
        editor.apply();
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
        stopTurnTimer(); // Ensure old timer is stopped
        warningPlayed = false; // Reset warning flag

        if (!isPlayerTurn) {
            timerTextView.setText("");
            return;
        }

        turnTimer = new CountDownTimer(TURN_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = millisUntilFinished / 1000;
                timerTextView.setText(String.valueOf(secondsLeft));

                // ⏱ Tictic sound every second
                if (soundLoaded) {
                    soundPool.play(ticticSoundId, 1f, 1f, 1, 0, 1f);
                }


                // Play warning sound and animation at last 5 seconds
                if (secondsLeft <= 5 && !warningPlayed) {
                    warningPlayed = true;
                    playWarningEffect();
                }
            }

            @Override
            public void onFinish() {
                if (isPlayerTurn) {
                    stopTurnTimer();
                    isPlayerTurn = false;
                    timeoutCount++; // Local player timeout

                    // Notify opponent
                    sendReceive.write(("TIMEOUT:" + timeoutCount).getBytes());

                    // ⛔ Check if local player should forfeit
                    if (timeoutCount >= MAX_TIMEOUTS) {
                        stopTurnTimer();
                        disableBoardInteractions();

                        // You lose, so opponent gets 1 point
                        scoreBoard.put("Opponent", scoreBoard.get("Opponent") + 1);

                        showGameEndDialog("You lost by timeout!", false, "You", "Opponent", 0, 1, "⚫ ⚫ ⚫");
                        updateMatchStatistics(false); // You lose
                        return;
                    }

                    // If not yet max timeouts, just pass turn
                    Toast.makeText(PlayActivity.this, "⏱ Time's up! Opponent's turn.", Toast.LENGTH_SHORT).show();
                    startTurnTimer(); // Starts the opponent's timer
                }
            }

        }.start();
    }

    private void stopTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            timerTextView.setText("");
            turnTimer = null;
        }
    }

    // Optional: Play sound and animate timer
    private void playWarningEffect() {
        warningPlayer = MediaPlayer.create(PlayActivity.this, R.raw.timer_warning);
        warningPlayer.start();

        timerTextView.setTextColor(Color.RED);
        Animation shake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake);
        timerTextView.startAnimation(shake);
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
    private void applyBounceAnimation(View view) {
        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
        view.startAnimation(bounce);
    }

    private void applyFadeInAnimation(View view) {
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(600);
        view.startAnimation(fadeIn);
    }

    private void applySlideInFromLeftAnimation(View view) {
        Animation slideIn = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        slideIn.setDuration(500);
        view.startAnimation(slideIn);
    }

    private void applySlideInFromRightAnimation(View view) {
        TranslateAnimation slideIn = new TranslateAnimation(300, 0, 0, 0);
        slideIn.setDuration(500);
        view.startAnimation(slideIn);
    }

    private void applyPopInAnimation(View view) {
        ScaleAnimation scale = new ScaleAnimation(
                0.7f, 1.0f, 0.7f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(300);
        view.startAnimation(scale);
    }
    // 🔊 Play mill sound
    private void playMillSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.celebrate);
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        mediaPlayer.start();
    }

    // ✨ Flash animation for pawns in mill
    private void flashPawn(ImageView pawn) {
        Animation flash = new AlphaAnimation(0.3f, 1.0f);
        flash.setDuration(200);
        flash.setRepeatMode(Animation.REVERSE);
        flash.setRepeatCount(4);
        pawn.startAnimation(flash);
    }

    // 🔄 Animate entire mill
    private void animateMill(int[] millCombo) {
        for (int i : millCombo) {
            flashPawn(boardSlots[i]);
        }
    }
    private void animateMill(int index, String color) {
        for (int[] mill : millCombinations) {
            if ((mill[0] == index || mill[1] == index || mill[2] == index) &&
                    boardState[mill[0]].equals(color) &&
                    boardState[mill[1]].equals(color) &&
                    boardState[mill[2]].equals(color)) {

                for (int slot : mill) {
                    Animation glow = new AlphaAnimation(0.3f, 1.0f);
                    glow.setDuration(200);
                    glow.setRepeatMode(Animation.REVERSE);
                    glow.setRepeatCount(4);
                    boardSlots[slot].startAnimation(glow);
                }

                return; // Only animate the first matching mill
            }
        }
    }

    private PopupWindow emojiPopup;

    private void showEmojiPopup(View anchor) {
        View popupView = getLayoutInflater().inflate(R.layout.reaction_popup, null);
        emojiPopup = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        emojiPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        emojiPopup.showAsDropDown(anchor, -200, -400);

        // Map emoji button IDs to string identifiers
        Map<Integer, String> emojiMap = new HashMap<>();
        emojiMap.put(R.id.emojiAngry, "angry");
        emojiMap.put(R.id.emojiSwag, "swag");
        emojiMap.put(R.id.emojiSurprise, "surprise");
        emojiMap.put(R.id.emojiCry, "cry");
        emojiMap.put(R.id.emojiFunny, "funny");
        emojiMap.put(R.id.emojiStock, "shock");
        emojiMap.put(R.id.emojiLove, "love");
        emojiMap.put(R.id.emojiExcited, "excited");

        // ✅ Loop through map and set safe click listeners
        for (Map.Entry<Integer, String> entry : emojiMap.entrySet()) {
            View emojiView = popupView.findViewById(entry.getKey());
            if (emojiView != null) {
                final String emojiType = entry.getValue(); // Important fix
                emojiView.setOnClickListener(v -> {
                    showEmoji(emojiType);
                    sendReceive.write(("EMOJI:" + emojiType).getBytes());
                    emojiPopup.dismiss();
                });
            } else {
                Log.e("EMOJI_ERROR", "Could not find view for ID: " + entry.getKey());
            }
        }
    }



    private void showEmoji(String type) {
        ImageView emojiReaction = findViewById(R.id.emojiReaction);
        switch (type) {
            case "angry":
                emojiReaction.setImageResource(R.drawable.sticker_angry);
                break;
            case "swag":
                emojiReaction.setImageResource(R.drawable.sticker_swag);
                break;
            case "surprise":
                emojiReaction.setImageResource(R.drawable.sticker_surprise);
                break;
            case "cry":
                emojiReaction.setImageResource(R.drawable.sticker_cry1);
                break;
            case "funny":
                emojiReaction.setImageResource(R.drawable.sticker_funny);
                break;
            case "shock":
                emojiReaction.setImageResource(R.drawable.sticker_shock);
                break;
            case "love":
                emojiReaction.setImageResource(R.drawable.sticker_love);
                break;
            case "excited":
                emojiReaction.setImageResource(R.drawable.sticker_excited);
                break;
        }

        // Load animation and start
        Animation popAnim = AnimationUtils.loadAnimation(this, R.anim.sticker_pop);
        // Combine both animations
        Animation fadeScale = AnimationUtils.loadAnimation(this, R.anim.fade_scale_in);
        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce1);
        emojiReaction.startAnimation(popAnim);

        AnimationSet combo = new AnimationSet(true);
        combo.addAnimation(fadeScale);
        combo.addAnimation(bounce);

        emojiReaction.startAnimation(combo);

        // Show and auto-hide
        emojiReaction.setVisibility(View.VISIBLE);
        new Handler().postDelayed(() -> emojiReaction.setVisibility(View.GONE), 2000);
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
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        stopTurnTimer();
        activeInstance = null;
    }
}