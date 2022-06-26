 package com.demo.epaper.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.demo.epaper.R;
import com.demo.epaper.adapter.DeviceListAdapter;
import com.demo.epaper.adapter.FilePickerAdapter;
import com.demo.epaper.entity.BleDevice;
import com.demo.epaper.entity.FileItem;
import com.demo.epaper.fragment.PaintFragment;
import com.demo.epaper.fragment.SettingsFragment;
import com.demo.epaper.fragment.TagFragment;
import com.demo.epaper.handler.BluetoothListener;
import com.demo.epaper.handler.FilePickHandler;
import com.demo.epaper.handler.DialogClickListener;
import com.demo.epaper.network.BleTransceiver;
import com.demo.epaper.thread.CopyAssetThread;
import com.demo.epaper.thread.SingleThreadPool;
import com.demo.epaper.thread.ListFileThread;
import com.demo.epaper.utils.AppUtils;
import com.demo.epaper.view.DottedProgressBar;
import com.demo.epaper.view.MessageDialog;
import com.demo.epaper.view.QToast;
import com.demo.epaper.view.WaveView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.epaper.databinding.ActivityMainBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.BarcodeFormat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

 public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private ActivityMainBinding binding;

    private static final String[] PERMISSIONS = new String[] {
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    };

    private static final int REQUEST_PERMISSION_CODE = 100;
    private static final int REQUEST_STORAGE_CODE = 101;
    private static final int REQUEST_OPEN_BLE = 102;
    private static final int REQUEST_SCAN_BARCODE = 103;

    public static final int TAG_PAGE = 0;
    public static final int PAINT_PAGE = 1;
    public static final int SETTING_PAGE = 2;

    private FragmentManager fragmentManager;
    private Fragment[] fragments;
    private int currentPage;

    private QToast toast;
    private MessageDialog messageDialog;

    private BottomSheetDialog deviceDialog;
    private TextView tvScanTitle;
    private WaveView waveView;
    private DeviceListAdapter deviceAdapter;
    private List<BleDevice> bleDevices;
    private boolean isScanning = false;
    private static final int LE_SCAN_FOREGROUND_TIME_MAX = 10000;
    private static final int LE_SCAN_BACKGROUND_TIME_MAX = 16000;
    private BluetoothAdapter bleAdapter;
    private BluetoothLeScanner scanner;
    private int clickPosition;

    public static final int FILE_DIALOG_FILE = 0;
    public static final int FILE_DIALOG_FOLDER = 1;
    private FilePickHandler[] filePickHandlers;
    private BottomSheetDialog fileDialog;
    private TextView tvFileTitle;
    private TextView tvFilePath;
    private TextView tvFolderConfirm;
    private int pickType;
    private FilePickerAdapter pickerAdapter;
    private List<FileItem> localFiles;
    private String initialPath;
    private Stack<String> pathStack;

    private AlertDialog mAlertDialog;

    private static final int MSG_DEVICE_SELECTED = 200;
    private static final int MSG_DEVICE_CONNECTED = 201;
    private static final int MSG_DEVICE_DISCONNECTED = 202;
    private static final int MSG_GATT_CONNECTED = 203;
    private static final int MSG_GATT_NO_SERVICE = 205;

    private static final int MSG_UPDATE_DEVICE_LIST = 206;
    public static final int MSG_LOAD_FILE_SUCCESS = 207;
    public static final int MSG_EMPTY_FOLDER = 208;
    private static final int MSG_SHOW_TOAST = 209;

    private static final int STATE_CONNECTING = 0;
    private static final int STATE_DISCONNECT = 2;
    private static final int STATE_DISCOVERY_NONE_SERVICE = 3;
    private static final int STATE_DISCOVERY_SERVICE = 1;
    private static final int STATE_DISMISS = 4;

    private static final int TAG_DIALOG_BLE_NOT_SUPPORT = 100;
    private static final int TAG_DIALOG_BLE_DISCONNECT = 101;

    private boolean userDisconnect;
    private boolean silentDisconnect;
    private BleTransceiver bleTransceiver;

    private SingleThreadPool singleThreadPool;

    private ProgressDialog progressDialog;
    private PopupWindow popupWindow;
    private boolean foregroundScan;
    private String targetMacAddress;
    private String targetBleName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toast = new QToast(getApplicationContext());
        bleTransceiver = BleTransceiver.getInstance();
        bleDevices = new ArrayList<>(48);

        if(isPermissionAllGranted()) {
            initFragments();
            openBluetooth();
        }else {
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                if(!Environment.isExternalStorageManager()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_STORAGE_CODE);
                }
            }else {
                requestPermissions(PERMISSIONS, REQUEST_PERMISSION_CODE);
            }
        }

        binding.ivSearchDevice.setOnClickListener(this);
        binding.ivSearchDevice.setOnLongClickListener(this);
    }

    private static final int[] NAV_ITEM_LUT = new int[]{R.id.item_tag, R.id.item_paint, R.id.item_settings};

    private void initFragments() {
        filePickHandlers = new FilePickHandler[3];

        fragments = new Fragment[3];
        fragmentManager = getSupportFragmentManager();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

        PaintFragment fragment = new PaintFragment(MainActivity.this);
        fragments[PAINT_PAGE] = fragment;

        currentPage = PAINT_PAGE;
        transaction.add(R.id.fragment_container, fragment);
        transaction.commit();

        BottomNavigationView navigationView = binding.navView;
        navigationView.setSelectedItemId(R.id.item_paint);
        navigationView.setOnNavigationItemSelectedListener((@NonNull MenuItem item) -> {
            int position = 0;
            int itemId = item.getItemId();
            for(int i = 0; i < NAV_ITEM_LUT.length; i++) {
                if(itemId == NAV_ITEM_LUT[i]) {
                    position = i;
                    break;
                }
            }
            if (position == currentPage) {
                return true;
            }

            FragmentTransaction trans = fragmentManager.beginTransaction();
            trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            trans.hide(fragments[currentPage]);
            if(fragments[position] == null) {
                if (position == TAG_PAGE) {
                    TagFragment tagFragment = new TagFragment(MainActivity.this);
                    fragments[position] = tagFragment;
                } else if (position == PAINT_PAGE) {
                    PaintFragment paintFragment = new PaintFragment(MainActivity.this);
                    fragments[position] = paintFragment;
                } else if(position == SETTING_PAGE) {
                    SettingsFragment settingsFragment = new SettingsFragment(MainActivity.this);
                    fragments[position] = settingsFragment;
                }
                trans.add(R.id.fragment_container, fragments[position]);
            } else {
                trans.show(fragments[position]);
            }
            currentPage = position;
            trans.commit();
            return true;
        });
    }

    private final BluetoothListener bleListener = new BluetoothListener() {
        @Override
        public void onDataReceived(UUID uuid, byte[] data, int length) {
        }
        @Override
        public void onStatusChanged(int type, int data) {
            if(type == BluetoothListener.BLE_CONNECTED) {
                handler.sendEmptyMessage(MSG_DEVICE_CONNECTED);

            }else if(type == BluetoothListener.BLE_DISCONNECTED) {
                handler.sendEmptyMessage(MSG_DEVICE_DISCONNECTED);

            }else if(type == BluetoothListener.SERVICE_DISCOVERED) {
                handler.sendEmptyMessage(MSG_GATT_CONNECTED);

            }else if(type == BluetoothListener.SERVICE_INVALID) {
                handler.sendEmptyMessage(MSG_GATT_NO_SERVICE);
            }
        }
    };

    private final Handler handler = new Handler((@NonNull Message msg) -> {
        if(msg.what == MSG_DEVICE_SELECTED) {
            showConnectAlert(STATE_CONNECTING);
            connectBluetooth();

        }else if(msg.what == MSG_DEVICE_CONNECTED) {
            showConnectAlert(STATE_DISCOVERY_SERVICE);

        }else if(msg.what == MSG_DEVICE_DISCONNECTED) {
            bleTransceiver.removeReceiveListener(bleListener);
            if(!userDisconnect) {
                showConnectAlert(STATE_DISCONNECT);
            }
            if(silentDisconnect) {
                finish();
            }else {
                binding.tvTitleHolder.setText(getResources().getString(R.string.title_no_connect));
                if(userDisconnect) {
                    toast.showMessage(getResources().getString(R.string.toast_ble_disconnect), QToast.COLOR_GREEN);
                }
            }
        }else if(msg.what == MSG_GATT_CONNECTED) {
            showConnectAlert(STATE_DISMISS);
            binding.tvTitleHolder.setText(bleTransceiver.getBluetoothDevice().getName());
            toast.showMessage(getResources().getString(R.string.toast_ble_connect), QToast.COLOR_GREEN);
            if(!bleTransceiver.isStart()) {
                bleTransceiver.start();
            }
            saveRecentDevice(bleDevices.get(clickPosition));

        }else if(msg.what == MSG_GATT_NO_SERVICE) {
            showConnectAlert(STATE_DISCOVERY_NONE_SERVICE);

        }else if(msg.what == MSG_UPDATE_DEVICE_LIST) {
            if(foregroundScan) {
                deviceAdapter.notifyDataSetChanged();
            }else {
                int endIndex = bleDevices.size() - 1;
                BleDevice device = bleDevices.get(endIndex);
                if(device.getMac().equals(targetMacAddress)) {
                    stopBluetoothScan(false);
                    clickPosition = endIndex;
                    showConnectAlert(STATE_CONNECTING);
                    connectBluetooth();
                }
            }
        }else if(msg.what == MSG_LOAD_FILE_SUCCESS) {
            pickerAdapter.notifyDataSetChanged();

        }else if(msg.what == MSG_EMPTY_FOLDER) {
            localFiles.add(new FileItem(null,null, FileItem.TYPE_EMPTY, 0));
            pickerAdapter.notifyDataSetChanged();

        }else if(msg.what == MSG_SHOW_TOAST) {
            toast.showMessage((String)msg.obj, msg.arg1);
        }
        return true;
    });

     @Override
     public boolean onLongClick(View view) {
         int id = view.getId();
         if(id == R.id.iv_search_device) {
             if(bleTransceiver.isBLEConnected()) {
                 return true;
             }
             if(popupWindow == null) {
                 popupWindow = initTipPopupWindow(view);
             }
             popupWindow.showAsDropDown(view);
         }
         return true;
     }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.iv_search_device) {
            if(!isBleConnectionValid()) {
                return;
            }
            if(bleTransceiver.isBLEConnected()) {
                if(messageDialog == null) {
                    messageDialog = new MessageDialog(this);
                    messageDialog.setTitle("断开连接");
                    messageDialog.setTag(TAG_DIALOG_BLE_DISCONNECT);
                    messageDialog.setButtonText("是的", "不了");
                    messageDialog.setDialogClickListener(msgDialogListener);
                }
                messageDialog.setMessage("蓝牙标签: " + bleDevices.get(clickPosition).getName()
                        + "\nMAC地址: " + bleDevices.get(clickPosition).getMac());
                messageDialog.show();
            }else {
                if(deviceDialog == null) {
                    initDeviceDialog();
                }
                deviceDialog.show();
                if(!isScanning) {
                    foregroundScan = true;
                    startBluetoothScan(LE_SCAN_FOREGROUND_TIME_MAX);
                }
            }
        }else if(id == R.id.btn_scan) {
            if(isScanning) {
                stopBluetoothScan(false);
            }else {
                bleDevices.clear();
                startBluetoothScan(LE_SCAN_FOREGROUND_TIME_MAX);
            }
        }else if(id == R.id.tv_qr_connect) {
            popupWindow.dismiss();
            if(isBleConnectionValid()) {
                startActivityForResult(new Intent(MainActivity.this, BarcodeActivity.class), REQUEST_SCAN_BARCODE);
            }
        }else if(id == R.id.tv_recent_connect) {
            popupWindow.dismiss();
            if(!isBleConnectionValid()) {
                return;
            }
            loadRecentDevice();
            if((targetBleName == null) || (targetMacAddress == null)) {
                toast.showMessage("无上次连接记录", QToast.COLOR_GREEN);
                return;
            }
            foregroundScan = false;
            bleDevices.clear();
            startBluetoothScan(LE_SCAN_BACKGROUND_TIME_MAX);
        }
    }

    private boolean isBleConnectionValid() {
        if(bleAdapter == null) {
            toast.showMessage("获取蓝牙服务失败", QToast.COLOR_ORANGE);
            return false;
        }
        if (!bleAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_OPEN_BLE);
            return false;
        }
        LocationManager systemService = (LocationManager)getSystemService(LOCATION_SERVICE);
        if ((systemService != null) && !systemService.isProviderEnabled("network")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.main_permission_title);
            builder.setMessage(R.string.main_permission_location_alert);
            builder.setNegativeButton(R.string.common_cancel, null);
            builder.setPositiveButton(R.string.common_set, (DialogInterface dialogInterface, int i) ->
                    startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS")));
            builder.show();
            return false;
        }
        return true;
    }

    private final DialogClickListener msgDialogListener = (int which, int tag, String str) -> {
        if((which == DialogClickListener.BUTTON_LEFT) && (tag == TAG_DIALOG_BLE_DISCONNECT)) {
            userDisconnect = true;
            silentDisconnect = false;
            bleTransceiver.disconnectBLE();
        }
    };

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BleDevice bleDevice;
            ScanRecord record = result.getScanRecord();
            BluetoothDevice bluetoothDevice = result.getDevice();
            if(TextUtils.isEmpty(record.getDeviceName())) {
                bleDevice = new BleDevice("N/A", bluetoothDevice.getAddress(), result.getRssi());
            }else {
                bleDevice = new BleDevice(record.getDeviceName(), bluetoothDevice.getAddress(), result.getRssi());
            }
            if(bleDevices.contains(bleDevice)) {
                return;
            }
            bleDevice.setDevice(bluetoothDevice);
            bleDevices.add(bleDevice);
            handler.sendEmptyMessage(MSG_UPDATE_DEVICE_LIST);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

     private void openBluetooth() {
         PackageManager manager = getPackageManager();
         if(!manager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
             messageDialog = new MessageDialog(this);
             messageDialog.setTitle("设备提示");
             messageDialog.setMessage("您当前的手机不支持低功耗蓝牙(BLE)\n请更换手机后再试");
             messageDialog.setTag(TAG_DIALOG_BLE_NOT_SUPPORT);
             messageDialog.setButtonText(null, "返回");
             messageDialog.show();
             return;
         }
         BluetoothManager bluetoothSVC = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
         bleAdapter = bluetoothSVC.getAdapter();
         if(bleAdapter == null) {
             toast.showMessage("获取蓝牙服务失败", QToast.COLOR_ORANGE);
             return;
         }
         if (!bleAdapter.isEnabled()) {
             Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
             startActivityForResult(intent, REQUEST_OPEN_BLE);
         }
     }

    private void startBluetoothScan(int scanTime) {
        isScanning = true;
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        builder.setReportDelay(0L);
        builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setLegacy(false);
            builder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED);
        }
        if(scanner == null) {
            scanner = bleAdapter.getBluetoothLeScanner();
        }
        scanner.startScan(null, builder.build(), leScanCallback);

        if(foregroundScan) {
            waveView.start();
            tvScanTitle.setText(getResources().getString(R.string.title_scanning));
        }else {
            if(progressDialog == null) {
                progressDialog = initProgressDialog();
            }
            if(TextUtils.isEmpty(targetBleName)) {
                progressDialog.setMessage("MAC地址: " + targetMacAddress);
            }else {
                progressDialog.setMessage("蓝牙名称: " + targetBleName + "\nMAC地址: " + targetMacAddress);
            }
            progressDialog.show();
        }
        handler.postDelayed(()-> {
            if(isScanning) {
                stopBluetoothScan(true);
            }
        }, scanTime);
    }

    private void stopBluetoothScan(boolean isTimeout) {
        isScanning = false;
        scanner.stopScan(leScanCallback);
        if(foregroundScan) {
            waveView.clearWave();
            waveView.stop();
            if(deviceDialog.isShowing()) {
                tvScanTitle.setText(getResources().getString(R.string.title_select));
            }
        }else {
            if(progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if(isTimeout) {
                toast.showMessage("没有找到蓝牙标签", QToast.COLOR_GREEN);
            }
        }
    }

     private void connectBluetooth() {
         userDisconnect = false;
         silentDisconnect = false;
         bleTransceiver.registerReceiveListener(bleListener);
         bleTransceiver.connectWithBLE(getApplicationContext(), bleDevices.get(clickPosition).getDevice());
     }

     private ProgressDialog initProgressDialog() {
         final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
         dialog.setTitle("搜索中...");
         dialog.setIcon(R.mipmap.ic_wireless);
         dialog.setCancelable(false);
         dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
         return dialog;
     }

     private PopupWindow initTipPopupWindow(final View anchorView) {
         final View contentView = LayoutInflater.from(anchorView.getContext()).inflate(R.layout.popup_connect, null);
         TextView tvQrcode = contentView.findViewById(R.id.tv_qr_connect);
         TextView tvRecent = contentView.findViewById(R.id.tv_recent_connect);

         tvQrcode.setOnClickListener(MainActivity.this);
         tvRecent.setOnClickListener(MainActivity.this);

         float density = getResources().getDisplayMetrics().density;
         float width = AppUtils.dp2Px(density, 150);
         float height = AppUtils.dp2Px(density, 100);

         final PopupWindow popupWindow = new PopupWindow(contentView, (int)width, (int)height, false);
         popupWindow.setOutsideTouchable(true);
         popupWindow.setTouchable(true);
         popupWindow.setFocusable(false);
         return popupWindow;
     }

    @SuppressWarnings("InflateParams")
    private void initDeviceDialog() {
        deviceDialog = new BottomSheetDialog(MainActivity.this, R.style.DeviceDialogStyle);
        View dialogView = getLayoutInflater().inflate(R.layout.view_device_dialog, null);
        deviceDialog.setContentView(dialogView);

        Window window = deviceDialog.getWindow();
        if(window != null) {
            window.setGravity(Gravity.BOTTOM);
        }

        tvScanTitle = dialogView.findViewById(R.id.tv_dev_title);
        FloatingActionButton btnScan = dialogView.findViewById(R.id.btn_scan);
        btnScan.setOnClickListener(this);
        RecyclerView recyclerDevice = dialogView.findViewById(R.id.recycler_device);
        waveView = dialogView.findViewById(R.id.wave_view);

        deviceAdapter = new DeviceListAdapter(bleDevices);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerDevice.setLayoutManager(layoutManager);
        recyclerDevice.setAdapter(deviceAdapter);

        deviceAdapter.setItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            stopBluetoothScan(false);
            deviceDialog.dismiss();
            clickPosition = position;
            handler.sendEmptyMessage(MSG_DEVICE_SELECTED);
        });

        deviceDialog.setOnDismissListener((DialogInterface dialog) -> {
            if(isScanning) {
                isScanning = false;
                waveView.clearWave();
                waveView.stop();
                scanner.stopScan(leScanCallback);
            }
        });
    }

    @SuppressWarnings("InflateParams")
    private void initFileDialog() {
        if(singleThreadPool == null) {
            singleThreadPool = SingleThreadPool.getInstance();
        }
        localFiles = new ArrayList<>(64);
        localFiles.add(0, new FileItem("根目录", null, FileItem.TYPE_OPERATOR, R.mipmap.ic_file_home));
        localFiles.add(1, new FileItem("上一级", null, FileItem.TYPE_OPERATOR, R.mipmap.ic_file_backward));

        initialPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        pathStack = new Stack<>();
        pathStack.push(initialPath);

        fileDialog = new BottomSheetDialog(MainActivity.this, R.style.DeviceDialogStyle);
        View dialogView = getLayoutInflater().inflate(R.layout.view_filepicker_dialog, null);
        fileDialog.setContentView(dialogView);

        Window window = fileDialog.getWindow();
        if(window != null) {
            window.setGravity(Gravity.BOTTOM);
        }
        tvFileTitle = dialogView.findViewById(R.id.tv_file_title);
        tvFilePath = dialogView.findViewById(R.id.tv_current_path);
        tvFolderConfirm = dialogView.findViewById(R.id.tv_file_confirm);
        RecyclerView recyclerFile = dialogView.findViewById(R.id.pick_recycler);

        tvFilePath.setText(initialPath);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerFile.setLayoutManager(layoutManager);

        pickerAdapter = new FilePickerAdapter(localFiles);
        recyclerFile.setAdapter(pickerAdapter);

        tvFolderConfirm.setOnClickListener((View view) -> {
            if(pickType == FILE_DIALOG_FOLDER) {
                String path = pathStack.peek();
                fileDialog.dismiss();
                for(FilePickHandler mHandler : filePickHandlers) {
                    if(mHandler != null) {
                        mHandler.onFileSelected(FilePickHandler.APP_MSG_FOLDER_SELECTED, path, -1);
                    }
                }
            }
        });

        pickerAdapter.setItemClickListener((int type, int position) -> {
            if(position == 0) {
                if(pathStack.size() <= 1) {
                    toast.showMessage("已经是根目录了", QToast.COLOR_CYAN);
                    return;
                }
                pathStack.clear();
                pathStack.push(initialPath);
                localFiles.subList(2, localFiles.size()).clear();
                singleThreadPool.execute(new ListFileThread(initialPath, localFiles, handler));
            }else if(position == 1) {
                if(pathStack.size() <= 1) {
                    return;
                }
                pathStack.pop();
                String path = pathStack.peek();
                if(path != null) {
                    tvFilePath.setText(path);
                    localFiles.subList(2, localFiles.size()).clear();
                    singleThreadPool.execute(new ListFileThread(path, localFiles, handler));
                }
            }else {
                FileItem item = localFiles.get(position);
                if(item.getViewType() == FileItem.TYPE_FOLDER) {
                    tvFilePath.setText(item.getPath());
                    pathStack.push(item.getPath());
                    localFiles.subList(2, localFiles.size()).clear();
                    singleThreadPool.execute(new ListFileThread(item.getPath(), localFiles, handler));
                }else {
                    fileDialog.dismiss();
                    for(FilePickHandler mHandler : filePickHandlers) {
                        if(mHandler != null) {
                            mHandler.onFileSelected(
                                    FilePickHandler.APP_MSG_FILE_SELECTED, item.getPath(), item.getSize()
                            );
                        }
                    }
                }
            }
        });
    }

    private DottedProgressBar mAlertProgress;
    private TextView mAlertTitle;
    private TextView mAlertStatusText;

    private RelativeLayout mAlterButtonLine;
    private AppCompatButton mAlertReconnectButton;

    private ImageView mConnectImg;
    private LinearLayout connectStateLine;

    private LinearLayout findServiceLine;
    private ImageView service0, service1;

    private boolean allowReconnect = false;

    protected void showConnectAlert(int state) {
        if (this.mAlertDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.view_connect_alert, null);

            mAlertTitle = dialogView.findViewById(R.id.text_connect_title);

            mAlterButtonLine = dialogView.findViewById(R.id.button_line);
            mAlertReconnectButton = dialogView.findViewById(R.id.re_connect);
            AppCompatButton mAlertCancelButton = dialogView.findViewById(R.id.cancel);

            mAlertProgress = dialogView.findViewById(R.id.connect_progress_bar);
            mAlertStatusText = dialogView.findViewById(R.id.connect_status_text);
            mConnectImg = dialogView.findViewById(R.id.img_disconnect);
            connectStateLine = dialogView.findViewById(R.id.img_connect_line);

            findServiceLine = dialogView.findViewById(R.id.find_service_line);
            service0 = dialogView.findViewById(R.id.img_service0);
            service1 = dialogView.findViewById(R.id.img_service1);

            mAlertCancelButton.setOnClickListener((View view) -> {
                this.mAlertDialog.dismiss();
                binding.tvTitleHolder.setText(getResources().getString(R.string.title_no_connect));
            });
            mAlertReconnectButton.setOnClickListener((View view) -> {
                if(allowReconnect) {
                    showConnectAlert(STATE_CONNECTING);
                    connectBluetooth();
                }else {
                    mAlertDialog.dismiss();
                }
            });

            showAlertViewStatus(state);
            builder.setView(dialogView);
            builder.setCancelable(false);
            mAlertDialog = builder.show();

        } else {
            if(!mAlertDialog.isShowing()) {
                mAlertDialog.show();
            }
            showAlertViewStatus(state);
        }
    }

    private int serviceIconTag = 0;

    private void blinkServiceIcon() {
        if(serviceIconTag >= 0) {
            if(serviceIconTag == 0) {
                serviceIconTag = 1;
                AppUtils.setImageViewColor(this.service1, this);
                this.service0.setImageResource(R.mipmap.ic_service);

            }else if(serviceIconTag == 1) {
                serviceIconTag = 0;
                AppUtils.setImageViewColor(this.service0, this);
                this.service1.setImageResource(R.mipmap.ic_service);

            }
            handler.postDelayed(this::blinkServiceIcon, 250);
        }
    }

    private void showAlertViewStatus(int state) {
        if (state == STATE_CONNECTING) {
            serviceIconTag = -1;
            this.findServiceLine.setVisibility(View.INVISIBLE);
            this.connectStateLine.setVisibility(View.VISIBLE);
            this.mConnectImg.setVisibility(View.INVISIBLE);
            this.mAlertProgress.startProgress();
            this.mAlterButtonLine.setVisibility(View.GONE);
            this.mAlertStatusText.setText(R.string.common_connecting);

        } else if (state == STATE_DISCOVERY_SERVICE) {
            serviceIconTag = 0;
            this.findServiceLine.setVisibility(View.VISIBLE);
            this.connectStateLine.setVisibility(View.INVISIBLE);
            this.mAlertProgress.stopProgress();
            this.mAlertStatusText.setText(R.string.common_finding_service);
            AppUtils.setImageViewColor(this.service0, this);
            this.service1.setImageResource(R.mipmap.ic_service);
            handler.postDelayed(this::blinkServiceIcon, 250);

        } else if (state == STATE_DISCONNECT) {
            this.allowReconnect = true;
            serviceIconTag = -1;
            this.findServiceLine.setVisibility(View.INVISIBLE);
            this.connectStateLine.setVisibility(View.VISIBLE);
            this.mConnectImg.setVisibility(View.VISIBLE);
            this.mAlertProgress.stopProgress();
            this.mAlterButtonLine.setVisibility(View.VISIBLE);
            this.mAlertStatusText.setText(R.string.common_disconnected);

        } else if (state == STATE_DISCOVERY_NONE_SERVICE) {
            serviceIconTag = -1;
            this.allowReconnect = false;
            this.findServiceLine.setVisibility(View.INVISIBLE);
            this.connectStateLine.setVisibility(View.VISIBLE);
            this.mConnectImg.setVisibility(View.VISIBLE);
            this.mAlertProgress.stopProgress();
            this.mAlertTitle.setText(R.string.common_disconnected);
            this.mAlterButtonLine.setVisibility(View.VISIBLE);
            this.mAlertReconnectButton.setText(R.string.common_sure);
            this.mAlertStatusText.setText(R.string.profile_no_services_found);

        } else if (state == STATE_DISMISS) {
            serviceIconTag = -1;
            mAlertProgress.stopProgress();
            mAlertDialog.dismiss();
        }
    }

    private boolean isPermissionAllGranted() {
        for(String permission : PERMISSIONS) {
            if(ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void copyCityDatabase() {
        if(singleThreadPool == null) {
            singleThreadPool = SingleThreadPool.getInstance();
        }
        singleThreadPool.execute(new CopyAssetThread(MainActivity.this));
    }

    private void loadRecentDevice() {
        SharedPreferences preferences = getSharedPreferences("app_env", Context.MODE_PRIVATE);
        targetBleName = preferences.getString("name", null);
        targetMacAddress = preferences.getString("mac", null);
    }

    private void saveRecentDevice(BleDevice dev) {
        SharedPreferences preferences = getSharedPreferences("app_env", Context.MODE_PRIVATE);
        targetMacAddress = preferences.getString("mac", null);
        if(dev.getMac().equals(targetMacAddress)) {
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("name", dev.getName());
        editor.putString("mac", dev.getMac());
        editor.apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_PERMISSION_CODE) {
            for(int grant : grantResults) {
                if(grant != PackageManager.PERMISSION_GRANTED) {
                    toast.showMessage("请先完成授权!", QToast.COLOR_ORANGE);
                    return;
                }
            }
            initFragments();
            openBluetooth();
            copyCityDatabase();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if((requestCode >= PaintFragment.REQUEST_MIN) && (requestCode < PaintFragment.REQUEST_MAX)) {
            Fragment fragment = fragments[1];
            fragment.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if ((requestCode == REQUEST_STORAGE_CODE) && (Build.VERSION.SDK_INT > Build.VERSION_CODES.R)) {
            if (Environment.isExternalStorageManager()) {
                requestPermissions(PERMISSIONS, REQUEST_PERMISSION_CODE);
            }else {
                toast.showMessage("请先授权存储权限!", QToast.COLOR_GREEN);
            }
        }else if(requestCode == REQUEST_OPEN_BLE) {
            if((resultCode == Activity.RESULT_OK) && bleAdapter.isEnabled()) {
                toast.showMessage("蓝牙已打开", QToast.COLOR_GREEN);
            }else {
                toast.showMessage("连接价签需要打开蓝牙", QToast.COLOR_ORANGE);
            }
        }else if((requestCode == REQUEST_SCAN_BARCODE) && (resultCode == RESULT_OK) && (data != null)) {
            BarcodeFormat format = (BarcodeFormat)data.getSerializableExtra("Format");
            if(format == BarcodeFormat.CODE_128) {
                String content = data.getStringExtra("Content");
                targetMacAddress = AppUtils.formatMacAddress(content.toUpperCase());
                if(targetMacAddress != null) {
                    foregroundScan = false;
                    bleDevices.clear();
                    startBluetoothScan(LE_SCAN_BACKGROUND_TIME_MAX);
                    return;
                }
            }
            toast.showMessage("条码格式不正确", QToast.COLOR_ORANGE);
        }
    }

    public void setFilePickHandler(int fragmentId, FilePickHandler handler) {
        filePickHandlers[fragmentId] = handler;
    }

    public void showToast(final String msg, final int type, boolean switchUiThread) {
        if(switchUiThread) {
            runOnUiThread(() -> toast.showMessage(msg, type));
        }else {
            toast.showMessage(msg, type);
        }
    }

    public void showFileDialog(String title, int type) {
        if(fileDialog == null) {
            initFileDialog();
            fileDialog.show();
            singleThreadPool.execute(new ListFileThread(initialPath, localFiles, handler));
        }else {
            fileDialog.show();
        }
        this.pickType = type;
        tvFileTitle.setText(title);
        tvFolderConfirm.setVisibility((type == FILE_DIALOG_FILE) ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(KeyEvent.KEYCODE_BACK == keyCode && event.getAction() == KeyEvent.ACTION_DOWN) {
            userDisconnect = true;
            silentDisconnect = true;
            if(bleTransceiver.isBLEConnected()) {
                bleTransceiver.disconnectBLE();
            }else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        toast = null;
        binding = null;

        if(popupWindow != null &&  popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
        if(progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        filePickHandlers[0] = null;
        filePickHandlers[1] = null;
        filePickHandlers[2] = null;
        filePickHandlers = null;

        fragments[0] = null;
        fragments[1] = null;
        fragments[2] = null;
        fragments = null;
        fragmentManager = null;

        deviceAdapter = null;
        if(scanner != null && isScanning) {
            scanner.stopScan(leScanCallback);
        }
        handler.removeCallbacksAndMessages(null);

        bleDevices.clear();
        bleDevices = null;

        bleTransceiver.stopReceiver();
        bleTransceiver = null;

        if(singleThreadPool != null) {
            singleThreadPool.close();
            singleThreadPool = null;
        }
        if (pathStack != null) {
            pathStack.clear();
            pathStack = null;
            initialPath = null;
            localFiles.clear();
            localFiles = null;
        }
    }
 }