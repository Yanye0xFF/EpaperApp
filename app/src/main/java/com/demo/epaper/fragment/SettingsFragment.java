package com.demo.epaper.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.demo.epaper.R;
import com.demo.epaper.activity.MainActivity;
import com.demo.epaper.databinding.FragmentSettingsBinding;
import com.demo.epaper.handler.FilePickHandler;
import com.demo.epaper.handler.BluetoothListener;
import com.demo.epaper.handler.DialogClickListener;
import com.demo.epaper.handler.ThreadCallback;
import com.demo.epaper.network.BleTransceiver;
import com.demo.epaper.thread.ImageVerifyThread;
import com.demo.epaper.thread.SingleThreadPool;
import com.demo.epaper.utils.AppUtils;
import com.demo.epaper.utils.NetConst;
import com.demo.epaper.view.QToast;
import com.demo.epaper.view.UpdateDialog;
import com.demo.epaper.view.discreteseekbar.DiscreteSeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

public class SettingsFragment extends Fragment implements View.OnClickListener {
    private final Activity activity;
    private FragmentSettingsBinding binding;

    private TextView tvBinaryPath;
    private TextView tvBinaryInfo;

    private Button btnSelect, btnUpdate, btnRebootUser;
    private DiscreteSeekBar seekBar;

    private int bleAdvInterval;
    private boolean hasDeviceInfo;
    private int firmwareVersion;

    private EditText edBleName;
    private Button btnSaveBleName;

    private BleTransceiver bleTransceiver;

    private static final int FLASH_SECTOR_SIZE = 4096;

    private SingleThreadPool singleThreadPool;

    private byte[] fileContent;
    private int fileVersionCode;
    private int fileSectors;
    private int baseAddress, limitAddress, optAddress, optOffset;
    private UpdateDialog updateDialog;
    private boolean hasNext;

    private static final String[] CONFIG_MESSAGE = new String[]{"设置成功", "数据包异常", "写入失败", "任务失败"};

