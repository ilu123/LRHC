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
#include <cmath>

#include "com_lrkj_business_LrNativeApi.h"

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
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("utf-8");
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
    return rtn;
}


/***********[LR]*****************************/
static char *ServerIP;
static int ServerPort = 4104;
static int ServerPortCmd = 4103;
static bool StopSocket = false;
static int sokt = -1;

static float orix, oriy;      // 地图原点（左下角的像素点） 所对应的实际坐标
static float reso = 0.025;  // 地图分辨率  单位  米/像素
static bool send_flag = false;
static float x, y;    // 目的地的实际坐标 单位 米
static cv::Point landMark(0, 0);  // 目的地（像素像素坐标）
static int MAP_WIDTH = 0, MAP_HEIGHT = 0;


static int createSocket(const char *ip, int port) {
    int skt = -1;
    if ((skt = socket(PF_INET, SOCK_STREAM, 0)) < 0) {
        return -1;
    }

    /* Configure our timeout to be 5 seconds */
    struct timeval timeouta = {15, 0};
    setsockopt(skt, SOL_SOCKET, SO_SNDTIMEO, &timeouta, sizeof(timeouta));
    setsockopt(skt, SOL_SOCKET, SO_RCVTIMEO, &timeouta, sizeof(timeouta));

    struct sockaddr_in serverAddr;
    socklen_t addrLen = sizeof(struct sockaddr_in);
    serverAddr.sin_family = PF_INET;
    serverAddr.sin_addr.s_addr = inet_addr(ip);
    serverAddr.sin_port = htons(port);

    if (connect(skt, (sockaddr *) &serverAddr, addrLen) < 0) {
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


static bool sendFloat(int sokt, float signal) {
    int bytes = 0;
    if ((bytes = send(sokt, &signal, sizeof(signal), 0)) == -1) {
        return false;
    }
    return true;
}

static bool printAck(int sokt) {
    int bytes = 0;
    char resuBuff[1024];
    if ((bytes = recv(sokt, resuBuff, 1024, 0)) == -1) {
        return false;
    }
    return true;
}


static void click(int px, int py, int flags) {   // 点击更改目的地为 px py
        //根据像素坐标推算实际坐标
        x = px * reso + orix;
        y = oriy + (MAP_HEIGHT - py) * reso;
        send_flag = true;
        landMark.x = px;
        landMark.y = py;
}

JNIEXPORT void JNICALL Java_com_lrkj_utils_LrSocketSurfaceView_getNaviFrame
        (JNIEnv *env, jclass clazz, jobject obj, jstring ipS, jstring mapname, jlong matMap, jlong ioMat, jint width, jint height) {

    StopSocket = false;

    const char *ip = jstringToChars(env, ipS);
    const char *mapName = jstringToChars(env, mapname);
    string navMapFile = (string("/mnt/sdcard/com.lrkj.ctrl/maps/") + string(mapName) + string(".pgm"));

    cv::Mat img = (*((cv::Mat *) matMap));
    cv::Mat ioImg = (*((cv::Mat *) ioMat));
    cv::Size ResImgSiz = cv::Size(ioImg.cols, ioImg.rows);


    // Tests
    //cv::circle(ioImg, cv::Point(5, 5), 16, cv::Scalar(255), -1, CV_AA);
    //cv::resize(img, ioImg, ResImgSiz);
//    jmethodID mid = env->GetMethodID(clazz, "postNaviFrameFromNative", "()V");
//    env->CallVoidMethod(obj, mid);
//    return;
    // Test end

    ////////// get map info
    //read Map  读取地图信息 ：  宽度， 高度 ， 原点， 分辨率（默认为0.025米/像素）
    MAP_WIDTH = width;
    MAP_HEIGHT = height;

    sokt = createSocket(ip, ServerPortCmd);
    if (sendSignal(sokt, 999) && sendSignal(sokt, 1013) &&
        ((send(sokt, mapName, strlen(mapName), 0)) != -1)) {
        while(printAck(sokt));
        if (!sendSignal(sokt, 1006)) {
            close(sokt);
            return;
        }
        int i = recvSignal(sokt);
        if (i != -1) {
            if (i == 0) {
                close(sokt);
                return;
            } else if (i == 1) {
                orix = recvFloat(sokt);  // 获取原点 x
                if (orix < -10000) {
                    //cout << "recv float error " << endl ;
                    close(sokt);
                    return;
                }

                oriy = recvFloat(sokt);   // 获取原点 y
                if (oriy < -10000) {
                    //cout << "recv float error " << endl ;
                    close(sokt);
                    return;
                }

                int width = recvSignal(sokt); // 获取地图宽度
                if (width == -1) {
                    //cout << "recv  error " << endl ;
                    close(sokt);
                    return;
                }

                int height = recvSignal(sokt); // 获取地图高度
                if (height == -1) {
                    //cout << "recv  error " << endl ;
                    close(sokt);
                    return;
                }

                // 获取地图本身
                cv::Mat img22 = cv::Mat(height, width, CV_8UC1);
                int imgSize = img22.total() * img22.elemSize();
                uchar *iptr = img22.data;

                int bytes = 0;
                if ((bytes = recv(sokt, iptr, imgSize, MSG_WAITALL)) <= 0) {
                    //std::cerr << "recv failed, received bytes = " << bytes << std::endl;
                    close(sokt);
                    return;
                }

                cv::imwrite(navMapFile, img22);

                img22.release();
            }
        } else {
            close(sokt);
            sokt = -1;
            return;
        }
    }else{
        close(sokt);
        return;
    }
    close(sokt);


    ////////// nav
    usleep(2000);
    int bytes = 0;
    cv::Mat markLayer = img.clone();

    sokt = createSocket(ip, ServerPort);

    while (!StopSocket) {
        if (sokt < 0) {
            sokt = createSocket(ip, ServerPort);
        }

        markLayer.setTo(cv::Scalar(0));

        // send goal   显示目的地， 如果存在的话
        if (send_flag) {
            cv::circle(markLayer, landMark, 8, cv::Scalar(255, 0, 0), -1, CV_AA);
            send_flag = false;

            if (sendSignal(sokt, 1005)) {

                //cout << "send succeed" << endl ;

                // 发送目的地
                if (!sendFloat(sokt, x)) return;
                if (!sendFloat(sokt, y)) return;
                if (!sendFloat(sokt, 0)) return;

            } else {

                //cout << "send fail" << endl ;
            }

        }

        // get pose  获取当前机器人位置
        if (sendSignal(sokt, 1007)) {

            // 机器人当前位置
            float x = recvFloat(sokt);
            if (x < -10000) {
                return;
            }


            float y = recvFloat(sokt);
            if (y < -10000) {
                return;
            }


            float z = recvFloat(sokt);


            int px = (int) ((x - orix) / reso);
            int py = (int) (img.rows - (y - oriy) / reso);

            // 显示目的地
            cv::circle(markLayer, cv::Point(px, py), 6, cv::Scalar(0, 255, 0), -1, CV_AA);
        } else {
            return;
        }

        // get path   获取规划路径
        if (sendSignal(sokt, 1008)) {
            int size;
            // 规划路径的点的个数
            if ((bytes = recv(sokt, &size, sizeof(size), 0)) <= 0) {
                return;
            }

            if (size > 0) {
                std::vector<std::pair<float, float> > vec; // 规划路径的容器
                vec.resize(size);

                if ((bytes = recv(sokt, &(*vec.begin()), size * 8, MSG_WAITALL)) <= 0) {
                    return;
                }

                for (unsigned int i = 0; i + 1 < vec.size(); i += 2) {

                    float x = vec[i].first;
                    float y = vec[i].second;

                    int px = (int) ((x - orix) / reso);
                    int py = (int) (img.rows - (y - oriy) / reso);

                    cv::circle(markLayer, cv::Point(px, py), 2, cv::Scalar(255,0,0), -1, CV_AA);

                }
            }

        } else {
            return;
        }

        cv::circle(markLayer, landMark, 6, cv::Scalar(0, 0, 255), -1, CV_AA);

        cv::subtract(img, markLayer, ioImg);

        // Call Java mid to draw
        jmethodID mid = env->GetMethodID(clazz, "postNaviFrameFromNative", "()V");
        env->CallVoidMethod(obj, mid);

        if (StopSocket) break;
    }

    close(sokt);
    sokt = -1;
}

JNIEXPORT void JNICALL Java_com_lrkj_utils_LrSocketSurfaceView_clickNaviTo
        (JNIEnv *env, jclass clazz, jint px, jint py) {
    click(px, py, 0);
}

JNIEXPORT void JNICALL Java_com_lrkj_utils_LrSocketSurfaceView_stopNaviSocket
        (JNIEnv *, jclass) {
    StopSocket = true;
    if (sokt >= 0) {
        close(sokt);
    }
    sokt = -1;
    StopSocket = false;
    orix = 0;
    oriy = 0;      // 地图原点（左下角的像素点） 所对应的实际坐标
    reso = 0.025;  // 地图分辨率  单位  米/像素
    send_flag = false;
    x = 0;
    y = 0;    // 目的地的实际坐标 单位 米
}

