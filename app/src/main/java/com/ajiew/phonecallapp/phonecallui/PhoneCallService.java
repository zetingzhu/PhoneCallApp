package com.ajiew.phonecallapp.phonecallui;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

import com.ajiew.phonecallapp.ActivityStack;

/**
 * author: aJIEw
 * description: 监听电话通信状态的服务，实现该类的同时必须提供电话管理的UI
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class PhoneCallService extends InCallService {
    private static final String TAG = PhoneCallService.class.getSimpleName();

    private Call.Callback callback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            super.onStateChanged(call, state);
            Log.e(TAG , "== call  state >>>>>>>" + state ) ;
//          接听  call  state >>>>>>>4
//         挂断   call  state >>>>>>>10
//         挂断   call  state >>>>>>>7
            switch (state) {
                case Call.STATE_ACTIVE: {

                    break;
                }

                case Call.STATE_DISCONNECTED: {
                    ActivityStack.getInstance().finishActivity(PhoneCallActivity.class);
                    break;
                }

            }
        }
    };

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);

        Log.e(TAG , "== call  onCallAdded >>>>>>>"  ) ;
        call.registerCallback(callback);
        PhoneCallManager.call = call;

        CallType callType = null;

        if (call.getState() == Call.STATE_RINGING) {
            callType = CallType.CALL_IN;
        } else if (call.getState() == Call.STATE_CONNECTING) {
            callType = CallType.CALL_OUT;
        }

        if (callType != null) {
            Call.Details details = call.getDetails();
            String phoneNumber = details.getHandle().getSchemeSpecificPart();
            PhoneCallActivity.actionStart(this, phoneNumber, callType);
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);

        Log.e(TAG , "== call  onCallRemoved >>>>>>>"  ) ;
        call.unregisterCallback(callback);
        PhoneCallManager.call = null;
    }

    public enum CallType {
        CALL_IN,
        CALL_OUT,
    }
}
