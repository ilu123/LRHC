# cmake version
cmake_minimum_required(VERSION 3.4.1)

# 支持 -std=gnu++11
set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

# opencv import libs
set(pathToOpenCV ./../OpenCV-2.4.11-android-sdk)
include_directories(${pathToOpenCV}/sdk/native/jni/include)

add_library( lib_opencv SHARED IMPORTED )
set_target_properties( lib_opencv PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/src/main/OpenCVLib/${ANDROID_ABI}/libopencv_java.so )


add_library( my-lib SHARED ${CMAKE_SOURCE_DIR}/src/main/jni/MyLib.cpp )

find_library( log-lib log )

#target_link_libraries( native-lib ${log-lib} lib_opencv)
target_link_libraries( my-lib ${log-lib} lib_opencv)