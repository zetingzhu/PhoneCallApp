package com.ajiew.phonecallapp;

import android.app.ActivityManager;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.ajiew.phonecallapp.listenphonecall.CallListenerService;
import com.ajiew.phonecallapp.phonecallui.PhoneCallService;

import java.lang.reflect.Field;

import ezy.assist.compat.SettingsCompat;

/**
 * author: aJIEw
 * description:
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = PhoneCallService.class.getSimpleName();

    private Switch switchPhoneCall;

    private Switch switchListenCall;
    private CompoundButton.OnCheckedChangeListener switchCallCheckChangeListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();


        RegisterBd();

        Intent callListener = new Intent(MainActivity.this, CallListenerService.class);
        startService(callListener);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Intent callListener = new Intent(MainActivity.this, CallListenerService.class);
        stopService(callListener);

        unRegisterBd() ;
    }



    private void initView() {
        switchPhoneCall = (Switch) findViewById(R.id.switch_default_phone_call);
        switchListenCall = (Switch) findViewById(R.id.switch_call_listenr);

        switchPhoneCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            // Android M 以上的系统发起将本应用设为默认电话应用的请求
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                if (switchPhoneCall.isChecked()) {
                    Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
                    intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                            getPackageName());
                    startActivity(intent);
                } else {
                    // 取消时跳转到默认设置页面
                    startActivity(new Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS"));
                }
            } else {
                Toast.makeText(MainActivity.this, "Android 6.0 以上才支持修改默认电话应用！", Toast.LENGTH_LONG).show();
                switchPhoneCall.setChecked(false);
            }

        }});

        // 使用使用 SettingsCompat 检查是否开启了权限
        switchCallCheckChangeListener = new CompoundButton.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            // 使用使用 SettingsCompat 检查是否开启了权限
            if (isChecked && !SettingsCompat.canDrawOverlays(MainActivity.this)) {
                askForDrawOverlay();
                switchListenCall.setOnCheckedChangeListener(null);
                switchListenCall.setChecked(false);
                switchListenCall.setOnCheckedChangeListener(switchCallCheckChangeListener);
                return;
            }

            Intent callListener = new Intent(MainActivity.this, CallListenerService.class);
            if (isChecked) {
                startService(callListener);
            } else {
                stopService(callListener);
            }
        }};
        switchListenCall.setOnCheckedChangeListener(switchCallCheckChangeListener);
    }

    private void askForDrawOverlay() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this)
                .setTitle("允许显示悬浮框")
                .setMessage("为了使电话监听服务正常工作，必须允许这项权限")
                .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    openDrawOverlaySettings();
                    dialog.dismiss();
                }})
                .setNegativeButton("稍后再说", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                      dialog.dismiss() ;
                    }
                }
                );

        alertDialog.show();
    }

    /**
     * 跳转悬浮窗管理设置界面
     */
    private void openDrawOverlaySettings() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M 以上引导用户去系统设置中打开允许悬浮窗
            // 使用反射是为了用尽可能少的代码保证在大部分机型上都可用
            try {
                Context context = this;
                Class clazz = Settings.class;
                Field field = clazz.getDeclaredField("ACTION_MANAGE_OVERLAY_PERMISSION");
                Intent intent = new Intent(field.get(null).toString());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "请在悬浮窗管理中打开权限", Toast.LENGTH_LONG).show();
            }
        } else {
            // 6.0 以下则直接使用 SettingsCompat 中提供的接口
            SettingsCompat.manageDrawOverlays(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.e(TAG , "== call  onResume >>>>>>>"  ) ;


        switchPhoneCall.setChecked(isDefaultPhoneCallApp());
        switchListenCall.setChecked(isServiceRunning(CallListenerService.class));
    }

    /**
     * Android M 以上检查是否是系统默认电话应用
     */
    public boolean isDefaultPhoneCallApp() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            TelecomManager manger = (TelecomManager) getSystemService(TELECOM_SERVICE);
            if (manger != null) {
                String name = manger.getDefaultDialerPackage();
                return name.equals(getPackageName());
            }
        }
        return false;
    }

    public boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    public void RegisterBd() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        filter.addAction(Intent.ACTION_CALL_BUTTON);
        filter.addAction(Intent.ACTION_VOICE_COMMAND);
        filter.addAction(Intent.ACTION_ANSWER);
        filter.addAction(Intent.ACTION_CALL);
        filter.addAction(Intent.ACTION_DIAL);
        filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        filter.addAction(Intent.EXTRA_TEMPLATE);
        filter.addAction(Intent.EXTRA_PHONE_NUMBER);
        registerReceiver(broadcastReceiver, filter);
    }

    private void unRegisterBd() {
        unregisterReceiver( broadcastReceiver );
    }


    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            Log.d(TAG , "广播" + intent.getAction());
        }
    };
}
