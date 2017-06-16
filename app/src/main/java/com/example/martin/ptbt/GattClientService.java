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
    public static final String ACTION_GATT_DIS_CHAR_FW_READ = "com.example.mabo.myapplication.ACTION_GATT_DIS_CHAR_FW_READ";
    public static final String ACTION_GATT_DIS_CHAR_HW_READ = "com.example.mabo.myapplication.ACTION_GATT_DIS_CHAR_HW_READ";
    public static final String ACTION_GATT_DIS_CHAR_MANUF_NAME_READ = "com.example.mabo.myapplication.ACTION_GATT_DIS_CHAR_MANUF_NAME_READ";
    public static final String ACTION_GATT_DIS_CHAR_MODEL_NUMBER_READ = "com.example.mabo.myapplication.ACTION_GATT_DIS_CHAR_MODEL_NUMBER_READ";
    public static final String ACTION_GATT_DIS_CHAR_SYSTEM_ID_READ = "com.example.mabo.myapplication.ACTION_GATT_DIS_CHAR_SYSTEM_ID_READ";
    public static final String EXTRA_DATA = "com.example.mabo.myapplication.EXTRA_DATA";

    private final IBinder localBinder = new LocalBinder();

    private NrfSpeedDevice mNrfSpeedDevice;

    private class ServiceCharacteristicBundle {
        UUID service;
        UUID characteristic;
    }

    private class ServiceCharacteristicWriteBundle {
        UUID service;
        UUID characteristic;
        byte[] value;
    }

    private Queue<ServiceCharacteristicBundle> mCharReadQueue = new LinkedList<>();
    private boolean mIsReading = false;

    private Queue<ServiceCharacteristicWriteBundle> mCharWriteQueue = new LinkedList<>();
    private Queue<ServiceCharacteristicBundle> mCccdWriteQueue = new LinkedList<>();
    private boolean mIsWritingChar = false;
    private boolean mIsWritingCccd = false;

    private String mBleDeviceAddress;
    private BluetoothGatt mGatt = null;
    private boolean mIsConnected = false;
    private boolean mIsTestRunning = false;
    private int mMtu = 0;


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
        mNrfSpeedDevice = new NrfSpeedDevice();
        connectToGattServer();
        Log.i(TAG, "onBind: " + intent.toString());
        return localBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind: UNBIND");
        stopSelf();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeGattClient();
//        disconnectFromGattServer();
        Log.i(TAG, "onDestroy: Gatt Client Service");
    }

    private void connectToGattServer() {
        final BluetoothManager bleManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        final BluetoothAdapter bleAdapter = bleManager.getAdapter();
        final BluetoothDevice bleDevice = bleAdapter.getRemoteDevice(mBleDeviceAddress);

        mGatt = bleDevice.connectGatt(this, false, gattCallback);
        Log.i(TAG, "connectToGattServer:  connect to " + mBleDeviceAddress);
    }

    private void shutDownService() {
        closeGattClient();
        stopSelf();
    }

    private void closeGattClient() {
        if (mGatt == null) {
            return;
        }
        mBleDeviceAddress = null;
        mGatt.close();
        Log.i(TAG, "closeGattClient: MGATT CLOSE");
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
        if (start && !mIsTestRunning) {
            command[0] = 1;
            mIsTestRunning = true;
        } else {
            command[0] = 2;
            mIsTestRunning = false;
        }
        return writeCharacteristic(NrfSpeedUUIDs.SPEED_SERVICE_UUID, NrfSpeedUUIDs.SPEED_COMMAND_CHAR_UUID, command);
    }

    public boolean writeCharacteristic(UUID serviceUuid, UUID charUuid, byte[] value) {
        if (mIsConnected) {
            ServiceCharacteristicWriteBundle bundle = new ServiceCharacteristicWriteBundle();
            bundle.service = serviceUuid;
            bundle.characteristic = charUuid;
            bundle.value = value;
            mCharWriteQueue.add(bundle);
            writeNextCharInQueue();
            return true;
        } else {
            return false;
        }
    }

    public boolean readCharacteristic(UUID serviceUuid, UUID charUuid) {
        if (mIsConnected) {
            try {
                ServiceCharacteristicBundle uuidBundle = new ServiceCharacteristicBundle();
                uuidBundle.service = serviceUuid;
                uuidBundle.characteristic = charUuid;
                mCharReadQueue.add(uuidBundle);
                readNextCharInQueue();
                return true;
            } catch (Exception e) {
                Log.i(TAG, "readCharacteristic: Exception: " + e);
                return false;
            }
        } else {
            return false;
        }
    }


    public boolean enableNotification(UUID serviceUuid, UUID charUuid) {
        if (mIsConnected) {
            ServiceCharacteristicBundle bundle = new ServiceCharacteristicBundle();
            bundle.service = serviceUuid;
            bundle.characteristic = charUuid;
            mCccdWriteQueue.add(bundle);
            writeNextCharInQueue();
            return true;
        } else {
            return false;
        }
    }

    private void writeNextCccdInQueue() {
        if (mIsWritingCccd) {
            return;
        }
        if (mCccdWriteQueue.size() == 0) {
            return;
        }
        mIsWritingCccd = true;
        ServiceCharacteristicWriteBundle bundle = mCharWriteQueue.poll();
        BluetoothGattService service = mGatt.getService(bundle.service);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(bundle.characteristic);
        BluetoothGattDescriptor cccd = characteristic.getDescriptor(NrfSpeedUUIDs.UUID_CCCD);
        cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mGatt.writeDescriptor(cccd);
        Log.i(TAG, "writeNextCccdInQueue: wrote to CCCD at char: " + characteristic.getUuid());
    }

    private void writeNextCharInQueue() {
        if (mIsWritingChar) {
            return;
        }
        if (mCharWriteQueue.size() == 0) {
            return;
        }
        mIsWritingChar = true;
        ServiceCharacteristicWriteBundle bundle = mCharWriteQueue.poll();
        BluetoothGattService service = mGatt.getService(bundle.service);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(bundle.characteristic);
        characteristic.setValue(bundle.value);
        mGatt.writeCharacteristic(characteristic);
        Log.i(TAG, "writeNextCharInQueue: wrote to char: " + characteristic.getUuid());
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
                mIsConnected = true;
                mGatt.discoverServices();
                broadcastUpdate(ACTION_GATT_CONNECTED);
                Log.i(TAG, "onConnectionStateChange: CONNECTED");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "onConnectionStateChange: DISCONNECTED");
                mIsConnected = false;
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
//                shutDownService();
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
            mIsWritingChar = false;
            Log.i(TAG, "onCharacteristicWrite: status: " + status);
            writeNextCharInQueue();
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
            mIsWritingCccd = false;
            writeNextCccdInQueue();
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            mMtu = mtu;
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
        } else {
            intent = new Intent(action);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.i(TAG, "broadcastUpdate: Intent: " + intent.getAction() + ", value: " + characteristic.getValue()[0]);
    }

    public int getMtu() {
        return mMtu;
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
}
