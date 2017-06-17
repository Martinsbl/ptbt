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
    final static UUID UUID_CHAR_MANUFACTURER_NAME =         UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    final static UUID UUID_CHAR_MODEL_NUMBER_STRING =       UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
    final static UUID UUID_CHAR_SYSTEM_ID =                 UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb");

    final static UUID UUID_CCCD =                           UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // APPLICATION SPECIFIC UUIDs
    final static UUID UUID_SERVICE_AMT = UUID.fromString("0000f00d-1212-efde-1523-785fef13d123");
//    final static UUID UUID_SERVICE_AMT =                    UUID.fromString("bb4a1523-ad03-415d-a96c-9d6cddda8304");
    final static UUID UUID_CHAR_AMTS =                      UUID.fromString("bb4a1524-ad03-415d-a96c-9d6cddda8304");
    final static UUID UUID_CHAR_ATM_RCV_BYTE_CNT =          UUID.fromString("bb4a1525-ad03-415d-a96c-9d6cddda8304");


    final static UUID SPEED_SERVICE_UUID =                              UUID.fromString("00001420-0000-1000-8000-00805f9b34fb");
    final static UUID SPEED_ATT_MTU_UUID =                              UUID.fromString("71261421-3692-ae93-e711-8118b6022fe0");
    final static UUID SPEED_CONN_INTERVAL_UUID =                        UUID.fromString("71261422-3692-ae93-e711-8118b6022fe0");
    final static UUID SPEED_DATA_LENGTH_EXTENSION_UUID =                UUID.fromString("71261423-3692-ae93-e711-8118b6022fe0");
    final static UUID SPEED_CONN_EVENT_LENGTH_EXTENSION_ENABLED_UUID =  UUID.fromString("71261424-3692-ae93-e711-8118b6022fe0");
    final static UUID SPEED_CODED_PHY_CHAR_UUID =                       UUID.fromString("71261425-3692-ae93-e711-8118b6022fe0");
    final static UUID SPEED_COMMAND_CHAR_UUID =                         UUID.fromString("71261426-3692-ae93-e711-8118b6022fe0");
    final static UUID UUID_CHAR_SPAM =                            UUID.fromString("71261427-3692-ae93-e711-8118b6022fe0");
    // TODO: Advertised UUID doesn't match speed service uuid
    final static UUID SPEED_SERVICE_UUID_BASE =                         UUID.fromString("71261420-3692-ae93-e711-8118b6022fe0");
    final static UUID INVALID_UUID =                         UUID.fromString("71261420-3692-ae93-e711-8114b6022fe0");
}
