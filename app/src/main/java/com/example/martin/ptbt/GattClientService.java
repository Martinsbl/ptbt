package com.example.martin.ptbt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by Martin on 10.05.2017.
 */

public class GattClientService extends Service {

    private static final String TAG = "GattClientService";

    public static final String ACTION_GATT_CONNECTED = "com.example.martin.ptbt.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = "com.example.martin.ptbt.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICES_DISCOVERED = "com.example.martin.ptbt.ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_NOT_SUPPORTED = "com.example.martin.ptbt.ACTION_NOT_SUPPORTED";
    public static final String ACTION_GATT_ON_CHARACTERISTIC_READ = "com.example.mabo.myapplication.ACTION_GATT_ON_CHARACTERISTIC_READ";

    private final IBinder localBinder = new LocalBinder();

    private NrfSpeedDevice mNrfSpeedDevice;

    private class ServiceCharacteristicBundle {
        UUID service;
        UUID characteristic;
    }
    private Queue<ServiceCharacteristicBundle> mCharReadQueue = new LinkedList<>();
    private boolean mIsReading = false;

    private String mBleDeviceAddress;
    private BluetoothGatt mGatt;


    public class LocalBinder extends Binder {
        public GattClientService getService() {
            return GattClientService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: ");
        mBleDeviceAddress = intent.getStringExtra(SpeedTestActivity.EXTRA_DEVICE_ADDRESS);
        mNrfSpeedDevice = new NrfSpeedDevice();
        connectToGattServer();

        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate: ");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind: " + intent.toString());
        return localBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // TODO: 13.05.2017  Clean up BLE connection
        Log.i(TAG, "onDestroy: ");
    }


    private void connectToGattServer() {
        final BluetoothManager bleManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        final BluetoothAdapter bleAdapter = bleManager.getAdapter();
        final BluetoothDevice bleDevice = bleAdapter.getRemoteDevice(mBleDeviceAddress);

        mGatt = bleDevice.connectGatt(this, false, gattCallback);
    }

    public void disconnectFromGattServer() {
        if (mGatt == null) {
            return;
        }
        mGatt.disconnect();
        mGatt.close();
        mGatt = null;
    }

    public void readDeviceInformation() {
        readCharacteristic(NrfSpeedUUIDs.UUID_SERVICE_DEVICE_INFORMATION, NrfSpeedUUIDs.UUID_CHAR_FW_REVISION);
        readCharacteristic(NrfSpeedUUIDs.UUID_SERVICE_DEVICE_INFORMATION, NrfSpeedUUIDs.UUID_CHAR_HW_REVISION);
    }

    public void readCharacteristic(UUID serviceUuid, UUID charUuid) {
        ServiceCharacteristicBundle uuidBundle = new ServiceCharacteristicBundle();
        uuidBundle.service = serviceUuid;
        uuidBundle.characteristic = charUuid;
        mCharReadQueue.add(uuidBundle);
        readNextCharInQueue();

    }

    private void readNextCharInQueue() {
        if (mIsReading) {
            return;
        }
        if (mCharReadQueue.size() == 0) {
            return;
        }
        mIsReading = true;
        ServiceCharacteristicBundle uuidBundle = (ServiceCharacteristicBundle) mCharReadQueue.poll();
        BluetoothGattService service = mGatt.getService(uuidBundle.service);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuidBundle.characteristic);
        mGatt.readCharacteristic(characteristic);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mGatt.discoverServices();
                broadcastUpdate(ACTION_GATT_CONNECTED);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                broadcastUpdate(ACTION_GATT_DISCONNECTED); Broadcast receiver gets unregistered before callback
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mNrfSpeedDevice.setBleServices(gatt.getServices());
            for (BluetoothGattService service : mNrfSpeedDevice.getBleServices()) {
                Log.i(TAG, "onServicesDiscovered: " + service.getUuid());
            }
            broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            broadcastUpdate(ACTION_GATT_ON_CHARACTERISTIC_READ, characteristic);
            mIsReading = false;
            readNextCharInQueue();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.i(TAG, "broadcastUpdate: Char value: " + characteristic.getValue()[0]);
    }

}
