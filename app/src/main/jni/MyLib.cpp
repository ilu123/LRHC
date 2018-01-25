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
static char* ServerIP;
static int Port_Dot = 4097;  // 该端口用来显示点云
static int Port_Laser = 4100;
static int SocketDot = -1;
static int SocketLaser = -1;
static bool StopSocketDot = false;
static bool StopSocketLaser = false;

static int createSocket(const char* ip, int port) {
    int skt = -1;
    if ((skt = socket(PF_INET, SOCK_STREAM, 0)) < 0) {
        return -1;
    }

    /* Configure our timeout to be 5 seconds */
    struct timeval timeouta = {8, 0};
    setsockopt( skt, SOL_SOCKET, SO_SNDTIMEO, &timeouta, sizeof( timeouta ) );
    setsockopt( skt, SOL_SOCKET, SO_RCVTIMEO, &timeouta, sizeof( timeouta ) );

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

JNIEXPORT void JNICALL Java_com_lrkj_business_LrNativeApi_setRobotIp
        (JNIEnv *env, jclass thiz, jstring ip) {
    ServerIP = jstringToChars(env, ip);
}

JNIEXPORT jboolean JNICALL Java_com_lrkj_utils_LrSocketSurfaceView_getDotFrame
        (JNIEnv *env, jclass clazz, jobject obj, jlong ioMat)
{
    if (ServerIP == NULL)
        return false;

    StopSocketDot = false;

    int64_t  MAX_VEC_SIZE = 10000; //最大不超过的点数

    if (SocketDot < 0) {
        if ((SocketDot = createSocket(ServerIP, Port_Dot)) < 0) {

        }
    }

    jmethodID mid = env->GetMethodID(clazz, "postDotFrameFromNative", "()V");

    std::vector<std::pair<int, int> > vt;
    vt.push_back(std::make_pair(1, 1));

    int bytes = 0;

    //Mat img = Mat(600, 600, CV_8UC3, cv::Scalar(255, 255, 255));
    cv::Mat img = (*((cv::Mat*)ioMat));

    int reconnect = 0;
    while (!StopSocketDot) {
        if (reconnect) { //因为错误等原因 断开链接后，需要自动重新连接
            close(SocketDot);
            if ((SocketDot = createSocket(ServerIP, Port_Dot)) < 0) {
                continue;
            }
        }

        std::vector<std::pair<int, int> > vec; // red pointcloud  红色点云vector
        std::vector<std::pair<int, int> > vec1;  // black pointcloud 黑色点云vector
        std::vector<std::pair<int, int> > vec2;  // current pose   轨迹vector
        int64_t sz;  // vector的长度容器

        int64_t sz1;  // vector的长度容器

        int64_t sz2;  // vector的长度容器

        //接收点云长度

        if ((bytes = recv(SocketDot, &sz, sizeof(sz), MSG_WAITALL)) == -1) {
            std::cerr << "recv failed, received bytes = " << bytes << std::endl;
            reconnect = 1;
            continue;

        }

        //更改vector尺寸
        if (sz > MAX_VEC_SIZE) sz = MAX_VEC_SIZE;
        try {
            vec.resize(sz);
        } catch (const std::exception &e) {
            close(SocketDot);
            reconnect = 1;
            continue;

        }

        //cout << " resize red dot vector "<< endl;
        //接收红色点云
        if ((bytes = recv(SocketDot, &(*vec.begin()), sz * sizeof(vt[0]), MSG_WAITALL)) == -1) {
            std::cerr << "recv failed, received bytes = " << bytes << std::endl;
            reconnect = 1;
            continue;
        }

        //接收尺寸
        if ((bytes = recv(SocketDot, &sz1, sizeof(sz1), MSG_WAITALL)) == -1) {
            std::cerr << "recv failed, received bytes = " << bytes << std::endl;
            reconnect = 1;
            continue;
        }

        //cout<< "black: " << sz1 << endl;
        //更改vector尺寸
        if (sz1 > MAX_VEC_SIZE) sz = MAX_VEC_SIZE;
        try {
            vec1.resize(sz1);
        } catch (const std::exception &e) {
            close(SocketDot);
            reconnect = 1;
            continue;
            //cout << "black big" << endl;

        }
        //cout << " resize black dot vector "<< endl;
        //接收黑色点云
        if ((bytes = recv(SocketDot, &(*vec1.begin()), sz1 * sizeof(vt[0]), MSG_WAITALL)) == -1) {
            std::cerr << "recv failed, received bytes = " << bytes << std::endl;
            reconnect = 1;
            continue;
        }

        reconnect = 0;

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

        //cout << " resize kfram vector "<< endl;
        //接收轨迹点云
        if ((bytes = recv(SocketDot, &(*vec2.begin()), sz2 * 8, MSG_WAITALL)) == -1) {
            reconnect = 1;
            continue;
        }

        //画红色点
        for (int i = 0; i < vec.size(); i++) {
            cv::circle(img, cv::Point(vec[i].first, vec[i].second), 1, cv::Scalar(0, 0, 255), -1);
        }
        //画黑色点
        for (int i = 0; i < vec1.size(); i++) {
            cv::circle(img, cv::Point(vec1[i].first, vec1[i].second), 1, cv::Scalar(0, 0, 0), -1);
        }
        //画轨迹
        for (int i = 0; i < vec2.size(); i++) {
            cv::circle(img, cv::Point(vec2[i].first, vec2[i].second), 2, cv::Scalar(0, 255, 0), 1,
                       CV_AA);
        }

        cv::circle(img, cv::Point(vec2[0].first, vec2[0].second), 5, cv::Scalar(255, 0, 0), -1,
                   CV_AA);

        // Call Java mid to draw
        env->CallVoidMethod(obj, mid);

        //显示画布
        //cv::imshow("RGBD Camera", img);
        //cv::imwrite("pointCloud.jpg", img);

        //重置画布
        //img.setTo(cv::Scalar(255, 255, 255));
    }
    return false;
}


JNIEXPORT void JNICALL Java_com_lrkj_utils_LrSocketSurfaceView_stopDotSocket
        (JNIEnv *, jclass)
{
    StopSocketDot = true;
    if (SocketDot >= 0) {
        close(SocketDot);
    }
    SocketDot = -1;
}

// Laser

JNIEXPORT jboolean JNICALL Java_com_lrkj_utils_LrSocketSurfaceView_getLaserFrame
        (JNIEnv *env, jclass clazz, jobject obj, jlong ioMat)
{
    if (ServerIP == NULL)
        return false;

    StopSocketLaser = false;

    int64_t  MAX_VEC_SIZE = 10000; //最大不超过的点数

    if (SocketLaser < 0) {
        if ((SocketLaser = createSocket(ServerIP, Port_Laser)) < 0) {

        }
    }

    int bytes = 0;

    //获取地图宽度
    int mapWidth =0 ;	// width of the map
    if ((bytes = recv(SocketLaser, &mapWidth, sizeof (mapWidth), MSG_WAITALL)) <= 0){
        std::cerr << "bytes = " << bytes << std::endl;
        return 0;

    }

    //获取地图高度
    int mapHeight =0 ;	// height of the map
    if ((bytes = recv(SocketLaser, &mapHeight, sizeof (mapHeight), MSG_WAITALL)) <= 0){
        std::cerr << "bytes = " << bytes << std::endl;
        return 0;
    }

    cv::Mat lasermap = cv::Mat(mapHeight, mapWidth, CV_8UC1, cv::Scalar(128));
    cv::Mat pose = cv::Mat(mapHeight, mapWidth, CV_8UC1, cv::Scalar(0));

    jmethodID mid = env->GetMethodID(clazz, "postLaserFrameFromNative", "()V");
    cv::Mat img = (*((cv::Mat*)ioMat));
    cv::Size ResImgSiz = cv::Size(img.cols, img.rows);

    while (!StopSocketLaser) {
        //获取x坐标
        int x_cam =0 ;	// height of the map
        if ((bytes = recv(SocketLaser, &x_cam, sizeof (x_cam), MSG_WAITALL)) <= 0){
            std::cerr << "bytes = " << bytes << std::endl;
            return 0;
        }

        //获取y坐标
        int y_cam =0 ;	// height of the map
        if ((bytes = recv(SocketLaser, &y_cam, sizeof (y_cam), MSG_WAITALL)) <= 0){
            std::cerr << "bytes = " << bytes << std::endl;
            return 0;
        }

        std::vector< std::pair< int , int> > laser_dots;
        int sz=0;

        //获取点云的尺寸
        if ((bytes = recv(SocketLaser, &sz, sizeof(sz) , MSG_WAITALL)) == -1) {
            std::cerr << "recv failed, received bytes = " << bytes << std::endl;
        }

        //获取点云
        if (sz > MAX_VEC_SIZE) sz = MAX_VEC_SIZE;
        laser_dots.resize( sz );
        if ((bytes = recv(SocketLaser, &(*laser_dots.begin()), sz * 8  , MSG_WAITALL)) == -1) {
            std::cerr << "recv failed, received bytes = " << bytes << std::endl;
        }

        // 画激光地图， 画图方式: 连线当前坐标（x_cam, y_cam）和点云中的所有点
        for(int i=0; i<laser_dots.size(); i++)
        {
            int x_p = laser_dots[i].first;
            int y_p = laser_dots[i].second;
            if(x_p >= 0 && y_p >=0 && x_p < lasermap.cols && y_p < lasermap.rows)
            {
                cv::line(lasermap,cv::Point(x_cam,y_cam),cv::Point(x_p,y_p),cv::Scalar(255));
                lasermap.at<uchar>(y_p,x_p) = 0;
            }
        }

        cv::circle ( pose , cv::Point ( x_cam , y_cam) ,  3 , cv::Scalar(255) , -1 ) ;

        cv::resize(lasermap-pose, img, ResImgSiz);

        // Call Java mid to draw
        env->CallVoidMethod(obj, mid);
    }
    return false;
}


JNIEXPORT void JNICALL Java_com_lrkj_utils_LrSocketSurfaceView_stopLaserSocket
        (JNIEnv *, jclass)
{
    StopSocketLaser = true;
    if (SocketLaser >= 0) {
        close(SocketLaser);
    }
    SocketLaser = -1;
}


// Ge all maps

JNIEXPORT jint JNICALL Java_com_lrkj_business_LrNativeApi_getAllMaps
        (JNIEnv *env, jclass clazz)
{
    int sokt = createSocket(ServerIP, 4101);
    int bytes = 0 ;

    int signal = 2017 ;

    /*
    signal meaning:
    2017 : retrive all maps from SLAM borad  获取所有地图
    2018  : send an edited map to SLAM board and replace the original one  发送一张修改过的地图给机器人
    */

    // 发送 信号
    if ((bytes = send(sokt, &signal, sizeof (signal) , 0 )) < 0 ) {
        //std::cerr << "send failed, send bytes = " << bytes << std::endl;
    }else {
        return 0;
    }

    // 获取所有地图
    if (signal == 2017) {
        int mapCnt =0 ;

        //获取地图总数量
        if ((bytes = recv(sokt, &mapCnt, sizeof(mapCnt) , MSG_WAITALL)) <=0) {
            //std::cerr << "recv failed, received bytes = " << bytes << std::endl;
            return 0;
        }

        for ( int i = 0 ; i < mapCnt ; i++ ){

            //获取地图名称长度【字符个数】
            int lenMapName = 0 ; // size of the mapName
            if ((bytes = recv(sokt, &lenMapName, sizeof (lenMapName), MSG_WAITALL)) <= 0){
                std::cerr << "bytes = " << bytes << std::endl;
                return 0;
            }

            char mapNameBuff [1024] ;  // name of the map

            //获取地图名称
            if ((bytes = recv(sokt, mapNameBuff, lenMapName, 0)) <= 0){
                std::cerr << "bytes = " << bytes << std::endl;
                return 0;
            }
            string mapName = string(mapNameBuff).substr(0 , bytes) ;

            //获取地图宽度
            int mapWidth =0 ;	// width of the map
            if ((bytes = recv(sokt, &mapWidth, sizeof (mapWidth), MSG_WAITALL)) <= 0){
                std::cerr << "bytes = " << bytes << std::endl;
                return 0;
            }

            //获取地图高度
            int mapHeight =0 ;	// height of the map
            if ((bytes = recv(sokt, &mapHeight, sizeof (mapHeight), MSG_WAITALL)) <= 0){
                std::cerr << "bytes = " << bytes << std::endl;
                return 0;
            }

            //制作地图容器
            cv::Mat img;  // image container
            cv::Mat img2;
            img = cv::Mat::zeros( mapHeight ,  mapWidth , CV_8UC1);    // CV_8UC1 means that each pixels in the image is type unsigned char
            img2 = cv::Mat::zeros( 48 ,  48 , CV_8UC1);    // CV_8UC1 means that each pixels in the image is type unsigned char
            int imgSize = img.total() * img.elemSize();
            uchar *iptr = img.data;

            //获取地图数据本身
            if ((bytes = recv(sokt, iptr, imgSize , MSG_WAITALL))  <=0 ) {
                std::cerr << "recv failed, received bytes = " << bytes << std::endl;
                return 0;
            }

            cv::Size ResImgSiz = cv::Size(img2.cols, img2.rows);
            cv::resize(img, img2, ResImgSiz);

            //保存到本地
            cv::imwrite (string("/mnt/sdcard/com.lrkj.ctrl/maps/") + string(mapName) + ".pgm",  img);
            cv::imwrite (string("/mnt/sdcard/com.lrkj.ctrl/maps/icon_") + string(mapName) + ".jpg",  img2);
        }
    }
    close(sokt);
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
