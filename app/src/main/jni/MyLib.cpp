//
// Created by Administrator on 2016/12/13.
//

#include <iostream>
#include <sstream>
#include <jni.h>
#include <opencv2/opencv.hpp>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <vector>
#include <string.h>
#include <android/log.h>

#define LOG_TAG "LOG_JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

using std::string;

#include "com_lrkj_business_LrNativeApi.h"

//---------------------------- Utils Methods [Begin]------------------------------------//
/*!
 @brief jstring to java String.
 @author  ztb
*/
static char *jstringToChars(JNIEnv *env, jstring jstr) {
    char *rtn = NULL;
    char *b = "utf-8";
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF(b);
    jmethodID mid = env->GetMethodID(clsstring, "getBytes",
                                     "(Ljava/lang/String;)[B");
    jbyteArray barr = (jbyteArray) env->CallObjectMethod(jstr, mid, strencode);
    jsize alen = env->GetArrayLength(barr);
    jbyte *ba = env->GetByteArrayElements(barr, JNI_FALSE);
    if (alen > 0) {
        rtn = (char *) malloc(alen + 1);
        memcpy(rtn, ba, alen);
        rtn[alen] = 0;
    }
    env->ReleaseByteArrayElements(barr, ba, 0);
    env->DeleteLocalRef(clsstring);
    env->DeleteLocalRef(strencode);
    return rtn;
}

static //将char类型转换成jstring类型
jstring char2jstring(JNIEnv *env, const char *str) {
    jsize len = strlen(str);
    jclass strClass = (env)->FindClass("java/lang/String");
    const char *b = "utf-8";
    jstring encoding = (env)->NewStringUTF(b);
    jmethodID ctorID = (env)->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
    jbyteArray bytes = (env)->NewByteArray(len);
    (env)->SetByteArrayRegion(bytes, 0, len, (jbyte *) str);
    jstring res = (jstring) (env)->NewObject(strClass, ctorID, bytes, encoding);

    env->ReleaseByteArrayElements(bytes, (jbyte *) str, 0);
    env->DeleteLocalRef(strClass);
    env->DeleteLocalRef(encoding);
    env->DeleteLocalRef(bytes);
    return res;
}

static void closeSocket(int s) {
    if (s >= 0)
        close(s);
}

/***********[LR]*****************************/
static char *ServerIP = NULL;
static int Port_Dot = 4097;  // 该端口用来显示点云
static int Port_Laser = 4100;
static int SocketDot = -1;
static int SocketLaser = -1;
static bool StopSocketDot = false;
static bool StopSocketLaser = false;
static bool Save_Laser = false;
static bool Reset_Laser = false;

static int createSocket(const char *ip, int port) {
    if (ip == NULL) {
        return -1;
    }
    int skt = -1;
    if ((skt = socket(PF_INET, SOCK_STREAM, 0)) < 0) {
        return -1;
    }

    /* Configure our timeout to be 5 seconds */
    struct timeval timeouta = {8, 0};
    setsockopt(skt, SOL_SOCKET, SO_SNDTIMEO, &timeouta, sizeof(timeouta));
    setsockopt(skt, SOL_SOCKET, SO_RCVTIMEO, &timeouta, sizeof(timeouta));

    struct sockaddr_in serverAddr;
    socklen_t addrLen = sizeof(struct sockaddr_in);
    serverAddr.sin_family = PF_INET;
    serverAddr.sin_addr.s_addr = inet_addr(ip);
    serverAddr.sin_port = htons(port);

    if (connect(skt, (sockaddr *) &serverAddr, addrLen) < 0) {
        closeSocket(skt);
        return -1;
    }
    return skt;
}

static bool sendSignal(int sokt, int signal) {
    int bytes = 0;
    if ((bytes = send(sokt, &signal, 4, 0)) == -1) {
        return false;
    }
    return true;
}

static int recvSignal(int sokt) {
    int bytes = 0;
    int signal = -1;
    if ((bytes = recv(sokt, &signal, 4, 0)) <= 0) {
        return -1;
    }
    return signal;
}

