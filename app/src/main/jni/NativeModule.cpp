//
// Created by aa on 2016-03-04.
//
#include <jni.h>
#include "com_android_hairyd_NativeFunc.h"
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <cv.h>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/calib3d/calib3d.hpp>

#include <opencv2/imgproc/imgproc_c.h> //cvFindContours
#include <android/log.h>
#include <vector>
#include <time.h>
#include <stdio.h>

using namespace std;
using namespace cv;


extern "C" {


JNIEXPORT jstring JNICALL Java_com_android_hairyd_NativeFunc_getStringFromNative
        (JNIEnv *env, jobject obj) {

    return (*env).NewStringUTF("My STring! yeah!");
}

JNIEXPORT void JNICALL Java_com_android_hairyd_NativeFunc_FindFeatures(JNIEnv *, jobject,
                                                                       jlong addrGray,
                                                                       jlong addrRgba, jint seek) {


    Mat &mGr = *(Mat *) addrGray;
    Mat &mRgb = *(Mat *) addrRgba;

    int cnt = 0;
    //create the cascade classifier object used for the face detection

    __android_log_print(ANDROID_LOG_WARN, "NDK", "ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡFaceDetection");

    IplImage camera = mRgb;

    IplImage *gray = cvCreateImage(cvGetSize(&camera), camera.depth, 1);
    IplImage *eye = cvCreateImage(cvSize(160, 160), camera.depth, 1);


    CvHaarClassifierCascade *face_cascade = (CvHaarClassifierCascade *) cvLoad(
            "/data/data/com.android.hairyd/files/haarcascade_frontalface_alt2.xml", 0, 0, 0);
    CvHaarClassifierCascade *eye_cascade = (CvHaarClassifierCascade *) cvLoad(            "/data/data/com.android.hairyd/files/haarcascade_eye.xml", 0, 0, 0);


    if (!face_cascade) {

        __android_log_print(ANDROID_LOG_WARN, "NDK", " face cascade error!!");


        return;
    }

    if (!eye_cascade) {
        __android_log_print(ANDROID_LOG_WARN, "NDK", "eye cascade error!!");


        return;
    }


    CvMemStorage *storage1 = cvCreateMemStorage(0);

    CvMemStorage *storage2 = cvCreateMemStorage(0);


    cvCvtColor(&camera, gray, CV_BGR2GRAY);


    CvSeq *faces = cvHaarDetectObjects(gray, face_cascade, storage1, 2.0, 1, 0, cvSize(100, 100),
                                       cvSize(300, 300));

    CvSeq *eyes = cvHaarDetectObjects(gray, eye_cascade, storage2, 2.0, 1, 0, cvSize(40, 40),
                                      cvSize(60, 60));


    for (int i = 0; i < faces->total; i++) {

        CvRect *face_region = 0;

        face_region = (CvRect *) cvGetSeqElem(faces, i);


        cvRectangle(&camera, cvPoint(face_region->x, face_region->y),
                    cvPoint(face_region->x + face_region->width,
                            face_region->y + face_region->height), cvScalar(255, 0, 0), 1, CV_AA,
                    0);


        for (int j = 0; j < eyes->total; j++) {

            CvRect *eye_region = 0;

            eye_region = (CvRect *) cvGetSeqElem(eyes, j);

            if ((eye_region->x > face_region->x) &&
                (eye_region->x < (face_region->x + face_region->width)) &&
                (eye_region->y > face_region->y) &&
                (eye_region->y < face_region->y + (face_region->height / 2))) {


                cvRectangle(&camera, cvPoint(eye_region->x, eye_region->y),
                            cvPoint(eye_region->x + eye_region->width,
                                    eye_region->y + eye_region->height), cvScalar(0, 0, 255), 1,
                            CV_AA, 0);


                cvSetImageROI(&camera, cvRect(eye_region->x, eye_region->y,
                                             eye_region->x + eye_region->width,
                                             eye_region->y + eye_region->height));

            }


        }


        cvReleaseMemStorage(&storage1);

        cvReleaseMemStorage(&storage2);

        cvReleaseImage(&gray);


        Rect rc(340, 20, 600, 680);  //x,y,w,h;
        Scalar color(255, 0, 0);
        int thickness = 2;    // line thickness
        rectangle(mRgb, rc, color, thickness);

    }
}
    JNIEXPORT void JNICALL Java_com_android_hairyd_NativeFunc_getDisparity(JNIEnv *, jobject,
                                                                           jlong left,
                                                                           jlong right, jint seek) {


        __android_log_print(ANDROID_LOG_WARN, "NDK",
                            "ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡreturning...");

    }

JNIEXPORT void JNICALL Java_com_android_hairyd_NativeFunc_drawPoint(JNIEnv *, jobject,
                                                                       jlong addrRgba,
                                                                       jfloat x, jfloat y) {


    __android_log_print(ANDROID_LOG_WARN, "NDK",
                        "ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡdrawing (%f,%f)",x,y);


    Mat &mRgb = *(Mat *) addrRgba;


    circle(mRgb, cvPoint((int)x, (int)y), 2, cvScalar(255,0,0));


}



}