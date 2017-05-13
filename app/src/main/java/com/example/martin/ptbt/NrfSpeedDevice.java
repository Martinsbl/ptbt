package com.example.martin.ptbt;

import android.bluetooth.BluetoothGattService;

import java.util.List;
import java.util.UUID;

/**
 * Created by Martin on 09.05.2017.
 */

public class NrfSpeedDevice {

    private List<BluetoothGattService> mBleServices;

    public NrfSpeedDevice() {
    }

    public List<BluetoothGattService> getBleServices() {
        return mBleServices;
    }

    public void setBleServices(List<BluetoothGattService> mBleServices) {
        this.mBleServices = mBleServices;
    }
}
