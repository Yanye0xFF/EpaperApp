package com.demo.epaper.handler;

public interface ThreadCallback {

    int FILE_SIZE_OUTOF_RANGE = 0;
    int READ_FILE_ERROR = 1;
    int FILE_SIGNATURE_ERROR = 2;

    void onStateChanged(int what);

    void onVerifyDone(byte[] data, int version);
}
