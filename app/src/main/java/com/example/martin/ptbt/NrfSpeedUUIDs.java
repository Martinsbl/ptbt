package com.example.martin.ptbt;

import java.util.UUID;

/**
 * Created by Martin on 09.05.2017.
 */

public class NrfSpeedUUIDs {

    // GAP UUIDs
    final static UUID UUID_SERVICE_GENERIC_ACCESS =         UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    final static UUID UUID_SERVICE_GENERIC_ATTRIBUTE =      UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
    final static UUID UUID_CHAR_PFREFERRED_CONN_PARAMS =    UUID.fromString("00002a04-0000-1000-8000-00805f9b34fb");

    // DEVICE ID UUIDs
    final static UUID UUID_SERVICE_DEVICE_INFORMATION =     UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    final static UUID UUID_CHAR_HW_REVISION =               UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");
    final static UUID UUID_CHAR_FW_REVISION =               UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");

    final static UUID UUID_CCCD =                           UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // APPLICATION SPECIFIC UUIDs
    final static UUID UUID_SERVICE_AMT = UUID.fromString("0000f00d-1212-efde-1523-785fef13d123");
//    final static UUID UUID_SERVICE_AMT =                    UUID.fromString("bb4a1523-ad03-415d-a96c-9d6cddda8304");
    final static UUID UUID_CHAR_AMTS =                      UUID.fromString("bb4a1524-ad03-415d-a96c-9d6cddda8304");
    final static UUID UUID_CHAR_ATM_RCV_BYTE_CNT =          UUID.fromString("bb4a1525-ad03-415d-a96c-9d6cddda8304");

}
