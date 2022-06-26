package com.demo.epaper.handler;

import java.util.UUID;

public interface BluetoothListener {

    void onDataReceived(UUID uuid, byte[] data, int length);

    int BLE_CONNECTED = 0;
    int BLE_DISCONNECTED = 1;

    int SERVICE_DISCOVERED = 2;
    int SERVICE_INVALID = 3;

    int READ_REMOTE_RSSI = 4;
    int MTU_CHANGED = 5;

    void onStatusChanged(int type, int data);
}
