package com.demo.epaper.handler;

@FunctionalInterface
public interface ItemClickListener {
    void onItemClick(int type, int position);
}