static float recvFloat(int sokt) {
    int bytes = 0;
    float signal = -1000000;
    if ((bytes = recv(sokt, &signal, sizeof(signal), 0)) <= 0) {
    }
    return signal;
}

static bool printAck(int sokt) {
    int bytes = 0;
    char resuBuff[1024];
    if ((bytes = recv(sokt, resuBuff, 1024, 0)) == -1) {
        return false;
    }
    return true;
}

JNIEXPORT void JNICALL Java_com_lrkj_business_LrNativeApi_setRobotIp
        (JNIEnv *env, jclass thiz, jstring ip) {
    ServerIP = jstringToChars(env, ip);
}

JNIEXPORT jboolean JNICALL Java_com_lrkj_utils_LrSocketSurfaceView_getDotFrame
        (JNIEnv *env, jclass clazz, jobject obj, jlong ioMat) {
    if (ServerIP == NULL)
        return false;

    StopSocketDot = false;

    int64_t MAX_VEC_SIZE = 10000000; //最大不超过的点数

    if (SocketDot < 0) {
        if ((SocketDot = createSocket(ServerIP, Port_Dot)) < 0) {
            return false;
        }
    }

    jmethodID mid = env->GetMethodID(clazz, "postDotFrameFromNative", "()V");
    jmethodID mid2 = env->GetMethodID(clazz, "setSlamState", "(I)V");

    std::vector<std::pair<int, int> > vt;
    vt.push_back(std::make_pair(1, 1));

    int bytes = 0;

    cv::Mat img = (*((cv::Mat *) ioMat));
    img.setTo(cv::Scalar(255, 255, 255));

    int reconnect = 0;
    while (!StopSocketDot) {
        if (reconnect) { //因为错误等原因 断开链接后，需要自动重新连接
            close(SocketDot);
            if ((SocketDot = createSocket(ServerIP, Port_Dot)) < 0) {
                env->CallVoidMethod(obj, mid2, -1);
                continue;
            }
        }

        int state = -1;

        //接收slam state
        if ((bytes = recv(SocketDot, &state, sizeof(state), MSG_WAITALL)) == -1) {
            env->CallVoidMethod(obj, mid2, -1);
            reconnect = 1;
            continue;
        }

        env->CallVoidMethod(obj, mid2, state);


        try {
            std::vector<std::pair<int, int> > vec1;  // black pointcloud 黑色点云vector
            std::vector<std::pair<int, int> > vec2;  // current pose   轨迹vector

            int64_t sz1;  // vector的长度容器
            int64_t sz2;  // vector的长度容器

            //接收尺寸
            if ((bytes = recv(SocketDot, &sz1, sizeof(sz1), MSG_WAITALL)) == -1) {
                std::cerr << "recv failed, received bytes = " << bytes << std::endl;
                reconnect = 1;
                continue;
            }

            //更改vector尺寸
            if (sz1 > MAX_VEC_SIZE) sz1 = MAX_VEC_SIZE;
            try {
                vec1.resize(sz1);
            } catch (const std::exception &e) {
                close(SocketDot);
                reconnect = 1;
                continue;

            }

            //接收黑色点云
            if ((bytes = recv(SocketDot, &(*vec1.begin()), sz1 * sizeof(vt[0]), MSG_WAITALL)) ==
                -1) {
                std::cerr << "recv failed, received bytes = " << bytes << std::endl;
                reconnect = 1;
                continue;
            }

            if (reconnect == 1)
                env->CallVoidMethod(obj, mid2, 0xd0);

            reconnect = 0;

            int pointCnt = vec1[0].first;
            string str = " map points number : " + cv::format("%d", pointCnt);
            putText(img, str, cv::Point(100, 100), cv::FONT_HERSHEY_PLAIN, 2,
                    cv::Scalar(0, 0, 255, 255));

            //接收尺寸
            if ((bytes = recv(SocketDot, &sz2, sizeof(sz2), MSG_WAITALL)) == -1) {
                std::cerr << "recv failed, received bytes = " << bytes << std::endl;
            }

            //更改vector尺寸
            if (sz2 > MAX_VEC_SIZE) sz2 = MAX_VEC_SIZE;
            try {
                vec2.resize(sz2);
            } catch (const std::exception &e) {
                close(SocketDot);
                reconnect = 1;
                continue;
            }

            //接收轨迹点云
            if ((bytes = recv(SocketDot, &(*vec2.begin()), sz2 * 8, MSG_WAITALL)) == -1) {
                reconnect = 1;
                continue;
            }

            //画黑色点
            for (int i = 0; i < vec1.size(); i++) {
                cv::circle(img, cv::Point(vec1[i].first, vec1[i].second), 1, cv::Scalar(0, 0, 0),
                           -1);
            }
            //画轨迹
            for (int i = 0; i < vec2.size(); i++) {
                cv::circle(img, cv::Point(vec2[i].first, vec2[i].second), 4, cv::Scalar(0, 255, 0),
                           1,
                           CV_AA);
            }

            cv::circle(img, cv::Point(vec2[0].first, vec2[0].second), 6, cv::Scalar(255, 0, 0), -1,
                       CV_AA);

            //mapsave
            bool mapsave = false;
            if ((bytes = recv(SocketDot, &mapsave, sizeof(mapsave), MSG_WAITALL)) == -1) {
                std::cerr << "recv failed, received bytes = " << bytes << std::endl;
                continue;
            }
            if (mapsave) {
                std::cout << "map save finish !!!!!!!" << std::endl;
                env->CallVoidMethod(obj, mid2, 0xc5);
            }

            // Call Java mid to draw
            env->CallVoidMethod(obj, mid);

            img.setTo(cv::Scalar(255, 255, 255));
        } catch (const std::exception &e) {
            continue;
        }
    }
    Java_com_lrkj_utils_LrSocketSurfaceView_stopDotSocket(NULL, NULL);
    return false;
}


