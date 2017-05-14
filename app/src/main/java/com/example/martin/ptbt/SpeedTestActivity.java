package com.example.martin.ptbt;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.LoginFilter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Martin on 10.05.2017.
 */

public class SpeedTestActivity extends AppCompatActivity {

    private static final String TAG = "SpeedTestActivity";
    public static final String EXTRA_DEVICE_ADDRESS = "com.example.mabo.myapplication.EXTRA_DEVICE_ADDRESS";
    public static final String EXTRA_NRF_SPEED_DEVICE = "com.example.mabo.myapplication.EXTRA_NRF_SPEED_DEVICE";

    private TextView bleSpeedDeviceAddress;
    private String bleDeviceAddress;
    private Button btnClose, btnRead;

    private NrfSpeedDevice mNrfSpeedDevice;

    Intent intentGattClientService;
    private GattClientService mGattClientService;
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

        mNrfSpeedDevice = new NrfSpeedDevice();
        startGattClientService();

        createGui();
    }

    private void startGattClientService() {
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, makeGattUpdateIntentFilter());

        Intent i = getIntent();
        bleDeviceAddress = i.getStringExtra(EXTRA_DEVICE_ADDRESS);
        intentGattClientService = new Intent(this, GattClientService.class);
        intentGattClientService.putExtra(EXTRA_DEVICE_ADDRESS, bleDeviceAddress);
        startService(intentGattClientService);
        bindService(intentGattClientService, serviceConnectionCallback, Context.BIND_AUTO_CREATE);
    }

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GattClientService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(GattClientService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(GattClientService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(GattClientService.ACTION_NOT_SUPPORTED);
        intentFilter.addAction(GattClientService.ACTION_GATT_ON_CHARACTERISTIC_READ);
        return intentFilter;
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "onReceive: Action: " + action);
            switch (action) {
                case GattClientService.ACTION_GATT_CONNECTED:
                    Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
                    break;
                case GattClientService.ACTION_GATT_DISCONNECTED:
                    // Not yet implemented because Broadcast receiver gets unregistered before callback
                    break;
                case GattClientService.ACTION_GATT_SERVICES_DISCOVERED:
                    break;

                default:
                    break;
            }
        }
    };

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
            mGattClientServiceIsBound = false;
            Log.i(TAG, "onServiceDisconnected: Component name: " + name.toString());
        }
    };

    private void createGui() {
        bleSpeedDeviceAddress = (TextView) findViewById(R.id.txtSpeedDeviceAddress);
        bleSpeedDeviceAddress.setText(bleDeviceAddress);


        btnClose = (Button) findViewById(R.id.btnClose);
        btnRead = (Button) findViewById(R.id.btnRead);
    }

    public void onButtonSpeedTestActivityClick(View view) {
        switch (view.getId()) {
            case R.id.btnClose:
                speedTestActivityCleanUp();
                break;
            case R.id.btnRead:
                mGattClientService.readDeviceInformation();
//                mGattClientService.readCharacteristic(NrfSpeedUUIDs.UUID_SERVICE_DEVICE_INFORMATION, NrfSpeedUUIDs.UUID_CHAR_FW_REVISION);
                break;
            default:
                break;

        }
    }

    private void speedTestActivityCleanUp() {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        mGattClientService.disconnectFromGattServer();
        if (mGattClientServiceIsBound) {
            unbindService(serviceConnectionCallback);
            mGattClientServiceIsBound = false;
        }
        stopService(intentGattClientService);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
