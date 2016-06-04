LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#OpenCV load

#OPENCV_LIB_TYPE:=SHARED
#OPENCV_CAMERA_MODULES:=on
#OPENCV_INSTALL_MODULES:=on

include C:\Users\aa\Desktop\hairydd\OpenCV-android-sdk\sdk\native\jni\OpenCV.mk

LOCAL_SHARED_LIBRARIES += libdl

LOCAL_MODULE := NativeModule
LOCAL_SRC_FILES := NativeModule.cpp
LOCAL_LDLIBS += -llog


include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)