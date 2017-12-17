package com.lrkj.business;

import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import com.lrkj.LrApplication;
import com.lrkj.defines.LrDefines;
import com.lrkj.utils.LrToast;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by tianbao.zhao on 2017/12/13.
 */

public class LrRobot {
    static final int READ_TIMEOUT = 3000;

    public Socket mSocket;
    public String mIp;

    private static LrRobot sInstance = null;

    private LrRobot() {
    }

    public static LrRobot getRobot(String ip) {
        if (sInstance == null) {
            sInstance = new LrRobot();
        }
        sInstance.setRobotIp(ip);
        return sInstance;
    }

    public static void destroyRobot() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        if (sInstance != null) {
            if (sInstance.mSocket != null) {
                try {
                    sInstance.mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            sInstance.mSocket = null;
            sInstance.mIp = null;
        }

    }

    public boolean isConnected() {
        if (mSocket != null && mSocket.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public void setRobotIp(String ip) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        if (!(ip + "").equalsIgnoreCase(mIp) && ip != null) {
            mIp = ip;
            if (mSocket != null) {
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mSocket = null;
            try {
                mSocket = new Socket();
                mSocket.setReuseAddress(true);
                mSocket.setKeepAlive(true);
                mSocket.setSoTimeout(READ_TIMEOUT);
                mSocket.connect(new InetSocketAddress(ip, LrDefines.PORT_COMMANDS));
            } catch (IOException e) {
                e.printStackTrace();
                mIp = null;
                if (mSocket != null) {
                    try {
                        mSocket.close();
                    } catch (IOException ea) {
                        ea.printStackTrace();
                    }
                }
                mSocket = null;
            }
        }
    }

    public boolean sendCommand(int c, String more) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            if (!isConnected()) {
                destroyRobot();
                setRobotIp(mIp);
            }
            if (mSocket != null) {
                byte[] cmd = ByteBuffer.allocate(4).putInt(c).array();
                byte[] buff = new byte[1024];
                try {
                    mSocket.getOutputStream().write(cmd);
                    mSocket.getOutputStream().flush();
                    int len = 0;
                    if (c == LrDefines.Cmds.CMD_SLAM) {
                        mSocket.getOutputStream().write(more.getBytes("utf-8"));
                        mSocket.getOutputStream().flush();
                    } else if (c == LrDefines.Cmds.CMD_NAVI_START) {
                        mSocket.getOutputStream().write(more.getBytes("utf-8"));
                        mSocket.getOutputStream().flush();
                        destroyRobot();
                        return true;
                    }
                    if ((len = mSocket.getInputStream().read(buff)) == -1) {
                        return false;
                    }else {
                        LrToast.toast(String.format("ACK: %s", new String(buff)), LrApplication.sApplication);
                    }
                } catch (Throwable e) {
                    Log.e("", e + "");
                }
            } else {
                LrToast.toast("请连接设备", LrApplication.sApplication);
            }
        }catch (Throwable e) {

        }
        return true;
    }

    public static void sendCmd(String ip, int port, int c, String more) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Socket mSocket = null;
        try {
            mSocket = new Socket();
            mSocket.setReuseAddress(true);
            mSocket.setKeepAlive(true);
            mSocket.setSoTimeout(READ_TIMEOUT);
            mSocket.connect(new InetSocketAddress(ip, port));

            byte[] cmd = ByteBuffer.allocate(4).putInt(c).array();
            mSocket.getOutputStream().write(cmd);
            mSocket.getOutputStream().flush();
            int len = 0;
            if (c == 2019) { // del map
                byte[] nn = more.getBytes("utf-8");
                int nl = nn.length;
                mSocket.getOutputStream().write(ByteBuffer.allocate(4).putInt(nl).array());
                mSocket.getOutputStream().flush();
                mSocket.getOutputStream().write(nn);
                mSocket.getOutputStream().flush();
            }
            byte[] buff = new byte[1024];
            if ((len = mSocket.getInputStream().read(buff)) == -1) {
                return;
            }
            LrToast.toast(String.format("ACK: %s", new String(buff)), LrApplication.sApplication);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (mSocket != null) {
                try {
                    mSocket.close();
                } catch (IOException ea) {
                    ea.printStackTrace();
                }
            }
            mSocket = null;
        }
    }

}
