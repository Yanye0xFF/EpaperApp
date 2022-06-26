package com.demo.epaper.entity;

public class FileItem {

    private final String title;
    private final String path;
    private final int size;

    private final int viewType;
    public static final int TYPE_FOLDER = 0;
    public static final int TYPE_FILE = 1;
    public static final int TYPE_EMPTY = 2;
    public static final int TYPE_OPERATOR = 3;

    public FileItem(String title, String path, int viewType, int size) {
        this.title = title;
        this.path = path;
        this.viewType = viewType;
        this.size = size;
    }

    public String getTitle() {
        return title;
    }

    public int getViewType() {
        return viewType;
    }

    public String getPath() {
        return this.path;
    }

    public int getSize() {
        return this.size;
    }

}
