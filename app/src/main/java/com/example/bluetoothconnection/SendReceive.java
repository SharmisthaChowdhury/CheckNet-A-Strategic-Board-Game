package com.example.bluetoothconnection;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
public class SendReceive extends Thread {
    private final BluetoothSocket bluetoothSocket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final Handler handler;
    private boolean running = true; // Flag to control thread termination
    public SendReceive(BluetoothSocket socket, Handler handler) {
        this.bluetoothSocket = socket;
        this.handler = handler;
        InputStream tempIn = null;
        OutputStream tempOut = null;
        try {
            tempIn = bluetoothSocket.getInputStream();
            tempOut = bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        inputStream = tempIn;
        outputStream = tempOut;
    }
    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;
        while (running) { // Use the running flag to control the loop
            try {
                bytes = inputStream.read(buffer);
                // Convert bytes to a string for processing
                String receivedState = new String(buffer, 0, bytes);
                // Notify PlayActivity if it's active
                if (PlayActivity.activeInstance != null) {
                    PlayActivity.activeInstance.onOpponentMove(receivedState);
                }
                // Notify the handler (if used elsewhere in your app)
                if (handler != null) {
                    handler.obtainMessage(MainActivity2.STATE_MESSAGE_RECEIVED, bytes, -1,
                            buffer).sendToTarget();
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
    // Method to send data to the other device
    public void write(byte[] bytes) {
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            if (handler != null) {
                handler.obtainMessage(MainActivity2.STATE_CONNECTION_FAILED).sendToTarget();
            }
        }
    }
    // Method to stop the thread gracefully
    public void stopThread() {
        running = false;
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
