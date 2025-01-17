package com.example.bluetoothconnection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class MainActivity2 extends AppCompatActivity {

    public static final int STATE_MESSAGE_RECEIVED = 1;
    private static final int REQUEST_ENABLE_BLUETOOTH = 2;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 3;

    public static final int STATE_CONNECTED = 4;
    public static final int STATE_CONNECTION_FAILED = 5;
    public static final int STATE_CONNECTION_REQUEST_RECEIVED = 6;

    private static final String APP_NAME = "BluetoothConnection";
    private static final UUID MY_UUID = UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66");

    Button listDevices, playButton, rulesButton;
    ListView listView;
    TextView status;
    ImageButton help;

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] btArray;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewByIdes();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Check and request Bluetooth permissions
        checkBluetoothPermissions();

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        }

        implementListeners();

        // Start the server to listen for incoming connections
        new ServerClass().start();
    }

    private void findViewByIdes() {
        listView = findViewById(R.id.listview);
        status = findViewById(R.id.status);
        listDevices = findViewById(R.id.listDevices);
        playButton = findViewById(R.id.playButton);
        rulesButton = findViewById(R.id.rules);
        help = findViewById(R.id.help);
    }

    private void checkBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    private void implementListeners() {
        listDevices.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                Set<BluetoothDevice> bt = bluetoothAdapter.getBondedDevices();
                String[] strings = new String[bt.size()];
                btArray = new BluetoothDevice[bt.size()];
                int index = 0;

                if (bt.size() > 0) {
                    for (BluetoothDevice device : bt) {
                        btArray[index] = device;
                        strings[index] = device.getName();
                        index++;
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, strings);
                    listView.setAdapter(arrayAdapter);
                }
            } else {
                Toast.makeText(MainActivity2.this, "Bluetooth permissions required", Toast.LENGTH_SHORT).show();
            }
        });

        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            if (ContextCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                ClientClass clientClass = new ClientClass(btArray[i]);
                clientClass.start();
                status.setText("Waiting for response from " + btArray[i].getName());
            } else {
                Toast.makeText(MainActivity2.this, "Bluetooth permissions required", Toast.LENGTH_SHORT).show();
            }
        });

        playButton.setOnClickListener(view -> {
            SendReceive sendReceive = BluetoothService.getInstance().getSendReceive();
            if (sendReceive != null) {
                String gameStartMessage = "GAME_START";
                sendReceive.write(gameStartMessage.getBytes());
                startGameCountdown(); // Start the countdown on the sender device
            } else {
                Toast.makeText(MainActivity2.this, "Not connected!", Toast.LENGTH_SHORT).show();
            }
        });


        rulesButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity2.this, RulesActivity.class);
            startActivity(intent);
        });
        help.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity2.this, HelpActivity.class);
            startActivity(intent);
        });


    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case STATE_CONNECTED:
                    status.setText("Connected and Ready to Play");
                    break;

                case STATE_CONNECTION_FAILED:
                    status.setText("Connection Failed. Please try again.");
                    break;

                case STATE_CONNECTION_REQUEST_RECEIVED:
                    BluetoothSocket socket = (BluetoothSocket) msg.obj;
                    if (socket != null) {
                        showConnectionRequestDialog(socket);
                    } else {
                        status.setText("Failed to receive connection request.");
                    }
                    break;

                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuffer = (byte[]) msg.obj;
                    String receivedMessage = new String(readBuffer, 0, msg.arg1);

                    if (receivedMessage.equals("CONNECTION_ESTABLISHED")) {
                        // Update the status on both devices
                        status.setText("Connected and Ready to Play");
                    } else if (receivedMessage.equals("GAME_START")) {
                        status.setText("Game starting in 5 seconds");
                        startGameCountdown();
                    } else if (receivedMessage.equals("CONNECTION_DECLINED")) {
                        status.setText("Connection Declined by opponent.");
                    } else if (receivedMessage.equals("PLAY_REQUEST")) {
                        status.setText("Play Request received.");
                    }
                    break;
            }
            return true;
        }
    });

    private void startGameCountdown() {
        final int[] countdown = {5}; // Countdown starts from 5
        Handler countdownHandler = new Handler();

        // Runnable to update the status every second
        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    status.setText("Game starting in " + countdown[0] + " seconds");
                    countdown[0]--; // Decrement the countdown value
                    countdownHandler.postDelayed(this, 1000); // Run again after 1 second
                } else {
                    // Countdown is over, start the game
                    status.setText("Starting the game...");
                    Intent intent = new Intent(MainActivity2.this, PlayActivity.class);
                    startActivity(intent);
                }
            }
        };

        // Start the countdown
        countdownHandler.post(countdownRunnable);
    }


    private void showConnectionRequestDialog(BluetoothSocket socket) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        new AlertDialog.Builder(MainActivity2.this)
                .setTitle("Connection Request")
                .setMessage("Do you want to play with " + socket.getRemoteDevice().getName() + "?")
                .setPositiveButton("Accept", (dialog, which) -> {
                    try {
                        SendReceive sendReceive = new SendReceive(socket, handler);
                        BluetoothService.getInstance().setSendReceive(sendReceive);
                        sendReceive.start();

                        // Update receiver's status
                        handler.obtainMessage(STATE_CONNECTED).sendToTarget();

                        // Notify sender that the connection is established
                        sendReceive.write("CONNECTION_ESTABLISHED".getBytes());
                    } catch (Exception e) {
                        e.printStackTrace();
                        handler.obtainMessage(STATE_CONNECTION_FAILED).sendToTarget();
                    }
                })
                .setNegativeButton("Decline", (dialog, which) -> {
                    try {
                        // Notify the sender about the decline
                        SendReceive sendReceive = new SendReceive(socket, handler);
                        sendReceive.write("CONNECTION_DECLINED".getBytes());

                        // Update receiver's status
                        status.setText("Connection Declined");

                        // Close the socket
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })
                .setCancelable(false)
                .show();
    }


    private class ClientClass extends Thread {
        private BluetoothSocket socket;

        public ClientClass(BluetoothDevice device) {
            BluetoothSocket tempSocket = null; // Initialize with a temporary value
            try {
                // Check for BLUETOOTH_CONNECT permission
                if (ActivityCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    tempSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                } else {
                    handler.obtainMessage(STATE_CONNECTION_FAILED).sendToTarget();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = tempSocket; // Assign the properly initialized socket
        }

        public void run() {
            if (socket == null) {
                handler.obtainMessage(STATE_CONNECTION_FAILED).sendToTarget();
                return;
            }

            try {
                // Check for BLUETOOTH_CONNECT permission before calling connect
                if (ActivityCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    socket.connect();
                    handler.obtainMessage(STATE_CONNECTED).sendToTarget();

                    SendReceive sendReceive = new SendReceive(socket, handler);
                    BluetoothService.getInstance().setSendReceive(sendReceive);
                    sendReceive.start();
                } else {
                    handler.obtainMessage(STATE_CONNECTION_FAILED).sendToTarget();
                }
            } catch (IOException e) {
                e.printStackTrace();
                handler.obtainMessage(STATE_CONNECTION_FAILED).sendToTarget();
                try {
                    socket.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
            }
        }
    }


    private class ServerClass extends Thread {
        private BluetoothServerSocket serverSocket;

        public ServerClass() {
            BluetoothServerSocket tempSocket = null; // Initialize with a temporary value
            try {
                // Check for BLUETOOTH_CONNECT permission
                if (ActivityCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    tempSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
                } else {
                    handler.obtainMessage(STATE_CONNECTION_FAILED).sendToTarget();
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverSocket = tempSocket; // Assign the properly initialized server socket
        }

        public void run() {
            if (serverSocket == null) {
                handler.obtainMessage(STATE_CONNECTION_FAILED).sendToTarget();
                return;
            }

            BluetoothSocket socket = null;
            try {
                // Accept connections in a loop
                while (true) {
                    try {
                        // Check for BLUETOOTH_CONNECT permission before accepting connections
                        if (ActivityCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            socket = serverSocket.accept();
                            if (socket != null) {
                                handler.obtainMessage(STATE_CONNECTION_REQUEST_RECEIVED, socket).sendToTarget();
                                break;
                            }
                        } else {
                            handler.obtainMessage(STATE_CONNECTION_FAILED).sendToTarget();
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        handler.obtainMessage(STATE_CONNECTION_FAILED).sendToTarget();
                        break;
                    }
                }
            } finally {
                try {
                    if (serverSocket != null) {
                        serverSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}