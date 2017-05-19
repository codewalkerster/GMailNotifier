package com.hardkernel.odroid.gmailnotifier;

import android.app.Notification;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by codewalker on 17. 5. 18.
 */

public class GMailNotifier extends NotificationListenerService {

    private final static String TAG = "GMailNotifier";

    private Process mProcess = null;
    private String mPWMEnableNode;
    private final String PWM_PREFIX = "/sys/devices/pwm-ctrl.";
    private final String PWM_ENABLE = "/enable0";
    private final String PWM_DUTY = "/duty0";
    private final String PWM_FREQ = "/freq0";
    private String mPWMDutyNode;
    private String mPWMFreqNode;

    private final static int UP = 0;
    private final static int DOWN = 1;
    private final static int RESET = 2;

    private int state = -1;
    private final static int CHECKING = 0;
    private final static int FINISHED = 1;

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Log.d(TAG, "msg.what = " + msg.what);
            if (msg.what == UP) {
                setDuty(110);
                sendEmptyMessageDelayed(DOWN, 5000);
            } else if (msg.what == DOWN) {
                setDuty(51);
                sendEmptyMessageDelayed(RESET, 1000);
            } else if (msg.what == RESET) {
                setDuty(0);
                state = FINISHED;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            if (mProcess == null)
                mProcess = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        for (int i = 0; i < 100; i++) {
            File f = new File(PWM_PREFIX + i);
            if (f.isDirectory()) {
                mPWMEnableNode = PWM_PREFIX + i + PWM_ENABLE;
                Log.e(TAG, "pwm enable : " + mPWMEnableNode);
                mPWMDutyNode = PWM_PREFIX + i + PWM_DUTY;
                Log.e(TAG, "pwm duty : " + mPWMDutyNode);
                mPWMFreqNode = PWM_PREFIX + i + PWM_FREQ;
                Log.e(TAG, "pwm freq : " + mPWMFreqNode);
                break;
            }
        }

        insmodPWM();
        setEnablePWM(true);
        setDuty(0);
        state = FINISHED;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        setEnablePWM(false);
        rmmodPWM();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        Notification mNotification = sbn.getNotification();

        if (sbn != null && sbn.getPackageName().equalsIgnoreCase("com.google.android.gm")){
            Log.e(TAG, "Gmail notification");
            if (state == FINISHED) {
                state = CHECKING;
                handler.sendEmptyMessage(UP);
            }
        }
    }

    private void insmodPWM() {
        try {
            DataOutputStream os = new DataOutputStream(mProcess.getOutputStream());
            os.writeBytes("insmod /system/lib/modules/pwm-meson.ko npwm=1\n");
            os.writeBytes("insmod /system/lib/modules/pwm-ctrl.ko\n");
            os.flush();
            Thread.sleep(100);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void rmmodPWM() {
        try {
            DataOutputStream os = new DataOutputStream(mProcess.getOutputStream());
            os.writeBytes("rmmod pwm_ctrl\n");
            os.writeBytes("rmmod pwm_meson\n");
            os.flush();
            Thread.sleep(100);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void setEnablePWM(boolean enable) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(mPWMEnableNode));
            if (enable)
                bw.write("1");
            else
                bw.write("0");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(mPWMFreqNode));
            bw.write("50");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setDuty(int duty) {
        Log.i(TAG, "setDuty(" + duty + ")");
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(mPWMDutyNode));
            bw.write(Integer.toString(duty));
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
