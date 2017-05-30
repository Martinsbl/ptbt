package com.example.martin.ptbt;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

/**
 * Created by Martin on 09.05.2017.
 */

public class NrfSpeedDevice {

    private List<BluetoothGattService> mBleServices;
    private List<BluetoothGattCharacteristic> mBleDisChars;
    private List<BluetoothGattCharacteristic> mBleAmtChars;
    private BluetoothGattService mDeviceInformationService;
    private BluetoothGattService mAmtService;

    // Refer to @defgroup BLE_GAP_PHYS GAP PHY in SDK
    public final static int BLE_GAP_PHY_AUTO = 0;
    public final static int BLE_GAP_PHY_1MBPS = 1;
    public final static int BLE_GAP_PHY_2MBPS = 2;
    public final static int BLE_GAP_PHY_CODED = 4;


    public BluetoothGattService getmDeviceInformationService() {
        return mDeviceInformationService;
    }

    public void setmDeviceInformationService(BluetoothGattService mDeviceInformationService) {
        this.mDeviceInformationService = mDeviceInformationService;
    }


    public NrfSpeedDevice() {
    }

    public List<BluetoothGattService> getBleServices() {
        return mBleServices;
    }

    public void setBleServices(List<BluetoothGattService> bleServices) {
        mBleServices = bleServices;
        populateAttributeTable(mBleServices);
    }


    public void populateAttributeTable(List<BluetoothGattService> services) {
        mBleServices = services;
        for (BluetoothGattService service : mBleServices) {
            if (NrfSpeedUUIDs.UUID_SERVICE_DEVICE_INFORMATION.equals(service.getUuid())) {
                mDeviceInformationService = service;
                mBleDisChars = service.getCharacteristics();
            } else if (NrfSpeedUUIDs.SPEED_SERVICE_UUID.equals(service.getUuid())) {
                mAmtService = service;
                mBleAmtChars = service.getCharacteristics();
            }
        }
    }


    public List<BluetoothGattCharacteristic> getBleAmtChars() {
        return mBleAmtChars;
    }

    public BluetoothGattService getAmtService() {
        return mAmtService;
    }
}
