package com.example.martin.ptbt;

import java.util.UUID;

/**
 * Created by Martin on 17.06.2017.
 */

public class TransferBundle {


    public enum Operation {
        READ,
        WRITE
    }

    public enum AttributeType {
        CHARACTERISTIC,
        CCCD
    }

    private UUID mService;
    private UUID mCharacteristic;
    private byte[] mValue;
    private Operation mOperation;
    private AttributeType mAttributeType;

    public TransferBundle(UUID mService, UUID mCharacteristic, byte[] mValue, Operation mOperation, AttributeType mAttributeType) {
        this.mService = mService;
        this.mCharacteristic = mCharacteristic;
        this.mValue = mValue;
        this.mOperation = mOperation;
        this.mAttributeType = mAttributeType;
    }

    public UUID getService() {
        return mService;
    }

    public UUID getCharacteristic() {
        return mCharacteristic;
    }

    public byte[] getValue() {
        return mValue;
    }

    public Operation getOperation() {
        return mOperation;
    }

    public AttributeType getAttributeType() {
        return mAttributeType;
    }
}
