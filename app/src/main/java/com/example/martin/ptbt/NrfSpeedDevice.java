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
    private BluetoothGattService mDeviceInformationService;
    private BluetoothGattService mAmtService;

    public BluetoothGattService getmDeviceInformationService() {
        return mDeviceInformationService;
    }

    public void setmDeviceInformationService(BluetoothGattService mDeviceInformationService) {
        this.mDeviceInformationService = mDeviceInformationService;
    }

    private List<BluetoothGattCharacteristic> mCharDeviceInformation;

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
            } else if (NrfSpeedUUIDs.UUID_CHAR_AMTS.equals(service.getUuid())) {
                mAmtService = service;
            }
        }

    }
}
