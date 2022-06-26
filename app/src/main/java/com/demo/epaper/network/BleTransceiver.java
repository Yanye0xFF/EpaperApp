package com.demo.epaper.network;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import com.demo.epaper.handler.BluetoothListener;
import com.demo.epaper.utils.NetConst;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;

public class BleTransceiver extends Thread {

    private static BleTransceiver bleTransceiver;
    private List<BluetoothListener> subscribers;

    private static final int BLE_MAX_MTU = 512;

    private final AtomicBoolean atomicConnection;
    private BluetoothDevice bleDevice;
    private BluetoothGatt bleGatt;
    private int bleMTU;

    private BluetoothGattCharacteristic epdReadChar;
    private BluetoothGattCharacteristic epdWriteChar;
    private BluetoothGattCharacteristic updateReadChar;
    private BluetoothGattCharacteristic updateWriteChar;

    private Semaphore semaphore;
    private volatile boolean start;

    private UUID uuid;
    private byte[] buffer;
    private int length;

    public static BleTransceiver getInstance() {
        if(bleTransceiver == null) {
            synchronized(BleTransceiver.class) {
                if(bleTransceiver == null) {
                    bleTransceiver = new BleTransceiver();
                }
            }
        }
        return bleTransceiver;
    }

    private BleTransceiver() {
        subscribers = new ArrayList<>(4);
        buffer = new byte[256];
        atomicConnection = new AtomicBoolean(false);
    }

    public void connectWithBLE(Context context, BluetoothDevice device) {
        this.bleDevice = device;
        this.bleDevice.connectGatt(context, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                for(BluetoothListener listener : subscribers) {
                    listener.onStatusChanged(BluetoothListener.BLE_CONNECTED, 0);
                }
                atomicConnection.set(true);
                bleGatt = gatt;
                gatt.discoverServices();
            }else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                bleGatt = null;
                atomicConnection.set(false);
                gatt.close();
                for(BluetoothListener listener : subscribers) {
                    listener.onStatusChanged(BluetoothListener.BLE_DISCONNECTED, 0);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService epdService = gatt.getService(UUID.fromString(NetConst.EPD_SERVICE_UUID));
                BluetoothGattService updateService = gatt.getService(UUID.fromString(NetConst.UPDATE_SERVICE_UUID));
                if((epdService == null) && (updateService == null)) {
                    gatt.disconnect();
                    for(BluetoothListener listener : subscribers) {
                        listener.onStatusChanged(BluetoothListener.SERVICE_INVALID, 0);
                    }
                    return;
                }
                if(epdService != null) {
                    epdReadChar = epdService.getCharacteristic(UUID.fromString(NetConst.EPD_READ_CHAR_UUID));
                    epdWriteChar = epdService.getCharacteristic(UUID.fromString(NetConst.EPD_WRITE_CHAR_UUID));
                }
                if(updateService != null) {
                    updateReadChar = updateService.getCharacteristic(UUID.fromString(NetConst.UPDATE_READ_CHAR_UUID));
                    updateWriteChar = updateService.getCharacteristic(UUID.fromString(NetConst.UPDATE_WRITE_CHAR_UUID));
                }

                gatt.requestMtu(BLE_MAX_MTU);

                if(epdReadChar != null) {
                    gatt.setCharacteristicNotification(epdReadChar, true);
                }
                if(updateReadChar != null) {
                    gatt.setCharacteristicNotification(updateReadChar, true);
                }
                for(BluetoothListener listener : subscribers) {
                    listener.onStatusChanged(BluetoothListener.SERVICE_DISCOVERED, 0);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                byte[] array = characteristic.getValue();
                uuid = characteristic.getUuid();
                length = array.length;
                System.arraycopy(array, 0, buffer, 0, array.length);
                semaphore.release();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] array = characteristic.getValue();
            uuid = characteristic.getUuid();
            length = array.length;
            System.arraycopy(array, 0, buffer, 0, array.length);
            semaphore.release();
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                for(BluetoothListener listener : subscribers) {
                    listener.onStatusChanged(BluetoothListener.READ_REMOTE_RSSI, rssi);
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                bleMTU = mtu;
                for(BluetoothListener listener : subscribers) {
                    listener.onStatusChanged(BluetoothListener.MTU_CHANGED, mtu);
                }
            }else {
                bleMTU = 0;
            }
        }
    };

    @Override
    public void run() {
        start = true;
        semaphore = new Semaphore(1,true);
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
            start = false;
        }
        while(start) {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
            for(BluetoothListener listener : subscribers) {
                listener.onDataReceived(uuid, buffer, length);
            }
        }
        semaphore = null;
        bleTransceiver = null;
        bleDevice = null;
        buffer = null;
        subscribers.clear();
        subscribers = null;
    }

    public boolean isStart() {
        return start;
    }

    public void stopReceiver() {
        if(start) {
            start = false;
            this.interrupt();
        }else {
            bleTransceiver = null;
            bleDevice = null;
            buffer = null;
            subscribers.clear();
            subscribers = null;
        }
    }

    private BluetoothGattCharacteristic getWriteableCharacteristic(@NonNull UUID uuid) {
        if(epdWriteChar != null && uuid.equals(epdWriteChar.getUuid())) {
            return epdWriteChar;
        }else if(updateWriteChar != null && uuid.equals(updateWriteChar.getUuid())) {
            return updateWriteChar;
        }else {
            return null;
        }
    }

    public boolean isBLEConnected() {
        return atomicConnection.get();
    }

    public BluetoothDevice getBluetoothDevice() {
        return this.bleDevice;
    }

    public boolean writeCharacteristic(@NonNull UUID uuid, byte[] buffer) {
        BluetoothGattCharacteristic characteristic = getWriteableCharacteristic(uuid);
        if(characteristic == null) {
            return false;
        }
        if(buffer.length <= bleMTU) {
            characteristic.setValue(buffer);
            bleGatt.writeCharacteristic(characteristic);
            return true;
        }
        return false;
    }

    public void requestMaxMTU() {
        bleGatt.requestMtu(BLE_MAX_MTU);
    }

    public void requestRSSI() {
        bleGatt.readRemoteRssi();
    }

    public int getMTU() {
        return this.bleMTU;
    }

    public void disconnectBLE() {
        if(atomicConnection.get()) {
            bleGatt.disconnect();
        }
    }

    public boolean hasBLEFunction(@NonNull UUID uuid) {
        BluetoothGattCharacteristic characteristic = getWriteableCharacteristic(uuid);
        return (characteristic != null);
    }

    public void registerReceiveListener(BluetoothListener listener) {
        if(listener != null && !subscribers.contains(listener)) {
            this.subscribers.add(listener);
        }
    }

    public void removeReceiveListener(BluetoothListener listener) {
        if(listener != null && subscribers.contains(listener)) {
            this.subscribers.remove(listener);
        }
    }

}
