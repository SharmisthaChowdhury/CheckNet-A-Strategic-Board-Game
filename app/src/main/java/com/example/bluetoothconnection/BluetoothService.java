package com.example.bluetoothconnection;
public class BluetoothService {
    private static BluetoothService instance;
    private SendReceive sendReceive;
    // private boolean isInitiator; // Add this flag
    private BluetoothService() {}
    public static synchronized BluetoothService getInstance() {
        if (instance == null) {
            instance = new BluetoothService();
        }
        return instance;
    }
    public void setSendReceive(SendReceive sendReceive) {
        this.sendReceive = sendReceive;
    }
    public SendReceive getSendReceive() {
        return sendReceive;
    }
    // Add a setter for the initiator flag
// public void setInitiator(boolean isInitiator) {
    //this.isInitiator = isInitiator;
    // }
    // Add the isInitiator() method
    // public boolean isInitiator() {
    //return isInitiator;
    //}
}
