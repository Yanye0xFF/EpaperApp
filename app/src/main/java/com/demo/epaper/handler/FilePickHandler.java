package com.demo.epaper.handler;

@FunctionalInterface
public interface FilePickHandler {

    int APP_MSG_FILE_SELECTED = 0;
    int APP_MSG_FOLDER_SELECTED = 1;

    void onFileSelected(int type, String param, int agr1);
}