    public SettingsFragment(@NonNull Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        tvBinaryPath = binding.tvUpdateFile;
        tvBinaryInfo = binding.tvBinaryInfo;
        btnRebootUser = binding.btnUpdateReboot;
        btnSelect = binding.btnSelect;
        btnUpdate = binding.btnUpdate;

        edBleName = binding.edBleName;
        btnSaveBleName = binding.btnBleName;

        seekBar = binding.seekBeacon;

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        btnSelect.setOnClickListener(this);
        btnUpdate.setOnClickListener(this);
        btnSaveBleName.setOnClickListener(this);
        btnRebootUser.setOnClickListener(this);
        binding.layoutAppVersion.setOnClickListener(this);

        bleTransceiver = BleTransceiver.getInstance();

        seekBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
            }
            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                int interval =  (seekBar.getProgress() * 1000000) / 625;
                if(bleAdvInterval == interval) {
                    return;
                }
                if(bleTransceiver.isBLEConnected() && bleTransceiver.hasBLEFunction(NetConst.EPD_WRITE_UUID)) {
                    actionUpdateAdvInterval(interval);
                }
            }
        });
    }

    private static final int MSG_FILE_DECRYPTED = 100;
    private static final int MSG_SET_PREFIX = 102;
    private static final int MSG_SET_PROGRESS = 103;
    private static final int MSG_UPDATE_DONE = 104;

    private static final int MSG_UPDATE_REMOTE_RSSI = 113;
    private static final int MSG_UPDATE_FIRMWARE_VERSION = 114;
    private static final int MSG_UPDATE_VBAT = 115;
    private static final int MSG_UPDATE_ADV_INTERVAL = 116;
    private static final int MSG_BLE_CONNECTED = 117;
    private static final int MSG_BLE_DISCONNECTED = 118;

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if(msg.what == MSG_FILE_DECRYPTED) {
                tvBinaryInfo.setText(String.format(Locale.CHINA, "固件大小: %s, 版本号: %s",
                                AppUtils.formatFileSize(fileContent.length), fileVersionCode));

            }else if(msg.what == MSG_SET_PREFIX) {
                updateDialog.setProgressPrefix((String)msg.obj);

            }else if(msg.what == MSG_SET_PROGRESS) {
                updateDialog.setProgress(msg.arg1);

            }else if(msg.what == MSG_UPDATE_DONE) {
                updateDialog.dismiss();
                showToast("固件升级成功", QToast.COLOR_GREEN, false);

            }else if(msg.what == MSG_UPDATE_REMOTE_RSSI) {
                binding.signalView.setSignalLevel(AppUtils.calcRSSILevel(msg.arg1));
                binding.tvDeviceRssi.setText(String.format(Locale.CHINA, "%ddBm", msg.arg1));
                getFirmwareVersion();

            }else if(msg.what == MSG_UPDATE_FIRMWARE_VERSION) {
                firmwareVersion = msg.arg1;
                binding.tvDeviceFirmware.setText(String.format(Locale.CHINA, "固件版本: %d", firmwareVersion));
                if(bleTransceiver.hasBLEFunction(NetConst.EPD_WRITE_UUID)) {
                    readBatteryVoltage();
                }else {
                    hasDeviceInfo = true;
                    binding.cardDeviceInfo.setVisibility(View.VISIBLE);
                }

            }else if(msg.what == MSG_UPDATE_VBAT) {
                float voltage = (msg.arg1 / 1000.0F);
                if(msg.arg1 >= 3000) {
                    binding.batteryView.setBatteryLevel(100);
                }else if(msg.arg1 <= 2500) {
                    binding.batteryView.setBatteryLevel(0);
                }else {
                    int level = (msg.arg1 - 2500) * 200 / 1000;
                    binding.batteryView.setBatteryLevel(level);
                }
                binding.tvDeviceVbat.setText(String.format(Locale.CHINA, "%.3fV", voltage));
                binding.cardDeviceInfo.setVisibility(View.VISIBLE);
                hasDeviceInfo = true;

                readConfig(NetConst.CONFIG_ADV_INTERVAL);

            }else if(msg.what == MSG_UPDATE_ADV_INTERVAL) {
                bleAdvInterval = msg.arg1;
                seekBar.setProgress(msg.arg1 * 625 / 1000000);

            }else if(msg.what == MSG_BLE_CONNECTED) {
                if(!hasDeviceInfo) {
                    hasDeviceInfo = true;
                    updateConnectionInfo();
                }
            }else if(msg.what == MSG_BLE_DISCONNECTED) {
                hasDeviceInfo = false;
                binding.cardDeviceInfo.setVisibility(View.GONE);
            }
            return true;
        }
    });

    private final DialogClickListener clickListener = (int which, int tag, String str) ->
            showToast("取消升级", QToast.COLOR_GREEN, false);

    private final BluetoothListener settingBleListener = new BluetoothListener() {
        @Override
        public void onDataReceived(UUID uuid, byte[] data, int length) {
            if(NetConst.UPDATE_READ_UUID.equals(uuid)) {
                dispatchUpdateData(data, length);
            }else if(NetConst.EPD_READ_UUID.equals(uuid)) {
                dispatchAppData(data, length);
            }
        }

        @Override
        public void onStatusChanged(int type, int data) {
            if(type == BluetoothListener.READ_REMOTE_RSSI) {
                Message message = handler.obtainMessage();
                message.what = MSG_UPDATE_REMOTE_RSSI;
                message.arg1 = data;
                handler.sendMessage(message);

            }else if(type == BluetoothListener.BLE_DISCONNECTED) {
                handler.sendEmptyMessage(MSG_BLE_DISCONNECTED);

            }else if(type == BluetoothListener.SERVICE_DISCOVERED) {
                handler.sendEmptyMessage(MSG_BLE_CONNECTED);
            }
        }
    };

    private final FilePickHandler filePickHandler = (int type, String param, int agr1) -> {
        if(type == FilePickHandler.APP_MSG_FILE_SELECTED) {
            tvBinaryPath.setText(param);
            verifyBinary(param, agr1);
        }
    };

    private static final String[] OTA_ERROR_MESSAGE = new String[] {"获取存储地址失败", "读取价签版本号失败", "擦除Flash失败", "芯片擦除失败", "写入Flash失败"};

    private void dispatchUpdateData(byte[] data, int length) {
        byte opcode = data[NetConst.OTA_RESP_OPCODE_IDX];
        if(data[NetConst.OTA_RESP_STATUS_IDX] != NetConst.OTA_STATE_OK) {
            if((opcode >= NetConst.OPCODE_APP_ADDRESS) && (opcode<= NetConst.OPCODE_WRITE_PAGE)) {
                showToast( OTA_ERROR_MESSAGE[opcode - 1], QToast.COLOR_ORANGE, true);
            }
            return;
        }
        if(opcode == NetConst.OPCODE_APP_ADDRESS) {
            Message message = handler.obtainMessage();
            message.what = MSG_SET_PREFIX;
            message.obj = "正在格式化Flash";
            handler.sendMessage(message);

            int address =  AppUtils.bytes2Int(data, 4, AppUtils.INT_BYTES);
            optAddress = baseAddress = (address == 0x0) ? 0x14000 : 0x0;
            limitAddress = (baseAddress + fileSectors * FLASH_SECTOR_SIZE);
            eraseAppSpace(baseAddress);

        }else if(opcode == NetConst.OPCODE_GET_VERSION) {
            Message message = handler.obtainMessage();
            message.what = MSG_UPDATE_FIRMWARE_VERSION;
            message.arg1 = AppUtils.bytes2Int(data, 4, AppUtils.INT_BYTES);
            handler.sendMessage(message);

        }else if(opcode == NetConst.OPCODE_ERASE_SECTOR) {
            Message message = handler.obtainMessage();
            optAddress += FLASH_SECTOR_SIZE;
            if(optAddress < limitAddress) {
                message.what = MSG_SET_PROGRESS;
                message.arg1 = (optAddress * 100 / limitAddress);
                handler.sendMessage(message);

                eraseAppSpace(optAddress);
            }else {
                message.what = MSG_SET_PREFIX;
                message.obj = "正在升级固件";
                handler.sendMessage(message);

                optOffset = 0;
                optAddress = baseAddress;
                hasNext = writeAppSpace(optAddress, optOffset);
            }
        }else if(data[NetConst.OTA_RESP_OPCODE_IDX] == NetConst.OPCODE_WRITE_PAGE) {
            Message message = handler.obtainMessage();
            if(hasNext) {
                message.what = MSG_SET_PROGRESS;
                message.arg1 = (optAddress * 100 / limitAddress);
                handler.sendMessage(message);
                optAddress += 235;
                optOffset += 235;
                hasNext = writeAppSpace(optAddress, optOffset);
            }else {
                message.what = MSG_UPDATE_DONE;
                handler.sendMessage(message);
                actionRebootEpaper();
            }
        }
    }

    private void dispatchAppData(byte[] data, int length) {
        byte opcode = data[NetConst.APP_RESP_ACTION_IDX];
        byte status = data[NetConst.APP_RESP_STATUS_IDX];
        int color = (status == NetConst.ACTION_RESULT_OK) ? QToast.COLOR_GREEN : QToast.COLOR_ORANGE;

        if(opcode == NetConst.ACTION_READ_HARDWARE_INFO) {
            if(status != NetConst.ACTION_RESULT_OK) {
                showToast("获取硬件信息失败", QToast.COLOR_ORANGE, true);
                return;
            }
            byte tag = data[NetConst.HARDWARE_INFO_TYPE_POS];
            if(tag == NetConst.HARDWARE_INFO_VBAT) {
                Message message = handler.obtainMessage();
                message.what = MSG_UPDATE_VBAT;
                message.arg1 = AppUtils.bytes2Int(data, 3, 2);
                handler.sendMessage(message);
            }
        }else if(opcode == NetConst.ACTION_WRITE_CONFIG) {
            showToast(CONFIG_MESSAGE[status], color, true);

        }else if(opcode == NetConst.ACTION_READ_CONFIG) {
            if(status != NetConst.ACTION_RESULT_OK) {
                return;
            }
            int tag = AppUtils.bytes2Int(data, 2, AppUtils.SHORT_BYTES);
            int len = AppUtils.bytes2Int(data, 4, AppUtils.SHORT_BYTES);

            if((tag == NetConst.CONFIG_ADV_INTERVAL) && (len == AppUtils.SHORT_BYTES)) {
                Message message = handler.obtainMessage();
                message.what = MSG_UPDATE_ADV_INTERVAL;
                message.arg1 = AppUtils.bytes2Int(data, 6, len);
                handler.sendMessage(message);
            }
        }else if(opcode == NetConst.ACTION_DELETE_CONFIG) {
            if(status == NetConst.ACTION_RESULT_OK) {
                showToast("已设为默认名称", QToast.COLOR_GREEN, true);
            }
        }
    }

    private static final String[] DECRYPT_MESSAGE = new String[] {"固件大小不正确", "读取文件失败", "文件签名错误"};

    private final ThreadCallback threadCallback = new ThreadCallback() {
        @Override
        public void onStateChanged(int what) {
            showToast(DECRYPT_MESSAGE[what], QToast.COLOR_ORANGE, false);
            fileContent = null;
        }
        @Override
        public void onVerifyDone(byte[] data, int version) {
            fileContent = data;
            fileVersionCode = version;
            handler.sendEmptyMessage(MSG_FILE_DECRYPTED);
        }
    };

    @Override
    public void onClick(View v) {
        int id = v.getId();
        MainActivity host = ((MainActivity)activity);
        final String FUNCTION_NOT_SUPPORT = "当前不支持该功能";

        if(id == R.id.btn_select) {
            actionImportBinary();
            return;
        }
        if(!bleTransceiver.isBLEConnected()) {
            if(id != R.id.layout_app_version) {
                host.showToast("请先连接蓝牙", QToast.COLOR_ORANGE, false);
            }
            return;
        }
        if((id == R.id.btn_update) || (id == R.id.btn_update_reboot)) {
            if(!bleTransceiver.hasBLEFunction(NetConst.UPDATE_WRITE_UUID)) {
                host.showToast(FUNCTION_NOT_SUPPORT, QToast.COLOR_ORANGE, false);
                return;
            }
            if(id == R.id.btn_update) {
                actionUpdate();
            }else {
                actionRebootEpaper();
            }
        }else {
            if(!bleTransceiver.hasBLEFunction(NetConst.EPD_WRITE_UUID)) {
                host.showToast(FUNCTION_NOT_SUPPORT, QToast.COLOR_ORANGE, false);
                return;
            }
            if(id == R.id.btn_ble_name) {
                actionUpdateBleName();
            }
        }
    }

    private void actionImportBinary() {
        MainActivity main = (MainActivity)activity;
        main.showFileDialog("选择固件", MainActivity.FILE_DIALOG_FILE);
    }

    private void verifyBinary(String filePath, int fileSize) {
        fileSectors = (fileSize / FLASH_SECTOR_SIZE);
        if((fileSectors * FLASH_SECTOR_SIZE) < fileSize) {
            fileSectors++;
        }
        if(singleThreadPool == null) {
            singleThreadPool = SingleThreadPool.getInstance();
        }
        singleThreadPool.execute(new ImageVerifyThread(filePath, threadCallback));
    }

    private void actionUpdate() {
        if(fileContent == null) {
            showToast("请先选择升级固件", QToast.COLOR_ORANGE, false);
            return;
        }
        if(fileVersionCode <= firmwareVersion) {
            showToast("已经是最新版本", QToast.COLOR_ORANGE, false);
            return;
        }
        if(updateDialog == null) {
            updateDialog = new UpdateDialog(activity);
            updateDialog.setDialogClickListener(clickListener);
        }else {
            updateDialog.resetView();
        }
        updateDialog.show();
        getAppAddress();
    }

    private void getFirmwareVersion() {
        byte[] request = new byte[9];
        request[0] = NetConst.OPCODE_GET_VERSION;
        bleTransceiver.writeCharacteristic(NetConst.UPDATE_WRITE_UUID, request);
    }

    private void getAppAddress() {
        byte[] request = new byte[9];
        request[0] = NetConst.OPCODE_APP_ADDRESS;
        bleTransceiver.writeCharacteristic(NetConst.UPDATE_WRITE_UUID, request);
    }

    private void eraseAppSpace(int address) {
        byte[] request = new byte[7];
        request[0] = NetConst.OPCODE_ERASE_SECTOR;
        request[1] = 0x04;
        request[2] = 0x00;
        AppUtils.int2Bytes(request, 3, address, AppUtils.INT_BYTES);
        bleTransceiver.writeCharacteristic(NetConst.UPDATE_WRITE_UUID, request);
    }

    private boolean writeAppSpace(int address, int offset) {
        byte[] request = new byte[244];
        request[0] = NetConst.OPCODE_WRITE_PAGE;
        request[1] = (byte)0xF1;
        request[2] = 0x00;
        AppUtils.int2Bytes(request, 3, address, AppUtils.INT_BYTES);
        request[7] = (byte)0xEB;
        request[8] = 0x00;
        int length = Math.min((fileContent.length - offset), 235);
        System.arraycopy(fileContent, offset, request, 9, length);
        bleTransceiver.writeCharacteristic(NetConst.UPDATE_WRITE_UUID, request);
        return ((offset + length) < fileContent.length);
    }

    private void actionRebootEpaper() {
        byte[] request = new byte[9];
        request[0] = NetConst.OPCODE_REBOOT;
        bleTransceiver.writeCharacteristic(NetConst.UPDATE_WRITE_UUID, request);
    }

    private void actionUpdateBleName() {
        byte[] request;
        String bleName = edBleName.getText().toString();
        if(TextUtils.isEmpty(bleName)) {
            request = new byte[]{NetConst.ACTION_DELETE_CONFIG, 0x03, 0x00};
            bleTransceiver.writeCharacteristic(NetConst.EPD_WRITE_UUID, request);
            return;
        }
        byte[] bytes = bleName.getBytes(StandardCharsets.UTF_8);
        int len = bytes.length;
        if(len > NetConst.BLE_NAME_MAX) {
            showToast("蓝牙名称过长", QToast.COLOR_ORANGE, false);
            return;
        }
        request = new byte[5 + len];
        request[0] = NetConst.ACTION_WRITE_CONFIG;
        request[1] = 0x03; request[2] = 0x00;
        request[3] = (byte)(len & 0xFF); request[4] = 0x00;
        System.arraycopy(bytes,0, request, 5, len);
        bleTransceiver.writeCharacteristic(NetConst.EPD_WRITE_UUID, request);
    }

    private void readBatteryVoltage() {
        final byte[] request = new byte[]{NetConst.ACTION_READ_HARDWARE_INFO, 0x1, NetConst.HARDWARE_INFO_VBAT};
        bleTransceiver.writeCharacteristic(NetConst.EPD_WRITE_UUID, request);
    }

    private void readConfig(int tag) {
        byte[] request = new byte[3];
        request[0] = NetConst.ACTION_READ_CONFIG;
        request[1] = (byte)(tag & 0xFF);
        request[2] = (byte)((tag >>> 8) & 0xFF);
        bleTransceiver.writeCharacteristic(NetConst.EPD_WRITE_UUID, request);
    }

    private void actionUpdateAdvInterval(int interval) {
        byte[] request = new byte[7];
        request[0] = NetConst.ACTION_WRITE_CONFIG;
        request[1] = 0x02; request[2] = 0x00;
        request[3] = 0x02; request[4] = 0x00;
        bleAdvInterval = interval;
        AppUtils.int2Bytes(request, 5, interval, AppUtils.SHORT_BYTES);
        bleTransceiver.writeCharacteristic(NetConst.EPD_WRITE_UUID, request);
    }

    private void showToast(String msg, int type, boolean b) {
        MainActivity main = (MainActivity)activity;
        main.showToast(msg, type, b);
    }

    private void updateConnectionInfo() {
        String bleName = bleTransceiver.getBluetoothDevice().getName();
        String bleMac = bleTransceiver.getBluetoothDevice().getAddress();

        edBleName.setText(bleName);
        binding.tvDeviceName.setText(String.format(Locale.CHINA, "蓝牙名称: %s", bleName));
        binding.tvDeviceMac.setText(String.format(Locale.CHINA, "MAC地址: %s", bleMac));

        bleTransceiver.requestRSSI();
    }

    @Override
    public void onResume() {
        super.onResume();
        bleTransceiver.registerReceiveListener(settingBleListener);
        MainActivity main = (MainActivity)activity;
        main.setFilePickHandler(MainActivity.SETTING_PAGE, filePickHandler);
        if(!hasDeviceInfo && bleTransceiver.isBLEConnected()) {
            updateConnectionInfo();
        }
    }

    @Override
    public void onPause() {
        bleTransceiver.removeReceiveListener(settingBleListener);
        MainActivity main = (MainActivity)activity;
        main.setFilePickHandler(MainActivity.SETTING_PAGE, null);
        super.onPause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        MainActivity main = (MainActivity)activity;
        if(hidden) {
            bleTransceiver.removeReceiveListener(settingBleListener);
            main.setFilePickHandler(MainActivity.SETTING_PAGE, null);
        }else {
            bleTransceiver.registerReceiveListener(settingBleListener);
            main.setFilePickHandler(MainActivity.SETTING_PAGE, filePickHandler);
            if(!hasDeviceInfo && bleTransceiver.isBLEConnected()) {
                updateConnectionInfo();
            }
        }
    }

    @Override
    public void onDestroy() {
        binding = null;
        fileContent = null;
        if(singleThreadPool != null) {
            singleThreadPool.close();
            singleThreadPool = null;
        }

        handler.removeCallbacksAndMessages(null);
        if(updateDialog != null && updateDialog.isShowing()) {
            updateDialog.dismiss();
        }
        updateDialog = null;
        super.onDestroy();
    }
}