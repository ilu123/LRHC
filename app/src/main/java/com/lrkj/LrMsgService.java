package com.lrkj;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.IntDef;
import android.util.Log;
import android.view.WindowManager;

import com.devspark.appmsg.AppMsg;
import com.lrkj.defines.LrDefines;
import com.lrkj.utils.LrToast;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by tianbao.zhao on 2018/01/25.
 */

public class LrMsgService extends Service {
    Thread mThread = null;
    Handler mHandler = null;
    volatile boolean mStop = false;
    AppMsg mAppMsg = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String ip = LrApplication.getIP() + "";
        final int port = 4200;
        if (mThread == null && ip != null) {
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Socket mSocket = null;
                    mSocket = new Socket();
                    byte[] buff = new byte[1024];
                    while (!mStop) {
                        try {
                            if (!mSocket.isConnected()) {
                                mSocket.setReuseAddress(true);
                                mSocket.connect(new InetSocketAddress(ip, port));
                            }
                            int len = 0;
                            Arrays.fill(buff, (byte)'\0');
                            if ((len = mSocket.getInputStream().read(buff)) > 0) {
                                if (mHandler != null)
                                    mHandler.sendMessage(mHandler.obtainMessage(1, new String(buff)));
                            }
                        } catch (Throwable e) {
                            if (mHandler != null)
                                mHandler.sendMessage(mHandler.obtainMessage(1, e.getMessage()));
                        }
                    }
                    if (mSocket != null) {
                        try {
                            mSocket.close();
                            mSocket = null;
                        } catch (Throwable e) {
                        }
                    }
                }
            });
            mThread.start();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (mAppMsg == null)
            mAppMsg = AppMsg.makeText(LrApplication.sApplication.sActivity, "", AppMsg.STYLE_INFO);
        if (mHandler == null)
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == 1) {
                        String m = "" + msg.obj;
                        if (LrApplication.sApplication != null && LrApplication.sApplication.sActivity != null) {
                            if (mAppMsg == null) {
                                return;
                            } else {
                                if (mAppMsg.getActivity() != LrApplication.sApplication.sActivity) {
                                    mAppMsg.cancel();
                                    mAppMsg = AppMsg.makeText(LrApplication.sApplication.sActivity, m, AppMsg.STYLE_INFO);
                                }
                            }
                            if (mAppMsg.isShowing()) {
                                mAppMsg.setText(m);
                            } else {
                                mAppMsg.show();
                            }
                        }
                    }
                }
            };
    }

    @Override
    public void onDestroy() {
        mStop = true;
        if (mThread != null) {
            mThread = null;
        }
        if (mAppMsg != null)
            mAppMsg.cancel();
        mAppMsg = null;
        super.onDestroy();
    }

}
