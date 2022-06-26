package com.demo.epaper.thread;

import com.demo.epaper.handler.ThreadCallback;
import com.demo.epaper.utils.AppUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ImageVerifyThread extends Thread {

    private static final int APP_MAX_SIZE = 81920;
    private static final int APP_VERSION_POS = 0x108;
    private static final int APP_SIGNATURE_POS = 0x167;
    private static final int APP_SIGNATURE = 0x52515152;

    private final ThreadCallback callback;
    private String filePath;

    public ImageVerifyThread(String path, ThreadCallback callback) {
        this.filePath = path;
        this.callback = callback;
    }

    @Override
    public void run() {
        File inFile = new File(filePath);
        int length = (int)inFile.length();
        if((length < (APP_SIGNATURE_POS + Integer.BYTES)) ||  (length >= APP_MAX_SIZE)) {
            callback.onStateChanged(ThreadCallback.FILE_SIZE_OUTOF_RANGE);
            return;
        }

        int readIn = 0;
        byte[] fileContent = new byte[length];
        try {
            FileInputStream fis = new FileInputStream(inFile);
            readIn = fis.read(fileContent, 0, length);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(readIn != length) {
            callback.onStateChanged(ThreadCallback.READ_FILE_ERROR);
            return;
        }

        int version = AppUtils.bytes2Int(fileContent, APP_VERSION_POS, AppUtils.INT_BYTES);
        int signature = AppUtils.bytes2Int(fileContent, APP_SIGNATURE_POS, AppUtils.INT_BYTES);
        if(APP_SIGNATURE == signature) {
            callback.onVerifyDone(fileContent, version);
        }else {
            callback.onStateChanged(ThreadCallback.FILE_SIGNATURE_ERROR);
        }
    }
}