JNIEXPORT void JNICALL Java_com_lrkj_utils_LrSocketSurfaceView_stopDotSocket
        (JNIEnv *, jclass) {
    StopSocketDot = true;
    if (SocketDot >= 0) {
        close(SocketDot);
    }
    SocketDot = -1;
}

// Laser
JNIEXPORT void JNICALL Java_com_lrkj_utils_LrSocketSurfaceView_saveLaserFrame
        (JNIEnv *env, jclass clazz) {
    Save_Laser = true;
}
JNIEXPORT void JNICALL Java_com_lrkj_utils_LrSocketSurfaceView_resetLaserFrame
        (JNIEnv *env, jclass clazz) {
    Reset_Laser = true;
}

JNIEXPORT jboolean JNICALL Java_com_lrkj_utils_LrSocketSurfaceView_getLaserFrame
        (JNIEnv *env, jclass clazz, jobject obj, jlong ioMat, jstring mappgm) {
    Save_Laser = false;

    if (ServerIP == NULL)
        return false;

    char *mapName = jstringToChars(env, mappgm);

    StopSocketLaser = false;

    int64_t MAX_VEC_SIZE = 10000000; //最大不超过的点数

    if (SocketLaser < 0) {
        if ((SocketLaser = createSocket(ServerIP, Port_Laser)) < 0) {
            return 0;
        }
    }

    jmethodID mid = env->GetMethodID(clazz, "postLaserFrameFromNative", "()V");
    jmethodID mid2 = env->GetMethodID(clazz, "saveLaserFrameFromNativeDone", "()V");

    // 获取点云地图
    cv::Mat pc;
    float orix, oriy;
    int pw, ph;

    int SocketGetMap = createSocket(ServerIP, 4101);
    if (SocketGetMap < 0)
        return 0;
    if (sendSignal(SocketGetMap, 2016)) {
        bool mapExist = false;
        int bytes = -1;

        // send the size of  mapName ; 发送地图名称在所占字节个数
        int mapNameLen = strlen(mapName);
        if ((bytes = send(SocketGetMap, &mapNameLen, sizeof(mapNameLen), 0)) <= 0) {
            return 0;
        }

        // send the name of the map  发送地图名称
        if ((bytes = send(SocketGetMap, mapName, mapNameLen, 0)) <= 0) {
            return 0;
        }

        //recieve ack information
        //获取反馈信息
        if ((bytes = recv(SocketGetMap, &mapExist, sizeof(mapExist), 0)) <= 0) {
            std::cerr << "bytes = " << bytes << std::endl;
            return 0;
        }


        if (mapExist) {
            //获取地图宽度
            int mapWidth = 0;    // width of the map
            if ((bytes = recv(SocketGetMap, &mapWidth, sizeof(mapWidth), MSG_WAITALL)) <= 0) {
                std::cerr << "bytes = " << bytes << std::endl;
                return 0;
            }

            //获取地图高度
            int mapHeight = 0;    // height of the map
            if ((bytes = recv(SocketGetMap, &mapHeight, sizeof(mapHeight), MSG_WAITALL)) <= 0) {
                return 0;
            }

            //制作地图容器
            cv::Mat img;  // image container
            img = cv::Mat::zeros(mapHeight, mapWidth,
                                 CV_8UC1);    // CV_8UC1 means that each pixels in the image is type unsigned char
            int imgSize = img.total() * img.elemSize();
            uchar *iptr = img.data;

            //获取地图数据本身
            if ((bytes = recv(SocketGetMap, iptr, imgSize, MSG_WAITALL)) <= 0) {
                return 0;
            }

            pw = mapWidth;
            ph = mapHeight;
            pc = img;
        }
    }else{
        return 0;
    }

    cv::Mat white = cv::Mat(ph, pw, CV_8UC1, cv::Scalar(255));
    pc = white - pc; //图像反向， 黑->白 ， 白->黑


    int bytes = 0;
    int mapWidth = 0;    // width of the map
    int mapHeight = 0;    // height of the map

    //获取地图宽度
    if ((bytes = recv(SocketLaser, &mapWidth, sizeof(mapWidth), MSG_WAITALL)) <= 0) {
        std::cerr << "bytes = " << bytes << std::endl;
        Java_com_lrkj_utils_LrSocketSurfaceView_stopLaserSocket(NULL, NULL);
        return 0;

    }

    //获取地图高度
    if ((bytes = recv(SocketLaser, &mapHeight, sizeof(mapHeight), MSG_WAITALL)) <= 0) {
        std::cerr << "bytes = " << bytes << std::endl;
        Java_com_lrkj_utils_LrSocketSurfaceView_stopLaserSocket(NULL, NULL);
        return 0;
    }

    cv::Mat lasermap = cv::Mat(mapHeight, mapWidth, CV_8UC1, cv::Scalar(0));
    cv::Mat pose = cv::Mat(mapHeight, mapWidth, CV_8UC1, cv::Scalar(0));
    cv::Mat trajectory = cv::Mat(mapHeight, mapWidth, CV_8UC1, cv::Scalar(0));
    if (!(pw == mapWidth && ph == mapHeight)) { //地图尺寸不一样的，有可能是特征点地图输入错误，退出程序
        return 0;
    }
    cv::Mat covisibleMap = cv::Mat(mapHeight, mapWidth, CV_8UC1,
                                   cv::Scalar(0));   //共试图，用于保存特征点和激光同时能看到的静态障碍
    cv::Mat laserPoint = cv::Mat(mapHeight, mapWidth, CV_8UC1, cv::Scalar(0));    // 用于画每针看到的激光点

    // java map init
    cv::Mat img = (*((cv::Mat *) ioMat));
    cv::Size ResImgSiz = cv::Size(img.cols, img.rows);

    while (!StopSocketLaser) {
        //获取x坐标
        int x_cam = 0;    // height of the map
        if ((bytes = recv(SocketLaser, &x_cam, sizeof(x_cam), MSG_WAITALL)) <= 0) {
            std::cerr << "bytes = " << bytes << std::endl;
            Java_com_lrkj_utils_LrSocketSurfaceView_stopLaserSocket(NULL, NULL);
            return 0;
        }

        //获取y坐标
        int y_cam = 0;    // height of the map
        if ((bytes = recv(SocketLaser, &y_cam, sizeof(y_cam), MSG_WAITALL)) <= 0) {
            std::cerr << "bytes = " << bytes << std::endl;
            Java_com_lrkj_utils_LrSocketSurfaceView_stopLaserSocket(NULL, NULL);
            return 0;
        }

        std::vector<std::pair<int, int> > laser_dots;
        int sz = 0;

        //获取点云的尺寸
        if ((bytes = recv(SocketLaser, &sz, sizeof(sz), MSG_WAITALL)) == -1) {
            std::cerr << "recv failed, received bytes = " << bytes << std::endl;
        }

        //获取点云
        if (sz > MAX_VEC_SIZE) sz = MAX_VEC_SIZE;
        laser_dots.resize(sz);
        if ((bytes = recv(SocketLaser, &(*laser_dots.begin()), sz * 8, MSG_WAITALL)) == -1) {
            std::cerr << "recv failed, received bytes = " << bytes << std::endl;
        }
        laserPoint.setTo(cv::Scalar(0));

        for (int i = 0; i < laser_dots.size(); i++) {
            int x_p = laser_dots[i].first;
            int y_p = laser_dots[i].second;

            if (x_p >= 0 && y_p >= 0 && x_p < lasermap.cols && y_p < lasermap.rows) {
                //lasermap 画布 画激光线
                cv::line(lasermap, cv::Point(x_cam, y_cam), cv::Point(x_p, y_p), cv::Scalar(255));
                lasermap.at<uchar>(y_p, x_p) = 0;

                //laserdot画布 画激光点
                cv::circle(laserPoint, cv::Point(x_p, y_p), 2, cv::Scalar(255), -1);
                lasermap.at<uchar>(y_p, x_p) = 0;
            }
        }

        cv::Mat res;
        cv::bitwise_and(laserPoint, pc, res); // laserpoint 图层和 pointcloud 图层 按位与运算。
        covisibleMap = covisibleMap + res; // 结果添加到共视图上

        //当前位置图层
        cv::circle(pose, cv::Point(x_cam, y_cam), 4, cv::Scalar(255), -1);
        //轨迹图层
        cv::circle(trajectory, cv::Point(x_cam, y_cam), 8, cv::Scalar(175), -1);

        //最终地图图层
        cv::Mat finalMap;

        //将轨迹经过的位置都涂成可通行区域
        cv::bitwise_or(lasermap - covisibleMap, trajectory * 10, finalMap);

        cv::resize(finalMap - trajectory - pose, img, ResImgSiz);

        if (Save_Laser) {
            //保存到本地
            cv::imwrite(string("/mnt/sdcard/com.lrkj.ctrl/maps/") + string(mapName) + ".pgm",
                        finalMap);
            cv::imwrite(string("/mnt/sdcard/com.lrkj.ctrl/maps/") + string(mapName) + ".jpg",
                        img);
            std::cout << "save map done!!!!" << std::endl;
            Save_Laser = false;

            env->CallVoidMethod(obj, mid2);
        }

        if (Reset_Laser) {
            lasermap.setTo ( cv::Scalar ( 0)  ) ;
            trajectory.setTo ( cv::Scalar ( 0)  ) ;
            Reset_Laser = false;
        }

        // Call Java mid to draw
        env->CallVoidMethod(obj, mid);
    }
    Java_com_lrkj_utils_LrSocketSurfaceView_stopLaserSocket(NULL, NULL);
    return false;
}


