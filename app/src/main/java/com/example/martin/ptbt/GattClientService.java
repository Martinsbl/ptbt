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
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Martin on 10.05.2017.
 */

public class GattClientService extends Service {

    private static final String TAG = "GattClientService";

    private final IBinder localBinder = new LocalBinder();

    private NrfSpeedDevice mNrfSpeedDevice;

    private String mBleDeviceAddress;
    private BluetoothGatt mGatt;


    public class LocalBinder extends Binder {
        public GattClientService getService() {
            return GattClientService.this;
        }
    }

    public GattClientService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: ");
        mBleDeviceAddress = intent.getStringExtra(SpeedTestActivity.EXTRA_DEVICE_ADDRESS);
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

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mNrfSpeedDevice = new NrfSpeedDevice();
                mGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mNrfSpeedDevice.setBleServices(gatt.getServices());
            for (BluetoothGattService service : mNrfSpeedDevice.getBleServices()) {
                Log.i(TAG, "onServicesDiscovered: " + service.getUuid());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
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

}
