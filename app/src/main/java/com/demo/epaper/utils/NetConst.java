package com.demo.epaper.utils;

import java.util.UUID;

public class NetConst {

    public static final String EPD_SERVICE_UUID = "0000d0ff-0000-1000-8000-00805f9b34fb";
    public static final String EPD_READ_CHAR_UUID = "0000d003-0000-1000-8000-00805f9b34fb";
    public static final String EPD_WRITE_CHAR_UUID = "0000d002-0000-1000-8000-00805f9b34fb";

    public static final String UPDATE_SERVICE_UUID = "0000fe00-0000-1000-8000-00805f9b34fb";
    public static final String UPDATE_READ_CHAR_UUID = "0000ff02-0000-1000-8000-00805f9b34fb";
    public static final String UPDATE_WRITE_CHAR_UUID = "0000ff01-0000-1000-8000-00805f9b34fb";

    public static final UUID EPD_WRITE_UUID = UUID.fromString(NetConst.EPD_WRITE_CHAR_UUID);
    public static final UUID EPD_READ_UUID = UUID.fromString(NetConst.EPD_READ_CHAR_UUID);
    public static final UUID UPDATE_WRITE_UUID = UUID.fromString(NetConst.UPDATE_WRITE_CHAR_UUID);
    public static final UUID UPDATE_READ_UUID = UUID.fromString(NetConst.UPDATE_READ_CHAR_UUID);

    public static final int REQUIRE_MINI_VERSION = 21;
    public static final int BLE_NAME_MAX = 28;

    public static final int APP_RESP_ACTION_IDX = 0;
    public static final int APP_RESP_STATUS_IDX = 1;

    public static final byte ACTION_RESULT_OK = 0x00;

    public static final byte ACTION_WRITE_RAM = 0x00;
    public static final int WRITE_RAM_HEADER_SIZE = 6;

    public static final byte ACTION_FLUSH_DISPLAY = 0x01;
    public static final byte DISPLAY_BUSY = 0x01;
    public static final byte FLUSH_DISPLAY_DONE = 0x02;

    public static final byte ACTION_READ_RAM = 0x02;
    public static final int READ_RAM_RESP_HEADER_SIZE = 2;
    public static final byte READ_RAM_OFFSET_ERR = 0x01;
    public static final byte READ_RAM_LENGTH_ERR = 0x02;

    public static final byte ACTION_READ_HARDWARE_INFO = 0x03;
    public static final int HARDWARE_INFO_TYPE_POS = 2;
    public static final byte HARDWARE_INFO_FLASH = 0x00;
    public static final byte HARDWARE_INFO_EFUSE = 0x01;
    public static final byte HARDWARE_INFO_VBAT = 0x02;

    public static final byte ACTION_ERASE_SECTOR = 0x04;

    public static final short CONFIG_BLE_MAX = 0x0001;
    public static final short CONFIG_ADV_INTERVAL = 0x0002;
    public static final short CONFIG_BLE_NAME = 0x0003;
    public static final short CONFIG_DEVICE_LICENCES = 0x0005;

    public static final byte ACTION_WRITE_CONFIG = 0x05;

    public static final byte ACTION_READ_CONFIG = 0x06;
    public static final int CONFIG_IS_ACTIVE = 0xFF01;

    public static final byte ACTION_DELETE_CONFIG = 0x07;

    public static final int CONFIG_ADDRESS = 0x28000;
    public static final int CONFIG_PAGES = 1;

    public static final int FLASH_PAGE_SIZE = 0x1000;
    public static final int SPIFS_ADDRESS_START = 0x2A000;
    public static final int SPIFS_ADDRESS_MAX = 0x40000;

    public static final int OTA_RESP_OPCODE_IDX = 1;
    public static final int OTA_RESP_STATUS_IDX = 0;

    public static final byte OTA_STATE_OK = 0x00;

    public static final byte OPCODE_APP_ADDRESS = 0x01;
    public static final byte OPCODE_GET_VERSION = 0x02;
    public static final byte OPCODE_ERASE_SECTOR = 0x03;
    public static final byte OPCODE_WRITE_PAGE = 0x05;
    public static final byte OPCODE_REBOOT = 0x09;
}