JNIEXPORT void JNICALL Java_com_lrkj_utils_LrSocketSurfaceView_stopLaserSocket
        (JNIEnv *, jclass) {
    StopSocketLaser = true;
    if (SocketLaser >= 0) {
        close(SocketLaser);
    }
    SocketLaser = -1;
}


// Ge all maps

JNIEXPORT jint JNICALL Java_com_lrkj_business_LrNativeApi_getAllMaps
        (JNIEnv *env, jclass clazz, jint mode) {
    int sokt = -1;
    try {
        sokt = createSocket(ServerIP, 4101);
        int bytes = 0;

        int signal = 2017;

        /*
        signal meaning:
        2017 : retrive all maps from SLAM borad  获取所有地图
        2018  : send an edited map to SLAM board and replace the original one  发送一张修改过的地图给机器人
        */

        // 发送 信号
        if ((bytes = send(sokt, &signal, sizeof(signal), 0)) < 0) {
            //std::cerr << "send failed, send bytes = " << bytes << std::endl;
            if (sokt >= 0) {
                close(sokt);
            }
            return 0;
        } else {

        }

        int mapCnt = 0;

        // 获取所有地图
        if (signal == 2017) {
            //获取地图总数量
            if ((bytes = recv(sokt, &mapCnt, sizeof(mapCnt), MSG_WAITALL)) <= 0) {
                close(sokt);
                return 0;
            }

            for (int i = 0; i < mapCnt; i++) {

                //获取地图名称长度【字符个数】
                int lenMapName = 0; // size of the mapName
                if ((bytes = recv(sokt, &lenMapName, sizeof(lenMapName), MSG_WAITALL)) <= 0) {
                    close(sokt);
                    return 0;
                }

                char mapNameBuff[1024];  // name of the map

                //获取地图名称
                if ((bytes = recv(sokt, mapNameBuff, lenMapName, 0)) <= 0) {
                    close(sokt);
                    return 0;
                }
                string mapName = string(mapNameBuff).substr(0, bytes);

                //获取地图宽度
                int mapWidth = 0;    // width of the map
                if ((bytes = recv(sokt, &mapWidth, sizeof(mapWidth), MSG_WAITALL)) <= 0) {
                    close(sokt);
                    return 0;
                }

                //获取地图高度
                int mapHeight = 0;    // height of the map
                if ((bytes = recv(sokt, &mapHeight, sizeof(mapHeight), MSG_WAITALL)) <= 0) {
                    close(sokt);
                    return 0;
                }

                //制作地图容器
                cv::Mat img;  // image container
                cv::Mat img2;
                img = cv::Mat::zeros(mapHeight, mapWidth,
                                     CV_8UC1);    // CV_8UC1 means that each pixels in the image is type unsigned char
                img2 = cv::Mat::zeros(48, 48,
                                      CV_8UC1);    // CV_8UC1 means that each pixels in the image is type unsigned char
                int imgSize = img.total() * img.elemSize();
                uchar *iptr = img.data;

                //获取地图数据本身
                if ((bytes = recv(sokt, iptr, imgSize, MSG_WAITALL)) <= 0) {
                    close(sokt);
                    return 0;
                }

                cv::Size ResImgSiz = cv::Size(img2.cols, img2.rows);
                cv::resize(img, img2, ResImgSiz);

                //保存到本地
                if (mode == 0) {
                    cv::imwrite(
                            string("/mnt/sdcard/com.lrkj.ctrl/maps/") + string(mapName) + ".pgm",
                            img);
                    cv::imwrite(
                            string("/mnt/sdcard/com.lrkj.ctrl/maps/") + string(mapName) + ".jpg",
                            img2);
                } else if (mode == 1) {
                    cv::imwrite(
                            string("/mnt/sdcard/com.lrkj.ctrl/navi/") + string(mapName) + ".pgm",
                            img);
                    cv::imwrite(
                            string("/mnt/sdcard/com.lrkj.ctrl/navi/") + string(mapName) + ".jpg",
                            img2);
                }
            }
        }
        if (sokt >= 0) {
            close(sokt);
        }
        return mapCnt;
    } catch (const std::exception &e) {
        if (sokt >= 0) {
            close(sokt);
        }
        return 0;
    }
}


