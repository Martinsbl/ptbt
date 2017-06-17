package com.example.martin.ptbt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

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
    public static final String ACTION_GATT_DIS_CHAR_FW_READ = "com.example.mabo.myapplication.ACTION_GATT_DIS_CHAR_FW_READ";
    public static final String ACTION_GATT_DIS_CHAR_HW_READ = "com.example.mabo.myapplication.ACTION_GATT_DIS_CHAR_HW_READ";
    public static final String ACTION_GATT_DIS_CHAR_MANUF_NAME_READ = "com.example.mabo.myapplication.ACTION_GATT_DIS_CHAR_MANUF_NAME_READ";
    public static final String ACTION_GATT_DIS_CHAR_MODEL_NUMBER_READ = "com.example.mabo.myapplication.ACTION_GATT_DIS_CHAR_MODEL_NUMBER_READ";
    public static final String ACTION_GATT_DIS_CHAR_SYSTEM_ID_READ = "com.example.mabo.myapplication.ACTION_GATT_DIS_CHAR_SYSTEM_ID_READ";
    public static final String ACTION_GATT_SPAM_CHAR_NOTIFY = "com.example.mabo.myapplication.ACTION_GATT_SPAM_CHAR_NOTIFY";
    public static final String EXTRA_DATA = "com.example.mabo.myapplication.EXTRA_DATA";
    public static final String EXTRA_DATA_INTEGER = "com.example.mabo.myapplication.EXTRA_DATA_INTEGER";

    private final IBinder localBinder = new LocalBinder();

    private String mBleDeviceAddress;
    private BluetoothGatt mGatt = null;
    CharacteristicTransferQueue mCharacteristicTransferQueue;
    private boolean mIsConnected = false;
    private boolean mIsTestRunning = false;

    private int mTransferredBytes = 0;


    public class LocalBinder extends Binder {
        public GattClientService getService() {
            return GattClientService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: ");
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate: ");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        mBleDeviceAddress = intent.getStringExtra(SpeedTestActivity.EXTRA_DEVICE_ADDRESS);
        connectToGattServer();
        Log.i(TAG, "onBind: " + intent.toString());
        return localBinder;
    }

    private void connectToGattServer() {
        final BluetoothManager bleManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        final BluetoothAdapter bleAdapter = bleManager.getAdapter();
        final BluetoothDevice bleDevice = bleAdapter.getRemoteDevice(mBleDeviceAddress);

        mGatt = bleDevice.connectGatt(this, false, gattCallback);
        mCharacteristicTransferQueue = new CharacteristicTransferQueue(mGatt);
        Log.i(TAG, "connectToGattServer:  connect to " + mBleDeviceAddress);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // TODO: is stopself() necessary?
        stopSelf();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeGattClient();
    }


    private void closeGattClient() {
        if (mGatt == null) {
            return;
        }
        mBleDeviceAddress = null;
        mGatt.close();
        mGatt = null;
    }

    public void readDeviceInformation() {
        readCharacteristic(NrfSpeedUUIDs.UUID_SERVICE_DEVICE_INFORMATION, NrfSpeedUUIDs.UUID_CHAR_MANUFACTURER_NAME);
        readCharacteristic(NrfSpeedUUIDs.UUID_SERVICE_DEVICE_INFORMATION, NrfSpeedUUIDs.UUID_CHAR_MODEL_NUMBER_STRING);
        readCharacteristic(NrfSpeedUUIDs.UUID_SERVICE_DEVICE_INFORMATION, NrfSpeedUUIDs.UUID_CHAR_SYSTEM_ID);
    }


    public boolean enableConnEvtLengthExtension(boolean enable) {
        byte[] value = new byte[1];
        if (enable) {
            value[0] = 1;
        } else {
            value[0] = 0;
        }
        return writeCharacteristic(NrfSpeedUUIDs.SPEED_SERVICE_UUID, NrfSpeedUUIDs.SPEED_CONN_EVENT_LENGTH_EXTENSION_ENABLED_UUID, value);
    }

    public boolean enableDataLengthExtension(boolean enable) {
        byte[] value = new byte[1];
        if (enable) {
            value[0] = 1;
        } else {
            value[0] = 0;
        }
        return writeCharacteristic(NrfSpeedUUIDs.SPEED_SERVICE_UUID, NrfSpeedUUIDs.SPEED_DATA_LENGTH_EXTENSION_UUID, value);
    }

    public boolean updatePhy(int phy) {
        // Refer to @defgroup BLE_GAP_PHYS in SDK
        if (phy == NrfSpeedDevice.BLE_GAP_PHY_1MBPS ||
                phy == NrfSpeedDevice.BLE_GAP_PHY_2MBPS ||
                phy == NrfSpeedDevice.BLE_GAP_PHY_AUTO ||
                phy == NrfSpeedDevice.BLE_GAP_PHY_CODED) {
            byte[] value = new byte[1];
            value[0] = (byte) phy;
            return writeCharacteristic(NrfSpeedUUIDs.SPEED_SERVICE_UUID, NrfSpeedUUIDs.SPEED_CODED_PHY_CHAR_UUID, value);
        } else {
            return false;
        }

    }

    public boolean updateMtu(int mtu) {
        byte[] value = new byte[1];
        value[0] = (byte) mtu;
        return writeCharacteristic(NrfSpeedUUIDs.SPEED_SERVICE_UUID, NrfSpeedUUIDs.SPEED_ATT_MTU_UUID, value);
    }


    public boolean updateConnInterval(int connInterval) {
        byte[] value = new byte[2];
        value[0] = (byte) connInterval;
        value[1] = (byte) (connInterval >> 8);
        return writeCharacteristic(NrfSpeedUUIDs.SPEED_SERVICE_UUID, NrfSpeedUUIDs.SPEED_CONN_INTERVAL_UUID, value);
    }


    public boolean startSpeedTest(boolean start) {
        byte[] command = new byte[1];
        command[0] = 1;
//        if (start && !mIsTestRunning) {
//            command[0] = 1;
//            mIsTestRunning = true;
//        } else {
//            command[0] = 2;
//            mIsTestRunning = false;
//        }
        return writeCharacteristic(NrfSpeedUUIDs.SPEED_SERVICE_UUID, NrfSpeedUUIDs.SPEED_COMMAND_CHAR_UUID, command);
    }

    public boolean writeCharacteristic(UUID serviceUuid, UUID charUuid, byte[] value) {
        if (mIsConnected) {
            mCharacteristicTransferQueue.characteristicWrite(serviceUuid, charUuid, TransferBundle.AttributeType.CHARACTERISTIC, value);
            return true;
        } else {
            return false;
        }
    }

    public boolean readCharacteristic(UUID serviceUuid, UUID charUuid) {
        if (mIsConnected) {
            mCharacteristicTransferQueue.characteristicRead(serviceUuid, charUuid, TransferBundle.AttributeType.CHARACTERISTIC);
            return true;
        } else {
            return false;
        }
    }


    public boolean enableNotification(UUID serviceUuid, UUID charUuid) {
        if (mIsConnected) {
            mCharacteristicTransferQueue.descriptorWrite(serviceUuid, charUuid, TransferBundle.AttributeType.CCCD, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            return true;
        } else {
            return false;
        }
    }



    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mIsConnected = true;
                mGatt.discoverServices();
                broadcastUpdate(ACTION_GATT_CONNECTED);
                Log.i(TAG, "onConnectionStateChange: CONNECTED");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "onConnectionStateChange: DISCONNECTED");
                mIsConnected = false;
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            broadcastUpdate(ACTION_GATT_ON_CHARACTERISTIC_READ, characteristic);
            mCharacteristicTransferQueue.transferNextInQueue();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i(TAG, "onCharacteristicWrite: status: " + status);
            mCharacteristicTransferQueue.transferNextInQueue();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (characteristic.getUuid().equals(NrfSpeedUUIDs.UUID_CHAR_SPAM)) {
                mTransferredBytes += characteristic.getValue().length;
                broadcastUpdate(ACTION_GATT_SPAM_CHAR_NOTIFY, characteristic);
            }
        }


        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            mCharacteristicTransferQueue.transferNextInQueue();
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.i(TAG, "onMtuChanged: MTU: " + mtu);
        }
    };


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        Intent intent;
        if (NrfSpeedUUIDs.UUID_CHAR_FW_REVISION.equals(characteristic.getUuid())) {
            intent = new Intent(ACTION_GATT_DIS_CHAR_FW_READ);
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
        }else if (NrfSpeedUUIDs.UUID_CHAR_HW_REVISION.equals(characteristic.getUuid())) {
            intent = new Intent(ACTION_GATT_DIS_CHAR_HW_READ);
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
        }else if (NrfSpeedUUIDs.UUID_CHAR_MANUFACTURER_NAME.equals(characteristic.getUuid())) {
            intent = new Intent(ACTION_GATT_DIS_CHAR_MANUF_NAME_READ);
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
        }else if (NrfSpeedUUIDs.UUID_CHAR_MODEL_NUMBER_STRING.equals(characteristic.getUuid())) {
            intent = new Intent(ACTION_GATT_DIS_CHAR_MODEL_NUMBER_READ);
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
        }else if (NrfSpeedUUIDs.UUID_CHAR_SYSTEM_ID.equals(characteristic.getUuid())) {
            intent = new Intent(ACTION_GATT_DIS_CHAR_SYSTEM_ID_READ);
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
        }else if (NrfSpeedUUIDs.UUID_CHAR_SPAM.equals(characteristic.getUuid())) {
            intent = new Intent(ACTION_GATT_SPAM_CHAR_NOTIFY);
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
        }else {
            intent = new Intent(action);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    public boolean isTestRunning() {
        return mIsTestRunning;
    }

    public BluetoothGatt getGatt() {
        return mGatt;
    }

    public int getTransferredBytes() {
        return mTransferredBytes;
    }
}
