package com.demo.epaper.entity;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.Nullable;

public class BleDevice {
    private final String name;
    private final String mac;
    private final int rssi;

    private BluetoothDevice device;

    public BleDevice(String name, String mac, int rssi) {
        this.name = name;
        this.mac = mac;
        this.rssi = rssi;
    }

    public String getName() {
        return name;
    }

    public String getMac() {
        return mac;
    }

    public int getRssi() {
        return rssi;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public BluetoothDevice getDevice() {
        return this.device;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj == null) {
            return false;
        }
        if (obj instanceof BleDevice) {
            BleDevice device = (BleDevice)obj;
            String mac2 = device.getMac();
            String name2 = device.getName();
            return (this.mac.equals(mac2) && this.name.equals(name2));
        }
        return false;
    }
}
