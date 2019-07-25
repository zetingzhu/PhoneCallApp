package com.ajiew.phonecallapp.util;

import android.app.Application;


/**
 * Created by zeting
 * Date 19/7/25.
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Utils.init(MyApplication.this) ;
    }
}
