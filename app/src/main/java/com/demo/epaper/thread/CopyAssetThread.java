package com.demo.epaper.thread;

import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.appcompat.app.AppCompatActivity;

public class CopyAssetThread extends Thread {

    private AppCompatActivity activity;

    private static final int BUFFER_SIZE = 8192;
    private static final String[] ASSET_PATH = new String[]{"db/", "font/", "icon/"};

    public CopyAssetThread(AppCompatActivity activity) {
        this.activity = activity;
    }

    @Override
    public void run() {
        boolean success;

        int readIn;
        byte[] buffer = new byte[BUFFER_SIZE];
        AssetManager assetManager = activity.getAssets();

        File out;
        FileOutputStream fos;
        InputStream inputStream;
        String[] filesName;

        try {
            for (String path : ASSET_PATH) {
                filesName = assetManager.list(path);
                if (filesName == null || filesName.length == 0) {
                    continue;
                }

                File parentDir = new File(activity.getExternalFilesDir(null) + "/" + path);
                success = parentDir.mkdirs();
                if (!success) {
                    break;
                }

                out = new File(parentDir, ".nomedia");
                if (!out.exists()) {
                    out.createNewFile();
                }

                for (String name : filesName) {
                    out = new File(parentDir, name);
                    if (out.exists()) {
                        continue;
                    }
                    out.createNewFile();
                    fos = new FileOutputStream(out);
                    inputStream = assetManager.open(path + name);

                    do {
                        readIn = inputStream.read(buffer, 0, BUFFER_SIZE);
                        fos.write(buffer, 0, readIn);
                    } while (readIn >= BUFFER_SIZE);

                    fos.flush();
                    fos.close();
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        activity = null;
    }
}
