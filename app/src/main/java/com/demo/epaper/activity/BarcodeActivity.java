package com.demo.epaper.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;

import com.demo.epaper.databinding.ActivityBarcodeBinding;
import com.demo.epaper.view.ZXingScannerView;
import com.google.zxing.Result;

public class BarcodeActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler{

    private ZXingScannerView mScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityBarcodeBinding binding = ActivityBarcodeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewGroup contentFrame = binding.contentFrame;
        mScannerView = new ZXingScannerView(this);
        contentFrame.addView(mScannerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        mScannerView.stopCamera();
        super.onPause();
    }

    @Override
    public void handleResult(Result rawResult) {
        Intent result = new Intent();
        result.putExtra("Content", rawResult.getText());
        result.putExtra("Format", rawResult.getBarcodeFormat());
        setResult(Activity.RESULT_OK, result);
        finish();
    }
}