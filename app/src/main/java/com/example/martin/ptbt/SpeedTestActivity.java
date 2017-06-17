package com.example.martin.ptbt;

import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.renderscript.Double2;
import android.support.v4.app.NotificationCompatSideChannelService;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

/**
 * Created by Martin on 10.05.2017.
 */

public class SpeedTestActivity extends AppCompatActivity {

    private static final String TAG = "SpeedTestActivity";
    public static final String EXTRA_DEVICE_ADDRESS = "com.example.mabo.myapplication.EXTRA_DEVICE_ADDRESS";

    private TextView txtDeviceFw, txtDeviceHw, txtSystemId;
    private Spinner spnrPhy;
    private Switch swCfgDle, swCfgConnEvtExt;
    private EditText etxtMtu, etxtConnInterval;


    Intent intentGattClientService;
    private GattClientService mGattClientService;
    private String bleDeviceAddress;
    boolean mGattClientServiceIsBound = false;


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
        return intentFilter;
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final byte[] rawBytes = intent.getByteArrayExtra(GattClientService.EXTRA_DATA);

            Log.i(TAG, "onReceive: Action: " + action);
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
                    mGattClientService.enableNotification(NrfSpeedUUIDs.SPEED_SERVICE_UUID, NrfSpeedUUIDs.SPEED_SPAM_CHAR_UUID);
                    break;
                case GattClientService.ACTION_GATT_ON_CHARACTERISTIC_READ:
                    break;
                case GattClientService.ACTION_GATT_DIS_CHAR_FW_READ:
                    final String firmware = "Firmware: " + byteArrayToString(rawBytes);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtDeviceFw.setText(firmware);
                        }
                    });
                    break;
                case GattClientService.ACTION_GATT_DIS_CHAR_HW_READ:
                    final String hardware = "Firmware: " + byteArrayToString(rawBytes);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtDeviceHw.setText(hardware);
                        }
                    });
                    break;
                case GattClientService.ACTION_GATT_DIS_CHAR_MANUF_NAME_READ:
                    final String manufacturerName = "Manuf name: " + byteArrayToString(rawBytes);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtDeviceHw.setText(manufacturerName);
                        }
                    });
                    break;
                case GattClientService.ACTION_GATT_DIS_CHAR_MODEL_NUMBER_READ:
                    final String modelNumber = "Model number: " + byteArrayToString(rawBytes);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtDeviceFw.setText(modelNumber);
                        }
                    });
                    break;
                case GattClientService.ACTION_GATT_DIS_CHAR_SYSTEM_ID_READ:
                    final String sysId = "System ID: " + byteArrayToString(rawBytes);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtSystemId.setText(sysId);
                        }
                    });
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
        TextView bleSpeedDeviceAddress = (TextView) findViewById(R.id.txtSpeedDeviceAddress);
        bleSpeedDeviceAddress.setText(bleDeviceAddress);
        txtDeviceFw = (TextView) findViewById(R.id.txtSpeedDeviceFw);
        txtDeviceHw = (TextView) findViewById(R.id.txtSpeedDeviceHw);
        txtSystemId = (TextView) findViewById(R.id.txtSystemId);

        etxtMtu = (EditText) findViewById(R.id.etxtCfgMtu);
        etxtConnInterval = (EditText) findViewById(R.id.etxtCfgConnInterval);
        spnrPhy = (Spinner) findViewById(R.id.spnrCfgPhy);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.phy_array, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnrPhy.setAdapter(adapter);
        swCfgConnEvtExt = (Switch) findViewById(R.id.swCfgConnLenExt);
        swCfgDle = (Switch) findViewById(R.id.swCfgDle);
    }

    public void updatePhy() {
        if (spnrPhy.getSelectedItem().toString().equals("1 Mbps")) {
            mGattClientService.updatePhy(NrfSpeedDevice.BLE_GAP_PHY_1MBPS);
        } else if (spnrPhy.getSelectedItem().toString().equals("2 Mbps")) {
            mGattClientService.updatePhy(NrfSpeedDevice.BLE_GAP_PHY_2MBPS);
        } else if (spnrPhy.getSelectedItem().toString().equals("Coded")) {
            mGattClientService.updatePhy(NrfSpeedDevice.BLE_GAP_PHY_CODED);
        } else {
            Log.i(TAG, "onButtonSpeedTestActivityClick: Something went wrong when selecting PHY.");
        }
    }

    public void enableDle() {
        if (swCfgDle.isChecked()) {
            mGattClientService.enableDataLengthExtension(true);
        } else {
            mGattClientService.enableDataLengthExtension(false);
        }
    }

    public void updateMtu() {
        int mtu = Integer.parseInt(etxtMtu.getText().toString());
        mGattClientService.updateMtu(mtu);
    }

    public void updateConnectionInterval() {
        double connIntervalDouble = Double.parseDouble(etxtConnInterval.getText().toString());
        int connInterval = (int) (connIntervalDouble / 1.25);
        Log.i(TAG, "updateConnectionInterval: Double: " + etxtConnInterval.getText().toString() + ". int: " + connInterval);
        mGattClientService.updateConnInterval(connInterval);
    }

    public void enableConnEvtLenghtExtension() {
        if (swCfgConnEvtExt.isChecked()) {
            mGattClientService.enableConnEvtLengthExtension(true);
        } else {
            mGattClientService.enableConnEvtLengthExtension(false);
        }
    }

    public void onButtonSpeedTestActivityClick(View view) {
        switch (view.getId()) {
            case R.id.btnUpdatePhy:
                updatePhy();
                break;
            case R.id.btnEnableDle:
                enableDle();
                break;
            case R.id.btnUpdateMtu:
                updateMtu();
                break;
            case R.id.btnUpdateConnInterval:
                updateConnectionInterval();
                break;
            case R.id.btnEnableConnEvtLengthExtension:
                enableConnEvtLenghtExtension();
                break;
            case R.id.btnUpdateAllParameters:
                updateMtu();
                updatePhy();
                enableConnEvtLenghtExtension();
                enableDle();
                updateConnectionInterval();
                break;
            case R.id.btnStartTest:
                if (!mGattClientService.isTestRunning()) {
                    mGattClientService.startSpeedTest(true);
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
