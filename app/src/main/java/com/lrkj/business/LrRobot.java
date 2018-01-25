package com.lrkj.business;

import android.os.Looper;
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
import java.nio.ByteOrder;

/**
 * Created by tianbao.zhao on 2017/12/13.
 */

public class LrRobot {
    static final int READ_TIMEOUT = 5000;

    public Socket mSocket;
    public String mIp;

    private static LrRobot sInstance = null;

    private LrRobot() {
    }


    public boolean isConnected() {
        if (mSocket != null && mSocket.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean connectRobot(String ip) {
        if (isInMainThread()) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        boolean ok = false;
        if (ip != null) {
            Socket mSocket = null;
            try {
                mSocket = new Socket();
                mSocket.setReuseAddress(true);
                mSocket.setSoTimeout(READ_TIMEOUT);
                mSocket.connect(new InetSocketAddress(ip, LrDefines.PORT_COMMANDS));
                ok = true;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (mSocket != null) {
                    try {
                        try {
                            mSocket.shutdownInput();
                        }catch (Throwable e) {}
                        try {
                            mSocket.shutdownOutput();
                        }catch (Throwable e) {}
                        mSocket.close();
                    } catch (IOException ea) {
                        ea.printStackTrace();
                    }
                }
                mSocket = null;
                System.gc();
            }
        }
        return ok;
    }
    public static boolean isInMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
    public static boolean sendCmd(String ip, int port, int c, String more) {
        if (ip == null)
            return false;
        if (isInMainThread()) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        Socket mSocket = null;
        boolean ok = false;
        try {
            mSocket = new Socket();
            mSocket.setReuseAddress(true);
            mSocket.setSoTimeout(READ_TIMEOUT);
            mSocket.connect(new InetSocketAddress(ip, port));

            byte[] cmd = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(c).array();
            mSocket.getOutputStream().write(cmd);
            mSocket.getOutputStream().flush();
            int len = 0;
            if (c == LrDefines.Cmds.CMD_DEL_ALL_MAP) { // del map
                if (more != null) {
                    byte[] nn = more.getBytes("utf-8");
                    int nl = nn.length;
                    mSocket.getOutputStream().write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(nl).array());
                    mSocket.getOutputStream().flush();
                    mSocket.getOutputStream().write(nn);
                    mSocket.getOutputStream().flush();
                }
            }else if (c == LrDefines.Cmds.CMD_SLAM) {
                if (more != null) {
                    mSocket.getOutputStream().write(more.getBytes("utf-8"));
                    mSocket.getOutputStream().flush();
                }
            } else if (c == LrDefines.Cmds.CMD_NAVI_START) {
                if (more != null) {
                    mSocket.getOutputStream().write(more.getBytes("utf-8"));
                    mSocket.getOutputStream().flush();
                }
            }
            if (c == 1078
                    || c == 1079
                    || c == 1000
                    || c == 1001
                    || c == 1002
                    || c == 1003
                    || c == 1099) {
                byte[] buff = new byte[1024];
                if ((len = mSocket.getInputStream().read(buff)) == -1) {
                    ok = false;
                } else {
                    LrToast.toast(String.format("ACK: %s", new String(buff)));
                    ok = true;
                }
            }else {
                ok = true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            ok = false;
            LrToast.toast("命令发送失败！");
        } finally {
            while (mSocket != null || mSocket.isConnected()) {
                if (mSocket != null) {
                    try {
                        try {
                            mSocket.shutdownOutput();
                        }catch (Throwable e) {}
                        try {
                            mSocket.shutdownInput();
                        }catch (Throwable e) {}
                        mSocket.close();
                        mSocket = null;
                    } catch (IOException ea) {
                        ea.printStackTrace();
                    }
                }
            }
            mSocket = null;
            System.gc();
        }
        return ok;
    }

    public static boolean sendCommand(String mRobotIp, int cmdStopMission, String o) {
        return sendCmd(mRobotIp, LrDefines.PORT_COMMANDS, cmdStopMission, o);
    }
}
