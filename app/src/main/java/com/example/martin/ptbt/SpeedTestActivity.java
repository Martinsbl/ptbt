package com.example.martin.ptbt;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * Created by Martin on 10.05.2017.
 */

public class SpeedTestActivity extends AppCompatActivity {

    private static final String TAG = "SpeedTestActivity";
    public static final String EXTRA_DEVICE_ADDRESS = "com.example.mabo.myapplication.EXTRA_DEVICE_ADDRESS";

    private TextView txtDeviceFw, txtDeviceHw, txtThroughput;
    private Switch swCfgDle, swCfgConnEvtExt;

    Intent intentGattClientService;
    private GattClientService mGattClientService;
    private String bleDeviceAddress;
    boolean mGattClientServiceIsBound = false;

    private boolean testInProgress = false;
    private long testProgressTime;
    private long testStartTime;
    private long testTimeSinceLastNotification;

    private int receivedBytes = 0;

    public static Intent createLaunchIntent(Context context, String deviceAddress) {
        Intent intentSpeedTestActivity = new Intent(context, SpeedTestActivity.class);
        intentSpeedTestActivity.putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress);
        return intentSpeedTestActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test);

        startGattClientService();
        createGui();
    }

    private void startGattClientService() {
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, makeGattUpdateIntentFilter());
        Log.i(TAG, "startGattClientService: START CLIENT SERVICE");
        Intent i = getIntent();
        bleDeviceAddress = i.getStringExtra(EXTRA_DEVICE_ADDRESS);
        intentGattClientService = new Intent(this, GattClientService.class);
        intentGattClientService.putExtra(EXTRA_DEVICE_ADDRESS, bleDeviceAddress);
        if (!bindService(intentGattClientService, serviceConnectionCallback, Context.BIND_AUTO_CREATE)) {
            Log.i(TAG, "startGattClientService: BINDING UNSUCCESSFUL");
        }
    }

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GattClientService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(GattClientService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(GattClientService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(GattClientService.ACTION_NOT_SUPPORTED);
        intentFilter.addAction(GattClientService.ACTION_GATT_ON_CHARACTERISTIC_READ);
        intentFilter.addAction(GattClientService.ACTION_GATT_DIS_CHAR_FW_READ);
        intentFilter.addAction(GattClientService.ACTION_GATT_DIS_CHAR_HW_READ);
        intentFilter.addAction(GattClientService.ACTION_GATT_DIS_CHAR_MANUF_NAME_READ);
        intentFilter.addAction(GattClientService.ACTION_GATT_DIS_CHAR_MODEL_NUMBER_READ);
        intentFilter.addAction(GattClientService.ACTION_GATT_DIS_CHAR_SYSTEM_ID_READ);
        intentFilter.addAction(GattClientService.ACTION_GATT_SPAM_CHAR_NOTIFY);
        intentFilter.addAction(GattClientService.ACTION_GATT_SPAM_CHAR_START_TEST);
        intentFilter.addAction(GattClientService.ACTION_GATT_SPAM_CHAR_END_TEST);
        return intentFilter;
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final byte[] rawBytes = intent.getByteArrayExtra(GattClientService.EXTRA_DATA);
            switch (action) {
                case GattClientService.ACTION_GATT_CONNECTED:
                    Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
                    break;
                case GattClientService.ACTION_GATT_DISCONNECTED:
                    stopGattClientService();
                    finish();
                    break;
                case GattClientService.ACTION_GATT_SERVICES_DISCOVERED:
                    mGattClientService.readDeviceInformation();
                    mGattClientService.enableNotification(NrfSpeedUUIDs.SPEED_SERVICE_UUID, NrfSpeedUUIDs.UUID_CHAR_SPAM);
                    break;
                case GattClientService.ACTION_GATT_ON_CHARACTERISTIC_READ:
                    break;
                case GattClientService.ACTION_GATT_DIS_CHAR_MODEL_NUMBER_READ: // FW ID characteristic not yet implemented in Fw ACTION_GATT_DIS_CHAR_FW_READ:
                    final String firmware = "Firmware: " + byteArrayToString(rawBytes);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtDeviceFw.setText(firmware);
                        }
                    });
                    break;
                case GattClientService.ACTION_GATT_DIS_CHAR_SYSTEM_ID_READ: // HW ID characteristic not yet implemented in Fw ACTION_GATT_DIS_CHAR_HW_READ:
                    final String hardware = "Sys ID: " + byteArrayToString(rawBytes);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtDeviceHw.setText(hardware);
                        }
                    });
                    break;
                case GattClientService.ACTION_GATT_SPAM_CHAR_START_TEST:
                    // TODO Start timer
                    testInProgress = true;
                    testStartTime = SystemClock.uptimeMillis();
                    testTimeSinceLastNotification = 0;
                    receivedBytes += rawBytes.length;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String string = String.format(Locale.ENGLISH, "%d Bytes", receivedBytes);
                            txtThroughput.setText(string);
                        }
                    });
                    break;
                case GattClientService.ACTION_GATT_SPAM_CHAR_NOTIFY:
                    // Test in progress
                    long lastNotificationTime = testProgressTime;
                    testProgressTime += SystemClock.uptimeMillis() - testStartTime;
                    testTimeSinceLastNotification = testProgressTime - lastNotificationTime;
                    receivedBytes += rawBytes.length;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String string = String.format(Locale.ENGLISH, "%d Bytes Time: %s", receivedBytes, testProgressTime);
                            txtThroughput.setText(string);
                        }
                    });
                    break;
                case GattClientService.ACTION_GATT_SPAM_CHAR_END_TEST:
                    // TODO Stop timer
                    testProgressTime = SystemClock.uptimeMillis() - testStartTime;
                    receivedBytes += rawBytes.length;
                    final float kbps = 8 * (float) receivedBytes / (float) testProgressTime;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String string = String.format(Locale.ENGLISH, "%d Bytes, Time: %s ms, TP: %.01f kbps", receivedBytes, testProgressTime, kbps);
                            txtThroughput.setText(string);
                        }
                    });
                    // Reset and prepare for next test
                    testInProgress = false;
                    receivedBytes = 0;
                    testProgressTime = 0;
                    testTimeSinceLastNotification = 0;
                    testStartTime = 0;
                    break;
                default:
                    break;
            }
        }
    };

    private String byteArrayToString(byte[] byteArray) {
        try {
            return new String(byteArray, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "broadcastReceiver: " + e.toString());
            return "Failed to convert string";
        }
    }

    private ServiceConnection serviceConnectionCallback = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GattClientService.LocalBinder binder = (GattClientService.LocalBinder) service;
            mGattClientService = binder.getService();
            mGattClientServiceIsBound = true;
            Log.i(TAG, "onServiceConnected: Component name: " + name.toString());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Not called on unbinds.
            Log.i(TAG, "onServiceDisconnected: Component name: " + name.toString());
            mGattClientServiceIsBound = false;
        }
    };

    private void createGui() {
        txtDeviceFw = (TextView) findViewById(R.id.txtSpeedDeviceFw);
        txtDeviceHw = (TextView) findViewById(R.id.txtSpeedDeviceHw);
        txtThroughput = (TextView) findViewById(R.id.txtThroughput);

        swCfgConnEvtExt = (Switch) findViewById(R.id.swCfgConnLenExt);
        swCfgDle = (Switch) findViewById(R.id.swCfgDle);
    }

    public void onButtonSpeedTestActivityClick(View view) {
        switch (view.getId()) {
            case R.id.btnStartTest:
                if (!testInProgress) {
                    mGattClientService.startSpeedTest(true);
                } else {
                    // TODO Implement stop test function
                }
                break;
            case R.id.btnDisconnect:
                stopGattClientService();
                finish();
                break;
            default:
                break;

        }
    }

    public void onRadioMtuClick(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.radioMtu23:
                if (checked) {
                    mGattClientService.updateMtu(23);
                }
                break;
            case R.id.radioMtu128:
                if (checked) {
                    mGattClientService.updateMtu(128);
                }
                break;
            case R.id.radioMtu247:
                if (checked) {
                    mGattClientService.updateMtu(247);
                }
                break;
            default:
                break;
        }
    }


    public void onRadioCiClick(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.radioCiHigh:
                if (checked) {
                    mGattClientService.updateConnInterval(8);
                }
                break;
            case R.id.radioCiMedium:
                if (checked) {
                    mGattClientService.updateConnInterval(24); // 24 = 30ms
                }
                break;
            case R.id.radioCi400:
                if (checked) {
                    mGattClientService.updateConnInterval(320);
                }
                break;
            default:
                break;
        }
    }

    public void onRadioPhyClick(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.radioPhy1Mbps:
                if (checked) {
                    mGattClientService.updatePhy(NrfSpeedDevice.BLE_GAP_PHY_1MBPS);
                }
                break;
            case R.id.radioPhy2Mbps:
                if (checked) {
                    mGattClientService.updatePhy(NrfSpeedDevice.BLE_GAP_PHY_2MBPS);
                }
                break;
            default:
                break;
        }
    }

    public void onSwitchCleClick(View view) {
        boolean checked = ((Switch) view).isChecked();
        if (checked) {
            mGattClientService.enableConnEvtLengthExtension(true);
        } else {
            mGattClientService.enableConnEvtLengthExtension(false);
        }
    }

    public void onSwitchDleClick(View view) {
        boolean checked = ((Switch) view).isChecked();
        if (checked) {
            mGattClientService.enableDataLengthExtension(true);
        } else {
            mGattClientService.enableDataLengthExtension(false);
        }
    }

    /**
     * This will unbind and close the Gatt Client Service. BLE will be
     * closed on service's onDestroy().
     */
    private void stopGattClientService() {
        // Disconnect BLE and close service if service is active.
        if (mGattClientServiceIsBound) {
            if (mGattClientService.isConnected()) {
                mGattClientService.getGatt().disconnect();
            }
            unbindService(serviceConnectionCallback);
            mGattClientServiceIsBound = false;
            stopService(intentGattClientService);
        }
        // Close Broadcast Receiver
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        mGattClientService = null;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        stopGattClientService();
        super.onStop();
    }
}
