package com.example.martin.ptbt;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by Martin on 17.06.2017.
 */

public class CharacteristicReadWriteQueue {

    private Queue<TransferBundle> mTransferQueue = new LinkedList<>();
    private boolean mTransferInProgress = false;

    private BluetoothGatt mGatt;

    public CharacteristicReadWriteQueue(BluetoothGatt mGatt) {
        this.mGatt = mGatt;
    }

    /**
     * WRITE operations
     */
    public void characteristicWrite(UUID service, UUID characteristic, TransferBundle.AttributeType attributeType, byte[] value) {
        TransferBundle transferBundle = new TransferBundle(service, characteristic, value, TransferBundle.Operation.WRITE, attributeType);
        mTransferQueue.add(transferBundle);
        transferFromQueue();
    }

    /**
     * READ operations.
     */
    public void characteristicRead(UUID service, UUID characteristic, TransferBundle.AttributeType attributeType) {
        TransferBundle transferBundle = new TransferBundle(service, characteristic, null, TransferBundle.Operation.READ, attributeType);
        mTransferQueue.add(transferBundle);
        transferFromQueue();
    }

    public void descriptorWrite(UUID service, UUID characteristic, TransferBundle.AttributeType attributeType, byte[] value) {
        TransferBundle transferBundle = new TransferBundle(service, characteristic, value, TransferBundle.Operation.WRITE, attributeType);
        mTransferQueue.add(transferBundle);
        transferFromQueue();
    }

    private void transferFromQueue() {
        // If transfer in progress then wait until transfer callback and call transferNextInQueue()
        if (mTransferInProgress) {
            return;
        }
        if (mTransferQueue.size() == 0) {
            return;
        }
        mTransferInProgress = true;
        TransferBundle transferBundle = mTransferQueue.poll();
        BluetoothGattService service = mGatt.getService(transferBundle.getService());
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(transferBundle.getCharacteristic());
        switch (transferBundle.getOperation()) {
            case READ:
                mGatt.readCharacteristic(characteristic);
                break;
            case WRITE:
                if (transferBundle.getAttributeType() == TransferBundle.AttributeType.CHARACTERISTIC) {
                    characteristic.setValue(transferBundle.getValue());
                    mGatt.writeCharacteristic(characteristic);
                } else if (transferBundle.getAttributeType() == TransferBundle.AttributeType.CCCD) {
                    mGatt.setCharacteristicNotification(characteristic, true);
                    BluetoothGattDescriptor cccd = characteristic.getDescriptor(NrfSpeedUUIDs.UUID_CCCD);
                    cccd.setValue(transferBundle.getValue());
                    mGatt.writeDescriptor(cccd);
                }
                break;
            default:
                break;
        }
    }

    /**
     * This function should only be called from Gatt Callbacks after transfers are completed.
     */
    public void transferNextInQueue() {
        mTransferInProgress = false;
        transferFromQueue();
    }
}
