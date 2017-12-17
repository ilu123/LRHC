package com.lrkj.defines;

/**
 * Created by tianbao.zhao on 2017/12/13.
 */

public class LrDefines {
    public static final int PORT_MAPS = 4101;
    public static final int PORT_COMMANDS = 4103;
    public static final int PORT_NAVIGATION = 4104;
    public static final int PORT_READ_VIDEO = 4096;
    public static final int PORT_DOT = 4097;
    public static final int PORT_READ_LASER = 4100;

    public static final class Cmds {
        public static final int CMD_MAP_RESET = 1;
        public static final int CMD_MAP_FINISH = 2;
        public static final int CMD_MAP_SAVE = 5;

        public static final int CMD_BRING_UP = 999;
        public static final int CMD_SHUTDOWN = 1000;
        public static final int CMD_REBOOT = 1001;
        public static final int CMD_STOP_MISSION = 1002;
        public static final int CMD_SLAM = 1003;
        public static final int CMD_GET_MAP = 1006;
        public static final int CMD_NAVI_START = 1013;
        public static final int CMD_NAVI_STOP = 1077;

    }

    public static final String COLOR_MAIN_THEME = "#00bcd4";
    public static final String COLOR_IP_DIALOG_THEME = "#d32f2f";



    public static String bytesToHexStr(byte[] bytes, int len) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[len*2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
