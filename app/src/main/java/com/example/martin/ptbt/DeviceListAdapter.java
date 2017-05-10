package com.example.martin.ptbt;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

/**
 * Created by mabo on 20.01.2017.
 */

public class DeviceListAdapter extends BaseAdapter {

    private ArrayList<ScanResult> mScanResults;

    public DeviceListAdapter() {
        this.mScanResults = new ArrayList<>();

    }

    public void addDevice(ScanResult device) {
        mScanResults.add(device);
    }

    public boolean hasDevice(ScanResult result) {
        for (ScanResult device : mScanResults) {
            if (device.getDevice().getAddress().equals(result.getDevice().getAddress()))
                return true;
        }
        return false;
    }


    public void clear() {
        this.mScanResults.clear();
    }

    private class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_list_row, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) convertView.findViewById(R.id.txtDeviceAddress);
            viewHolder.deviceName = (TextView) convertView.findViewById(R.id.txtDeviceName);
            viewHolder.deviceRssi = (TextView) convertView.findViewById(R.id.txtDeviceRssi);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final BluetoothDevice device = mScanResults.get(position).getDevice();
        final String deviceName = mScanResults.get(position).getScanRecord().getDeviceName();

        if (!TextUtils.isEmpty(deviceName))
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText("Unknown Device");
        viewHolder.deviceAddress.setText(device.getAddress());

        viewHolder.deviceRssi.setText("RSSI: -" + mScanResults.get(position).getRssi() + " dBm");

        return convertView;
    }


    @Override
    public int getCount() {
        return mScanResults.size();
    }


    @Override
    public ScanResult getItem(int position) {
        return this.mScanResults.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}
