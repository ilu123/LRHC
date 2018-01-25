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
        final String ip = LrApplication.getIP()+"";
        final int port = 4200;
        if (mThread == null && ip != null) {
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Socket mSocket = null;
                    mSocket = new Socket();
                    while (!mStop) {
                        try {
                            if (!mSocket.isConnected()) {
                                mSocket.setReuseAddress(true);
                                mSocket.connect(new InetSocketAddress(ip, port));
                            }
                            byte[] buff = new byte[1024];
                            if ((mSocket.getInputStream().read(buff)) > 0) {
                                if (mHandler != null)
                                    mHandler.sendMessage(mHandler.obtainMessage(1, String.format("%s", new String(buff))));
                            }
                        } catch (Throwable e) {
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
        if (mAppMsg == null)
            mAppMsg = AppMsg.makeText(LrApplication.sApplication.sActivity, "", AppMsg.STYLE_INFO);
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    String m = ""+msg.obj ;
                    if (LrApplication.sApplication != null && LrApplication.sApplication.sActivity != null) {
                        if (mAppMsg == null) {
                            return;
                        }
                        if (mAppMsg.isShowing()) {
                            mAppMsg.setText(m);
                        }else {
                            mAppMsg.show();
                        }
                    }
                }
            }
        };
        super.onCreate();
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
