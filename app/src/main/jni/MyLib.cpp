//
// Created by Administrator on 2016/12/13.
//

#include <iostream>
#include <sstream>
#include <jni.h>
#include <opencv2/opencv.hpp>

#include "hwj_opencvjni_OpenCVHelper.h"

JNIEXPORT jstring JNICALL
Java_hwj_opencvjni_OpenCVHelper_getStringTmp(JNIEnv *env, jclass thiz){
    std::stringstream ss;
    ss << "Hello from c++ " << std::endl;
    return env->NewStringUTF(ss.str().c_str());
}

JNIEXPORT jintArray JNICALL
Java_hwj_opencvjni_OpenCVHelper_getGrayImage(JNIEnv *env, jobject, jintArray buf, int w, int h){

    jint *pixels = env->GetIntArrayElements(buf, NULL);

    if(pixels == NULL){
        return NULL;
    }

    cv::Mat imgData(h, w, CV_8UC4, pixels);
    uchar *ptr = imgData.ptr(0);

    for(int i=0; i<w*h; i++){
        int grayScale = (int)(ptr[4*i+2]*0.299 + ptr[4*i+1]*0.587 + ptr[4*i+0]*0.114);
        ptr[4*i+0] = (uchar)grayScale;
        ptr[4*i+1] = (uchar)grayScale;
        ptr[4*i+2] = (uchar)grayScale;
    }

    int size = w * h;
    jintArray result = env->NewIntArray(size);
    env->SetIntArrayRegion(result, 0, size, pixels);
    env->ReleaseIntArrayElements(buf, pixels, 0);

    return result;
}
