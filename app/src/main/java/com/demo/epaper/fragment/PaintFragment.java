package com.demo.epaper.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import com.demo.epaper.R;
import com.demo.epaper.activity.MainActivity;
import com.demo.epaper.adapter.BrushAdapter;
import com.demo.epaper.databinding.FragmentPaintBinding;
import com.demo.epaper.entity.BrushSelector;
import com.demo.epaper.handler.FilePickHandler;
import com.demo.epaper.handler.BluetoothListener;
import com.demo.epaper.handler.DialogClickListener;
import com.demo.epaper.imageio.ImageDither;
import com.demo.epaper.network.BleTransceiver;
import com.demo.epaper.ucrop.UCrop;
import com.demo.epaper.utils.AppUtils;
import com.demo.epaper.utils.NetConst;
import com.demo.epaper.view.BrushView;
import com.demo.epaper.view.ColorView;
import com.demo.epaper.view.InputDialog;
import com.demo.epaper.view.PixelView;
import com.demo.epaper.view.LineDecoration;
import com.demo.epaper.view.TransferDialog;
import com.demo.epaper.view.QToast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class PaintFragment extends Fragment {
    private Activity activity;
    private FragmentPaintBinding binding;

    private PixelView pixelView;
    private SeekBar seekBrush;
    private RecyclerView recyclerBrush;
    private List<BrushSelector> brushSelectors;
    private BrushAdapter brushAdapter;
    private int lastSelected;

    private InputDialog inputDialog;
    private TransferDialog transferDialog;

    private Button btnExport, btnImport;
    private Button btnPull, btnUpload;

    private LinearLayout binaryLayout;
    private RadioGroup binaryGroup;
    private ColorView lastColorView;
    private Button btnOriginal, btnDone;
    private boolean imageEditLock;
    private boolean textEditLock;

    private ImageDither dither;
    private int[] rawPixels;
    private int[] transformedPixels;
    private int imageWidth, imageHeight;
    private static final int IMAGE_ORIGINAL = 0;
    private static final int IMAGE_TRANSFORMED = 1;
    private int imageState;

    private BleTransceiver bleTransceiver;

    private static final int BRUSH_COLOR_POSITION = 0;
    private static final int BRUSH_TEXT_POSITION = 5;
    private static final int BRUSH_FILL_POSITION = 7;

    private byte[] frameBuffer;

    private int mtu;
    private AtomicInteger atomicOffset;
    private AtomicInteger atomicTrunk;
    private int firmwareVersion;
    private int actionTag;

    public static final int REQUEST_MIN = 300;
    public static final int REQUEST_MAX = 399;

    private static final int REQUEST_PICK_IMAGE = 300;
    private static final int REQUEST_CROP_IMAGE = 301;

    public PaintFragment() {
    }

    public PaintFragment(@NonNull Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPaintBinding.inflate(inflater, container, false);
        pixelView = binding.paintView;
        seekBrush = binding.seekBrushSize;
        recyclerBrush = binding.recyclerBrush;
        btnExport = binding.btnExport;
        btnImport = binding.btnImport;
        btnPull = binding.btnPull;
        binaryLayout = binding.imageLayout;
        btnOriginal = binding.btnOriginal;
        btnDone = binding.btnConfirm;
        btnUpload = binding.btnUpload;
        binaryGroup = binding.binaryRadioGroup;
        return binding.getRoot();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initView();
        initParam();

        brushAdapter.setItemClickListener((int type, int position) -> {
            if(imageEditLock) {
                showToast("请先完成图片操作", QToast.COLOR_ORANGE, false);
                return;
            }
            if(position == BRUSH_COLOR_POSITION) {
                int color = pixelView.nextPaintColor();
                brushSelectors.get(BRUSH_COLOR_POSITION).setDotColor(color);
                brushAdapter.notifyItemChanged(BRUSH_COLOR_POSITION, "UPDATE");
                return;
            }
            int count = Math.abs(position - lastSelected);
            if(count > 0) {
                if(lastSelected == BRUSH_TEXT_POSITION) {
                    textEditLock = false;
                    pixelView.paintTextDone();
                    brushSelectors.get(lastSelected).setType(BrushView.CIRCLE_TYPE_TEXT);
                }
                brushSelectors.get(lastSelected).setCheck(false);
                brushSelectors.get(position).setCheck(true);
                brushAdapter.notifyItemRangeChanged(Math.min(position, lastSelected), count + 1, "UPDATE");
                pixelView.setPaintType(brushSelectors.get(position).getType());
                lastSelected = position;
            }
            int action = brushSelectors.get(position).getType();
            if(action == BrushView.CIRCLE_TYPE_TEXT) {
                textEditLock = true;
                popupInputDialog();
                brushSelectors.get(position).setType(BrushView.CIRCLE_TYPE_DONE);
                brushAdapter.notifyItemChanged(position);

            }else if(action == BrushView.CIRCLE_TYPE_DONE) {
                textEditLock = false;
                pixelView.paintTextDone();
                brushSelectors.get(position).setType(BrushView.CIRCLE_TYPE_TEXT);
                brushAdapter.notifyItemChanged(position);

            }else if(action == BrushView.CIRCLE_TYPE_FILL) {
                pixelView.clear();

            }
        });

        seekBrush.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                pixelView.setPaintWidth(seekBar.getProgress() + 1);
            }
        });

        binaryGroup.setOnCheckedChangeListener((RadioGroup group, int checkedId) -> dispatchBinaryMethod(checkedId));

        bleTransceiver = BleTransceiver.getInstance();
        imageEditLock = false;
        imageState = IMAGE_ORIGINAL;
    }

    private final View.OnClickListener colorListener = (View view) -> {
        ColorView inView = (ColorView)view;
        if(lastColorView.equals(inView)) {
            return;
        }
        lastColorView.setChecked(false);

        lastColorView = inView;
        lastColorView.setChecked(true);
        int id = view.getId();

        if(id == R.id.color_view_bw) {
            dither.setColorFilter(ImageDither.COLOR_FILTER_BW);
        }else if(id == R.id.color_view_rw) {
            dither.setColorFilter(ImageDither.COLOR_FILTER_RW);
        }else if(id == R.id.color_view_bwr) {
            dither.setColorFilter(ImageDither.COLOR_FILTER_BWR);
        }

        id = binaryGroup.getCheckedRadioButtonId();
        if(id == -1) {
            showToast("请先选择二值化类型", QToast.COLOR_ORANGE, false);
            return;
        }
        dispatchBinaryMethod(id);
    };

    private void initView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity.getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerBrush.setLayoutManager(layoutManager);

        LineDecoration lineDecoration = new LineDecoration(activity.getApplicationContext(),
                LineDecoration.HORIZONTAL, 0, 1);
        recyclerBrush.addItemDecoration(lineDecoration);

        lastSelected = 1;
        brushSelectors = new ArrayList<>(8);
        brushSelectors.add(new BrushSelector(0xFFF1F1F1, 0xFF000000,
                BrushView.CIRCLE_TYPE_COLOR, false));
        brushSelectors.add(new BrushSelector(0xFFF1F1F1, 0xFF4E4F51,
                BrushView.CIRCLE_TYPE_DRAW, true));
        brushSelectors.add(new BrushSelector(0xFFF1F1F1, 0xFF4E4F51,
                BrushView.CIRCLE_TYPE_LINE, false));
        brushSelectors.add(new BrushSelector(0xFFF1F1F1, 0xFF4E4F51,
                BrushView.CIRCLE_TYPE_RECTANGLE, false));
        brushSelectors.add(new BrushSelector(0xFFF1F1F1, 0xFF4E4F51,
                BrushView.CIRCLE_TYPE_OVAL, false));
        brushSelectors.add(new BrushSelector(0xFFF1F1F1, 0xFF4E4F51,
                BrushView.CIRCLE_TYPE_TEXT, false));
        brushSelectors.add(new BrushSelector(0xFFF1F1F1, 0xFF4E4F51,
                BrushView.CIRCLE_TYPE_ERASE, false));
        brushSelectors.add(new BrushSelector(0xFFF1F1F1, 0xFF4E4F51,
                BrushView.CIRCLE_TYPE_FILL, false));

        brushAdapter = new BrushAdapter(brushSelectors);
        recyclerBrush.setAdapter(brushAdapter);

        btnPull.setOnClickListener(buttonListener);
        btnUpload.setOnClickListener(buttonListener);
        btnExport.setOnClickListener(buttonListener);
        btnImport.setOnClickListener(buttonListener);
        btnOriginal.setOnClickListener(buttonListener);
        btnDone.setOnClickListener(buttonListener);

        lastColorView = binding.colorViewBw;
        binding.colorViewBw.setChecked(true);
        binding.colorViewBw.setOnClickListener(colorListener);
        binding.colorViewRw.setOnClickListener(colorListener);
        binding.colorViewBwr.setOnClickListener(colorListener);
    }

    private void initParam() {
        atomicOffset = new AtomicInteger();
        atomicTrunk = new AtomicInteger();
        firmwareVersion = 0;
        actionTag = 0;
        frameBuffer = new byte[PixelView.EPD_RAM_SIZE];
        imageEditLock = false;
        textEditLock = false;
    }

    private static final int MSG_UPDATE_PROGRESS = 100;
    private static final int MSG_DISMISS_DIALOG = 101;
    private static final int MSG_SHOW_RAW_IMAGE = 102;
    private static final int MSG_SHOW_BINARY_IMAGE = 103;
    private static final int MSG_SHOW_RECEIVED_IMAGE = 104;
    private static final int MSG_GET_FIRMWARE_VERSION = 105;

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if(msg.what == MSG_UPDATE_PROGRESS) {
                transferDialog.setProgress(msg.arg1);

            }else if(msg.what == MSG_DISMISS_DIALOG) {
                transferDialog.setProgress(0);
                transferDialog.dismiss();

            }else if(msg.what == MSG_SHOW_RAW_IMAGE) {
                imageEditLock = true;
                imageState = IMAGE_ORIGINAL;
                binaryLayout.setVisibility(View.VISIBLE);
                pixelView.setPaintType(PixelView.BRUSH_IMAGE);
                pixelView.setPaintImage(rawPixels, imageWidth, imageHeight);

            }else if(msg.what == MSG_SHOW_BINARY_IMAGE) {
                pixelView.setPaintImage(transformedPixels, imageWidth, imageHeight);

            }else if(msg.what == MSG_SHOW_RECEIVED_IMAGE) {
                transferDialog.setProgress(0);
                transferDialog.dismiss();
                decodeFrameBuffer();

            }else if(msg.what == MSG_GET_FIRMWARE_VERSION) {
                if(msg.arg1 >= NetConst.REQUIRE_MINI_VERSION) {
                    firmwareVersion = msg.arg1;
                    if(actionTag == R.id.btn_upload) {
                        actionUpload();
                    }else if(actionTag == R.id.btn_pull) {
                        actionDownload();
                    }
                }else {
                    firmwareVersion = -1;
                    showToast("请升级新版固件", QToast.COLOR_ORANGE, false);
                }
            }
            return true;
        }
    });

    private static final String[] READ_WRITE_MESSAGE = new String[]{"写入成功", "偏移量超范围",
            "数据长度超范围", "包格式错误", "价签忙碌中"};
    private static final String[] FLUSH_MESSAGE = new String[]{"传输完成", "价签忙碌中", "价签刷新完成"};

    private final BluetoothListener paintBleListener = new BluetoothListener() {
        @Override
        public void onDataReceived(UUID uuid, byte[] data, int length) {
            byte opcode, status;

            if(NetConst.UPDATE_READ_UUID.equals(uuid)) {
                opcode = data[NetConst.OTA_RESP_OPCODE_IDX];
                status = data[NetConst.OTA_RESP_STATUS_IDX];

                if(status != NetConst.OTA_STATE_OK) {
                    return;
                }

                if(opcode == NetConst.OPCODE_GET_VERSION) {
                    Message message = handler.obtainMessage();
                    message.what = MSG_GET_FIRMWARE_VERSION;
                    message.arg1 = AppUtils.bytes2Int(data, 4, AppUtils.INT_BYTES);
                    handler.sendMessage(message);
                }

            }else if(NetConst.EPD_READ_UUID.equals(uuid)) {
                opcode = data[NetConst.APP_RESP_ACTION_IDX];
                status = data[NetConst.APP_RESP_STATUS_IDX];

                if(opcode == NetConst.ACTION_WRITE_RAM) {
                    if(status == NetConst.ACTION_RESULT_OK) {
                        uploadFrameBuffer();
                    }else {
                        handler.sendEmptyMessage(MSG_DISMISS_DIALOG);
                        showToast(READ_WRITE_MESSAGE[status], QToast.COLOR_ORANGE, true);
                    }

                }else if(opcode == NetConst.ACTION_FLUSH_DISPLAY) {
                    showToast(FLUSH_MESSAGE[status], QToast.COLOR_GREEN, true);

                }else if(opcode == NetConst.ACTION_READ_RAM) {
                    if (status == NetConst.ACTION_RESULT_OK) {
                        int offset = atomicOffset.get();
                        int trunk = atomicTrunk.get();

                        System.arraycopy(data, 2, frameBuffer, offset, trunk);
                        offset += trunk;

                        if (offset < PixelView.EPD_RAM_SIZE) {
                            int percent = ((offset * 100) / PixelView.EPD_RAM_SIZE);
                            Message message = handler.obtainMessage();
                            message.what = MSG_UPDATE_PROGRESS;
                            message.arg1 = percent;
                            handler.sendMessage(message);

                            downloadFrameBuffer(offset);
                        } else {
                            handler.sendEmptyMessage(MSG_SHOW_RECEIVED_IMAGE);
                        }
                    } else {
                        showToast(READ_WRITE_MESSAGE[status], QToast.COLOR_ORANGE, true);
                    }
                }
            }
        }

        @Override
        public void onStatusChanged(int type, int data) {
            if(type == BluetoothListener.BLE_DISCONNECTED) {
                firmwareVersion = 0;
                actionTag = 0;
            }
        }
    };

    private final FilePickHandler fileHandler = (int type, String param, int agr1) -> {
        if(type == FilePickHandler.APP_MSG_FOLDER_SELECTED) {
            File file = new File(param, System.currentTimeMillis() + ".png");
            boolean success = false;
            try {
                success = file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(!success) {
                showToast("创建文件失败", QToast.COLOR_GREEN, false);
                return;
            }
            Bitmap bmpRef = pixelView.getBitmap();
            try {
                FileOutputStream fos  = new FileOutputStream(file);
                bmpRef.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            showToast("保存成功", QToast.COLOR_GREEN, false);
        }
    };

    private final DialogClickListener clickListener = (int which, int tag, String str) -> {
        if(which == DialogClickListener.BUTTON_LEFT) {
            pixelView.setPaintType(PixelView.BRUSH_TEXT);
            pixelView.setPaintText(str);
        }
    };

    private void popupInputDialog() {
        if(inputDialog == null) {
            inputDialog = new InputDialog(activity);
            inputDialog.setTitle("放置文本");
            inputDialog.setMessage("在下方输入内容, 触摸画板区域放置文本");
            inputDialog.setDialogClickListener(clickListener);
        }
        inputDialog.setEditTextInput("");
        inputDialog.show();
    }

    private final View.OnClickListener buttonListener = (View view) -> {
        int id = view.getId();
        if(id == R.id.btn_confirm) {
            if(imageState == IMAGE_ORIGINAL) {
                showToast("请先选择二值化类型", QToast.COLOR_ORANGE, false);
            }else {
                binaryLayout.setVisibility(View.GONE);
                pixelView.paintImageDone();
                BrushSelector selector = brushSelectors.get(lastSelected);
                pixelView.setPaintType(selector.getType());
                imageEditLock = false;
                rawPixels = null;
                transformedPixels = null;
            }
            return;
        }else if(id == R.id.btn_original) {
            imageState = IMAGE_ORIGINAL;
            pixelView.setPaintImage(rawPixels, imageWidth, imageHeight);
            return;
        }
        if(textEditLock) {
            showToast("请先完成文本操作", QToast.COLOR_ORANGE, false);
            return;
        }
        if(imageEditLock) {
            showToast("请先完成图片操作", QToast.COLOR_ORANGE, false);
            return;
        }
        if(id == R.id.btn_export) {
            actionExportImage();
            return;
        }else if(id == R.id.btn_import) {
            actionImportImage();
            return;
        }
        if(isConnectionValid()) {
            if(firmwareVersion < 0) {
                showToast("请升级新版固件", QToast.COLOR_ORANGE, false);
            }else if(firmwareVersion < NetConst.REQUIRE_MINI_VERSION) {
                actionTag = id;
                actionGetVersion();
            }else {
                if(id == R.id.btn_upload) {
                    actionUpload();
                }else if(id == R.id.btn_pull) {
                    actionDownload();
                }
            }
        }
    };

    private void dispatchBinaryMethod(int checkedId) {
        if(checkedId == -1) {
            return;
        }
        imageState = IMAGE_TRANSFORMED;
        if(checkedId == R.id.radio_fuse) {
            dither.gaussianBinary(rawPixels, imageWidth, imageHeight, transformedPixels);

        }else if(checkedId == R.id.radio_bayer) {
            dither.bayerDither(rawPixels, imageWidth, imageHeight, transformedPixels);

        }else if(checkedId == R.id.radio_floyd) {
            dither.floydSteinbergDither(rawPixels, imageWidth, imageHeight, transformedPixels);

        }else if(checkedId == R.id.radio_gray) {
            dither.ostuBinary(rawPixels, imageWidth, imageHeight, transformedPixels);

        }
        handler.sendEmptyMessage(MSG_SHOW_BINARY_IMAGE);
    }

    private void actionGetVersion() {
        byte[] request = new byte[9];
        request[0] = NetConst.OPCODE_GET_VERSION;
        bleTransceiver.writeCharacteristic(NetConst.UPDATE_WRITE_UUID, request);
    }

    private void actionUpload() {
        if(transferDialog == null) {
            transferDialog = new TransferDialog(activity);
        }
        transferDialog.setTitle("上传中");
        transferDialog.show();

        mtu = (bleTransceiver.getMTU() - 3);
        atomicOffset.set(0);

        pixelView.getTransformedData(frameBuffer);

        uploadFrameBuffer();
    }

    private void uploadFrameBuffer() {
        int offset = atomicOffset.get();
        int trunk = Math.min((mtu - NetConst.WRITE_RAM_HEADER_SIZE), (PixelView.EPD_RAM_SIZE - offset));
        if(trunk == 0) {
            handler.sendEmptyMessage(MSG_DISMISS_DIALOG);
            flushEpaperDisplay();
            return;
        }
        int percent = ((offset * 100) / PixelView.EPD_RAM_SIZE);
        Message message = handler.obtainMessage();
        message.what = MSG_UPDATE_PROGRESS;
        message.arg1 = percent;
        handler.sendMessage(message);

        byte[] data = new byte[mtu];
        data[0] = NetConst.ACTION_WRITE_RAM;
        AppUtils.int2Bytes(data, 1, offset, 4);
        data[5] = (byte)trunk;
        System.arraycopy(frameBuffer, offset, data, 6, trunk);

        atomicOffset.set(offset + trunk);
        bleTransceiver.writeCharacteristic(NetConst.EPD_WRITE_UUID, data);
    }

    private void flushEpaperDisplay() {
        byte[] request = new byte[]{NetConst.ACTION_FLUSH_DISPLAY};
        bleTransceiver.writeCharacteristic(NetConst.EPD_WRITE_UUID, request);
    }

    private void actionImportImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    private void actionExportImage() {
        MainActivity main = (MainActivity)activity;
        main.showFileDialog("选择存储文件夹", MainActivity.FILE_DIALOG_FOLDER);
    }

    private void actionDownload() {
        if(transferDialog == null) {
            transferDialog = new TransferDialog(activity);
        }
        transferDialog.setTitle("接收中");
        transferDialog.show();

        mtu = (bleTransceiver.getMTU() - 3);
        System.out.println("mtu:" + mtu);
        downloadFrameBuffer(0);
    }

    private void downloadFrameBuffer(int offset) {
        final int requestMax = (mtu - NetConst.READ_RAM_RESP_HEADER_SIZE);

        int trunk = (PixelView.EPD_RAM_SIZE - offset);
        trunk = Math.min(trunk, requestMax);

        atomicTrunk.set(trunk);
        atomicOffset.set(offset);

        byte[] request = new byte[6];
        request[0] = NetConst.ACTION_READ_RAM;
        AppUtils.int2Bytes(request, 1, offset, 4);
        request[5] = (byte)(trunk & 0xFF);

        bleTransceiver.writeCharacteristic(NetConst.EPD_WRITE_UUID, request);
    }

    private static final int[] COLOR_LUT = new int[]{0xFF000000, 0xFFFFFFFF, 0, 0xFFFF0000};
    private void decodeFrameBuffer() {
        int y2, index, shift;
        byte px;
        int[] pixels = new int[PixelView.EPD_WIDTH * PixelView.EPD_HEIGHT];
        for(int y = 0; y < PixelView.EPD_HEIGHT; y++) {
            for(int x = 0; x < PixelView.EPD_WIDTH ; x++) {
                y2 = ((PixelView.EPD_WIDTH - 1) - x);
                index = (y2 * 32) + (y / 4);
                shift = ((y & 3) << 1);
                px =  (byte)((frameBuffer[index] >> shift) & 0x3);
                pixels[y * PixelView.EPD_WIDTH + x] = COLOR_LUT[px];
            }
        }
        pixelView.setPixels(pixels, PixelView.EPD_WIDTH, PixelView.EPD_HEIGHT);
    }

    private void showToast(String msg, int type, boolean b) {
        MainActivity main = (MainActivity)activity;
        main.showToast(msg, type, b);
    }

    private boolean isConnectionValid() {
        MainActivity host = (MainActivity)activity;
        if(!bleTransceiver.isBLEConnected()) {
            host.showToast("请先连接蓝牙", QToast.COLOR_ORANGE, false);
            return false;
        }
        if((!bleTransceiver.hasBLEFunction(NetConst.EPD_WRITE_UUID)) || (!bleTransceiver.hasBLEFunction(NetConst.UPDATE_WRITE_UUID))) {
            host.showToast("当前不支持该功能", QToast.COLOR_ORANGE, false);
            return false;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if((resultCode != Activity.RESULT_OK) || (data == null)) {
            return;
        }
        if(requestCode == REQUEST_PICK_IMAGE) {
            Uri src = data.getData();
            Uri dest = Uri.fromFile(new File(activity.getExternalCacheDir(), System.currentTimeMillis() + ".jpg"));
            UCrop.Options options = new UCrop.Options();
            options.setFreeStyleCropEnabled(true);
            options.withAspectRatio(2.049180F, 1.0F);
            options.withMaxResultSize(250, 122);
            UCrop.of(src, dest).withOptions(options).start(activity, REQUEST_CROP_IMAGE);

        }else if(requestCode == REQUEST_CROP_IMAGE) {
            Uri imageUri = UCrop.getOutput(data);
            if(imageUri == null) {
                return;
            }
            imageWidth = UCrop.getOutputImageWidth(data);
            imageHeight = UCrop.getOutputImageHeight(data);
            if(dither == null) {
                dither = new ImageDither();
                dither.setColorFilter(ImageDither.COLOR_FILTER_BW);
            }
            rawPixels = new int[imageWidth * imageHeight];
            transformedPixels = new int[imageWidth * imageHeight];
            Bitmap bitmap = BitmapFactory.decodeFile(imageUri.getPath());
            bitmap.getPixels(rawPixels, 0, imageWidth, 0, 0, imageWidth, imageHeight);
            bitmap.recycle();
            handler.sendEmptyMessage(MSG_SHOW_RAW_IMAGE);
        }
    }

    @Override
    public void onPause() {
        bleTransceiver.removeReceiveListener(paintBleListener);
        MainActivity main = (MainActivity)activity;
        main.setFilePickHandler(MainActivity.PAINT_PAGE, null);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        bleTransceiver.registerReceiveListener(paintBleListener);
        MainActivity main = (MainActivity)activity;
        main.setFilePickHandler(MainActivity.PAINT_PAGE, fileHandler);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if(hidden) {
            firmwareVersion = 0;
            actionTag = 0;
            bleTransceiver.removeReceiveListener(paintBleListener);
            MainActivity main = (MainActivity)activity;
            main.setFilePickHandler(MainActivity.PAINT_PAGE, null);
        }else {
            bleTransceiver.registerReceiveListener(paintBleListener);
            MainActivity main = (MainActivity)activity;
            main.setFilePickHandler(MainActivity.PAINT_PAGE, fileHandler);
        }
    }

    @Override
    public void onDestroy() {
        pixelView.recycle();
        pixelView = null;
        binding = null;
        activity = null;
        brushSelectors.clear();
        brushSelectors = null;
        brushAdapter.close();
        brushAdapter = null;
        dither = null;
        rawPixels = null;
        transformedPixels = null;
        frameBuffer = null;
        bleTransceiver = null;
        handler.removeCallbacksAndMessages(null);
        if((transferDialog != null) && (transferDialog.isShowing())) {
            transferDialog.dismiss();
        }
        transferDialog = null;
        atomicTrunk = null;
        atomicOffset = null;
        super.onDestroy();
    }
}