// Send edited map

JNIEXPORT jint JNICALL Java_com_lrkj_business_LrNativeApi_sendEditMap
        (JNIEnv *env, jclass clazz, jstring mname, jstring mapPath) {
    if (!mapPath)
        return 0;
    try {
        int sokt = createSocket(ServerIP, 4101);
        int bytes = 0;
        int signal = 2018;

        string mapName = string(jstringToChars(env, mapPath)); //修改过的地图的名字
        string mapNN = string(jstringToChars(env, mname));
        cv::Mat img = cv::imread(mapName, CV_8UC1);  //读取本地修改过得地图

        /*
        signal meaning:
        2017 : retrive all maps from SLAM borad  获取所有地图
        2018  : send an edited map to SLAM board and replace the original one  发送一张修改过的地图给机器人
        */

        // 发送2018指令， 表示要上传地图
        if ((bytes = send(sokt, &signal, sizeof(signal), 0)) < 0) {
            //std::cerr << "send failed, send bytes = " << bytes << std::endl;
            return 0;
        }


        if (signal == 2018) {
            // send the size of  mapName ; 发送地图名称在所占字节个数
            int mapNameLen = mapNN.size();
            if ((bytes = send(sokt, &mapNameLen, sizeof(mapNameLen), 0)) <= 0) {
                return 0;
            }

            // send the name of the editedMap  发送地图名称
            if ((bytes = send(sokt, mapNN.c_str(), mapNN.size(), 0)) <= 0) {
                return 0;
            }

            usleep(50000);

            int width = img.cols;
            int height = img.rows;

            //send map width 发送地图宽度
            if ((bytes = send(sokt, &width, sizeof(width), 0)) <= 0) {
                return 0;
            }

            //send map height 发送地图高度
            if ((bytes = send(sokt, &height, sizeof(height), 0)) <= 0) {
                return 0;
            }

            //send map data 发送地图数据
            int imgSize = img.total() * img.elemSize();

            if (imgSize != width * height * 1) {
                return 0;
            }

            if ((bytes = send(sokt, img.data, imgSize, 0)) <= 0) {
                return 0;
            }

            //recieve ack information
            //获取反馈信息
            char ackBuff[1024];
            if ((bytes = recv(sokt, ackBuff, 1024, 0)) <= 0) {
                return 0;
            }
        }

        if (sokt >= 0) {
            close(sokt);
        }
        return 1;
    } catch (const std::exception &e) {
        return 0;
    }
}

