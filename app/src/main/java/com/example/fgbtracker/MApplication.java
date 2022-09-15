package com.example.fgbtracker;

import android.app.Application;
import android.content.Context;

import com.secneo.sdk.Helper;

import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class MApplication extends Application {
    public static final String TAG = MApplication.class.getName();

    private static BaseProduct product;


    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
    }
    public static synchronized Aircraft getAircraftInstance() {
        if (!isAircraftConnected()) {
            return null;
        }
        return (Aircraft) getProductInstance();
    }
    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }
    public static synchronized BaseProduct getProductInstance() {
        product = DJISDKManager.getInstance().getProduct();
        return product;
    }
}