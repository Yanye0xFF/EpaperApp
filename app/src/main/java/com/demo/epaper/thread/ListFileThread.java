package com.demo.epaper.thread;

import android.os.Handler;

import com.demo.epaper.activity.MainActivity;
import com.demo.epaper.entity.FileItem;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ListFileThread extends Thread {

    private String path;
    private List<FileItem> dest;
    private Handler handler;

    public ListFileThread(String path, List<FileItem> dest, Handler handler) {
        this.path = path;
        this.dest = dest;
        this.handler = handler;
    }

    @Override
    public void run() {
        File directory = new File(path);
        File[] files = directory.listFiles();
        int subDirs;
        File[] subFiles;

        if(files != null && files.length > 0) {

            List<File> fileList = Arrays.asList(files);
            Collections.sort(fileList, (File o1, File o2) -> {
                if(o1.isDirectory() && o2.isFile()) {
                    return -1;
                }
                if(o1.isFile() && o2.isDirectory()) {
                    return 1;
                }
                return o1.getName().compareTo(o2.getName());
            });

            for(File file : files) {
                if(file.getName().charAt(0) == '.') {
                    continue;
                }
                if(file.isDirectory()) {
                    subFiles = file.listFiles();
                    subDirs = (subFiles == null) ? 0 : subFiles.length;

                    dest.add(new FileItem(file.getName(), file.getAbsolutePath(), FileItem.TYPE_FOLDER,
                            subDirs));
                }else {
                    dest.add(new FileItem(file.getName(), file.getAbsolutePath(), FileItem.TYPE_FILE,
                            (int)file.length()));
                }
            }

            handler.sendEmptyMessageDelayed(dest.isEmpty() ? MainActivity.MSG_EMPTY_FOLDER : MainActivity.MSG_LOAD_FILE_SUCCESS, 200);

        }else {

            handler.sendEmptyMessageDelayed(MainActivity.MSG_EMPTY_FOLDER, 200);
        }
    }
}