JNIEXPORT jboolean JNICALL Java_com_lrkj_business_LrNativeApi_writeBitmapToPgm
        (JNIEnv *env, jclass, jstring path, jintArray buf, int w, int h) {

    jint *pixels = env->GetIntArrayElements(buf, NULL);
    char *pp = jstringToChars(env, path);
    if (pixels == NULL || pp == NULL) {
        return false;
    }

    try {
        cv::Mat imgData(h, w, CV_8UC4, pixels);
        cv::cvtColor(imgData, imgData, CV_RGBA2GRAY);
        cv::imwrite(string(pp), imgData);
    } catch (const std::exception &e) {
        return false;
    }
    return true;
}
//**********************************************/

JNIEXPORT jstring JNICALL
Java_hwj_opencvjni_OpenCVHelper_getStringTmp(JNIEnv *env, jclass thiz) {
    std::stringstream ss;
    ss << "Hello from c++ " << std::endl;
    return env->NewStringUTF(ss.str().c_str());
}

JNIEXPORT jintArray JNICALL
Java_hwj_opencvjni_OpenCVHelper_getGrayImage(JNIEnv *env, jobject, jintArray buf, int w, int h) {

    jint *pixels = env->GetIntArrayElements(buf, NULL);

    if (pixels == NULL) {
        return NULL;
    }

    cv::Mat imgData(h, w, CV_8UC4, pixels);
    uchar *ptr = imgData.ptr(0);

    for (int i = 0; i < w * h; i++) {
        int grayScale = (int) (ptr[4 * i + 2] * 0.299 + ptr[4 * i + 1] * 0.587 +
                               ptr[4 * i + 0] * 0.114);
        ptr[4 * i + 0] = (uchar) grayScale;
        ptr[4 * i + 1] = (uchar) grayScale;
        ptr[4 * i + 2] = (uchar) grayScale;
    }

    int size = w * h;
    jintArray result = env->NewIntArray(size);
    env->SetIntArrayRegion(result, 0, size, pixels);
    env->ReleaseIntArrayElements(buf, pixels, 0);

    return result;
}


