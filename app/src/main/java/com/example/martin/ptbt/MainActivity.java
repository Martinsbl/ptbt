package com.example.martin.ptbt;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanRecord;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    final private static String TAG = "MainActivity";

    DeviceListAdapter deviceAdapter;

    private BluetoothLeScannerCompat mScanner;
    private ArrayList<ScanFilter> scanFilterList;
    private Handler mScannerHandler;
    private boolean mIsScanning = false;
    private Button btnScan;
    private TextView txtScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        mScannerHandler = new Handler();

        createGui();
        prepareForScan();
        if (isBleEnabled()) {
            startLeScan();
        } else {
            Toast.makeText(this, "BLE not enabled.", Toast.LENGTH_LONG).show();
        }
    }

    private void createGui() {
        ListView deviceList = (ListView) findViewById(R.id.listDevices);
        deviceList.setAdapter(deviceAdapter = new DeviceListAdapter());
        deviceList.setOnItemClickListener(this);

        btnScan = (Button) findViewById(R.id.btnScan);
        txtScan = (TextView) findViewById(R.id.txtScan);
        txtScan.setText("Scan");
    }


    private void promptForPermission() {
        // Check if ACCESS_FINE_LOCATION premission is granted
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted yet

            // If Build.VERSION.SDK_INT >= 23 permission rationale must be shown
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user asynchronously -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult (int requestCode,String[] permissions,int[] grantResults){
        Toast.makeText(this, "Location permission granted.", Toast.LENGTH_SHORT).show();
    }

    private void prepareForScan() {
        enableBle();
        if (isBleSupported()) {
            // If Android version is Marshmallow or above we need to prompt for location permissions.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                promptForPermission();
            }

            final ParcelUuid amtUuid = new ParcelUuid(NrfSpeedUUIDs.SPEED_SERVICE_UUID_BASE);
            scanFilterList = new ArrayList<>();
            scanFilterList.add(new ScanFilter.Builder().setServiceUuid(amtUuid).build());
            mScanner = BluetoothLeScannerCompat.getScanner();
        } else {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_LONG).show();
        }
    }


    private void enableBle() {
        final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        return adapter != null && adapter.isEnabled();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }

    private boolean isBleEnabled() {
        final BluetoothManager bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        final BluetoothAdapter ba = bm.getAdapter();
        return ba != null && ba.isEnabled();
    }

    private boolean isBleSupported() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }


    private void startLeScan() {
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                // Refresh the devices list every second
                .setReportDelay(500)
                // Hardware filtering has some issues on selected devices
                .setUseHardwareFilteringIfSupported(false)
                // Samsung S6 and S6 Edge report equal value of RSSI for all devices. In this app we ignore the RSSI.
                    /*.setUseHardwareBatchingIfSupported(false)*/
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();

        mScannerHandler.postDelayed(mStopScanningTask, 10000);
        mScanner.startScan(scanFilterList, settings, scanCallback);
        deviceAdapter.clear();
        deviceAdapter.notifyDataSetChanged();
        btnScan.setText("Stop scan");
        txtScan.setText("Stop scan");
        mIsScanning = true;
    }


    private void stopLeScan() {
        mScannerHandler.removeCallbacks(mStopScanningTask);
        mIsScanning = false;
        mScanner.stopScan(scanCallback);
        btnScan.setText("Scan");
        txtScan.setText("Scan");
    }

    private Runnable mStopScanningTask = new Runnable() {
        @Override
        public void run() {
            stopLeScan();
        }
    };


    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, final ScanResult result) {
            // We scan with report delay > 0. This will never be called.
            Log.i(TAG, "onScanResult: " + result.getDevice().getAddress());
        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            for (final ScanResult result : results) {
                Log.i(TAG, "Found device: " + result.getDevice().getAddress() + " , " + result.getScanRecord().getDeviceName());
                if (!deviceAdapter.hasDevice(result)) {
                    deviceAdapter.addDevice(result);
                    deviceAdapter.notifyDataSetChanged();
                    lazyAutoConnect(); // Auto connect to first discovered device
                }
            }
        }

        @Override
        public void onScanFailed(final int errorCode) {
            // This should be handled
            Log.i(TAG, "Scan error");
        }
    };


    public void lazyAutoConnect() {
        BluetoothDevice bleDevice = deviceAdapter.getItem(0).getDevice();
        startActivity(SpeedTestActivity.createLaunchIntent(this, bleDevice.getAddress()));
        stopLeScan();
        deviceAdapter.clear();
        deviceAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BluetoothDevice bleDevice = deviceAdapter.getItem(position).getDevice();

        startActivity(SpeedTestActivity.createLaunchIntent(this, bleDevice.getAddress()));

        stopLeScan();
        deviceAdapter.clear();
        deviceAdapter.notifyDataSetChanged();
    }

    public void onButtonsClick(View view) {
        switch (view.getId()) {
            case R.id.btnScan:
                if (mIsScanning) {
                    stopLeScan();
                } else {
                    startLeScan();
                }
                break;
            default:
                break;
        }
    }

    public void onScanTextClick(View view) {
        if (!mIsScanning) {
            txtScan.setText("Stop scan");
            startLeScan();
        } else {
            txtScan.setText("Scan");
            stopLeScan();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        deviceAdapter.clear();
        stopLeScan();
    }
}
