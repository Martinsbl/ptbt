package com.example.martin.ptbt;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by Martin on 10.05.2017.
 */

public class SpeedTestActivity extends AppCompatActivity {

    private static final String TAG = "SpeedTestActivity";
    public static final String EXTRA_DEVICE_ADDRESS = "com.example.mabo.myapplication.EXTRA_DEVICE_ADDRESS";

    private TextView bleSpeedDeviceAddress;
    private String bleDeviceAddress;
    private Button btnClose;

    Intent intentGattClientService;
    private GattClientService mGattClientService;
    boolean gattClientServiceStatus;

    public static Intent createLaunchIntent(Context context, String deviceAddress) {
        Intent intentSpeedTestActivity = new Intent(context, SpeedTestActivity.class);
        intentSpeedTestActivity.putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress);

        return intentSpeedTestActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test);

        createGui();

        Intent i = getIntent();
        bleDeviceAddress = i.getStringExtra(EXTRA_DEVICE_ADDRESS);

        intentGattClientService = new Intent(this, GattClientService.class);
        intentGattClientService.putExtra(EXTRA_DEVICE_ADDRESS, bleDeviceAddress);
        gattClientServiceStatus = true;
        startService(intentGattClientService);
        bindService(intentGattClientService, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GattClientService.LocalBinder binder = (GattClientService.LocalBinder) service;
            mGattClientService = binder.getService();
            gattClientServiceStatus = true;
            Log.i(TAG, "onServiceConnected: Component name: " + name.toString());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            gattClientServiceStatus = false;
            Log.i(TAG, "onServiceDisconnected: Component name: " + name.toString());
        }
    };

    private void createGui() {
        bleSpeedDeviceAddress = (TextView) findViewById(R.id.txtSpeedDeviceAddress);
        bleSpeedDeviceAddress.setText(bleDeviceAddress);

        btnClose = (Button) findViewById(R.id.btnClose);
    }

    public void onButtonSpeedTestActivityClick(View view) {
        switch (view.getId()) {
            case R.id.btnClose:
                speedTestActivityCleanUp();
                break;
            default:
                break;

        }
    }

    private void speedTestActivityCleanUp() {
        if (gattClientServiceStatus) {
            unbindService(serviceConnection);
            gattClientServiceStatus = false;
        }
        stopService(intentGattClientService);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
