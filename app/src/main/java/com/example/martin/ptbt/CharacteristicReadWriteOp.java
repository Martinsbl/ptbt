package com.example.martin.ptbt;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by Martin on 17.06.2017.
 */

public class CharacteristicReadWriteOp {

    public enum Operation {
        READ,
        WRITE
    }

    public enum AttributeType {
        CHARACTERISTIC,
        CCCD
    }

    // TODO Make static queue and awesomeness!
//    static Queue<> transferQueue = new LinkedList();


    private UUID service;
    private UUID characteristic;
    private byte[] value;
    private Operation operation;
    private AttributeType attributeType;

    /**
     * Constructor for WRITE operations
     *
     * @param service
     * @param characteristic
     * @param value
     * @param attributeType
     */
    public CharacteristicReadWriteOp(UUID service, UUID characteristic, AttributeType attributeType, byte[] value) {
        this.service = service;
        this.characteristic = characteristic;
        this.value = value;
        this.operation = Operation.WRITE;
        this.attributeType = attributeType;
    }

    /**
     * Constructor for READ operations.
     *
     * @param service
     * @param characteristic
     * @param attributeType
     */
    public CharacteristicReadWriteOp(UUID service, UUID characteristic, AttributeType attributeType) {
        this.service = service;
        this.characteristic = characteristic;
        this.value = null;
        this.operation = Operation.READ;
        this.attributeType = attributeType;
    }


    public UUID getService() {
        return service;
    }

    public UUID getCharacteristic() {
        return characteristic;
    }

    public byte[] getValue() {
        return value;
    }

    public AttributeType getAttributeType() {
        return attributeType;
    }

    public Operation getOperation() {
        return operation;
    }
}
