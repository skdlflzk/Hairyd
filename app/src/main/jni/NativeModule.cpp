//
// Created by aa on 2016-03-04.
//
#include <jni.h>
#include "com_android_hairyd_NativeFunc.h"
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
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
    vector <KeyPoint> v;

//hsb의 살색 범위는 약 h 22~36까지
    /*
     128 ≤ Cr ≤ 170
    73 ≤ Cb ≤ 158
[출처] [OpenCV] 손 동작 인식, 피부색(Skin Color)를 이용한 손 검출|작성자 꼬꼬꼬
     */
    GaussianBlur(mRgb,mRgb,Size(9,9),2.0,2.0);

    Mat hsv(mGr.rows, mGr.cols, CV_8UC3, 0);
    cvtColor(mRgb, hsv, CV_RGB2HSV); //rgb를 hsv로 변환 CV_RGB2HSV
    uchar H, S, V;
    Mat hsv_channels[3];
    split(hsv, hsv_channels);

    Mat YCrCb(mGr.rows, mGr.cols, CV_8UC3, 0);
    cvtColor(mRgb, YCrCb, CV_RGB2YCrCb);
    uchar Y, Cr, Cb;
    Mat YCrCb_channels[3];
    split(YCrCb, YCrCb_channels);

    Mat mask;

    for (int i = 0; i < hsv.cols; i++) {
        for (int j = 0; j < hsv.rows; j++) {
//
//            H = (uchar) hsv_channels[0].data[(i * hsv.rows) + j + 0];

            Cr = (uchar) YCrCb_channels[1].data[(i * hsv.rows) + j + 0];
            Cb = (uchar) YCrCb_channels[2].data[(i * hsv.rows) + j + 0];

            if (!( (Cr >= (128 + seek) && Cr <= 170-seek) && ( Cb >= 73+seek && Cb <= 158-seek))) {
                hsv_channels[2].data[(i * hsv.rows) + j + 0] = 0;
//                __android_log_print(ANDROID_LOG_WARN,"NDK", "Cr=%c,Cb=%c",Cr,Cb);

            }
        }
    }

    vector <Mat> channelslzm;

    //Mat gray=0.299*channels[2]+0.587*channels[1]+0.114*channels[0];r,g,b 순서?

//    CvMemStorage *storage = cvCreateMemStorage(0);
//    CvSeq * contour = 0;
    vector < vector<Point> > contour;

//    Mat tmp(mGr.rows, mGr.cols, CV_8UC3,0);
    Mat tmp = hsv_channels[2].clone();

//    Canny( tmp, tmp, seek, 200, 3 );

//      threshold(tmp,tmp,seek,255,CV_THRESH_BINARY); //TODO 안하는게 나은듯
//    adaptiveThreshold(tmp,tmp,255,CV_ADAPTIVE_THRESH_GAUSSIAN_C,CV_THRESH_BINARY,7,5);

    int sz = 5;
    Mat mel(sz, sz, CV_8U, Scalar(1));
    morphologyEx(tmp, tmp, MORPH_OPEN, mel);
    morphologyEx(tmp, tmp, MORPH_CLOSE, mel);
//    erode(tmp,tmp);

    findContours(tmp, contour, RETR_EXTERNAL, CHAIN_APPROX_NONE);
    //Mat, vector<vector<Point>>

//    __android_log_print(ANDROID_LOG_WARN, "NDK", "2, 개수 = %d", contour.size() );

    channelslzm.push_back(hsv_channels[0]);//h
    channelslzm.push_back(hsv_channels[1]);//s
    channelslzm.push_back(hsv_channels[2]);//v
    merge(channelslzm, mRgb);


    cvtColor(mRgb, mRgb, CV_HSV2RGB);
//    for (int i = 0; i < contour.size(); i++) { }

//    drawContours(mRgb, contour,
//                 -1,    // 모든 외곽선 그리기
//                 Scalar(255, 0, 0, 255), // 빨강 Scalar(0)은 검정
//                C);    //CV_FILLED시 채움 두께를 2로
    int largest_area = 0;
    int largest_contour_index = 0;
    Rect bounding_rect;

    for (int i = 0; i < contour.size(); i++) // iterate through each contour.
    {
        double a = contourArea(contour[i], false);  //  Find the area of contour
        if (a > largest_area) {
            largest_area = a;
            largest_contour_index = i;                //Store the index of largest contour
            bounding_rect = boundingRect(
                    contour[i]); // Find the bounding rectangle for biggest contour
        }
    }
    drawContours(mRgb, contour, largest_contour_index, Scalar(255, 0, 0, 255),
                 2); // Draw the largest contour using previously stored index.


}

JNIEXPORT void JNICALL Java_com_android_hairyd_NativeFunc_getDisparity(JNIEnv *, jobject,
                                                                       jlong left,
                                                                       jlong right, jint seek) {
    __android_log_print(ANDROID_LOG_WARN,"NDK", "ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡgetDisparity");

    Mat &mLeft = *(Mat *) left;
    const CvMat* cmLeft = (CvMat* ) left;

    Mat &mRight = *(Mat *) right;
    const CvMat* cmRight = (CvMat* ) right;


    int r= mLeft.rows;
    int c= mLeft.cols;

    Mat le(mLeft.rows, mLeft.cols, CV_8UC1, 0);
    Mat ri(mLeft.rows, mLeft.cols, CV_8UC1, 0);

    cvtColor(mLeft, le, CV_BGR2GRAY);
    cvtColor(mRight, ri, CV_BGR2GRAY);

    /*
    __android_log_print(ANDROID_LOG_WARN,"NDK", "Disp");

    Mat &mLeft = *(Mat *) left;
    const CvMat* cmLeft = (CvMat* ) left;

    Mat &mRight = *(Mat *) right;
    const CvMat* cmRight = (CvMat* ) right;


    int r= mLeft.rows;
    int c= mLeft.cols;

    Mat le(mLeft.rows, mLeft.cols, CV_8UC1, 0);
    Mat ri(mLeft.rows, mLeft.cols, CV_8UC1, 0);

    cvtColor(mLeft, le, CV_BGR2GRAY);
    cvtColor(mRight, ri, CV_BGR2GRAY);

    StereoSGBM sgbm;
    sgbm.SADWindowSize = 5;
    sgbm.numberOfDisparities = 192;
    sgbm.preFilterCap = 4;
    sgbm.minDisparity = -64;
    sgbm.uniquenessRatio = 1;
    sgbm.speckleWindowSize = 150;
    sgbm.speckleRange = 2;
    sgbm.disp12MaxDiff = 10;
    sgbm.fullDP = false;
    sgbm.P1 = 600;
    sgbm.P2 = 2400;

    Mat disp, disp8;
    sgbm(le, ri, disp);

    normalize(disp, disp8, 0, 255, CV_MINMAX, CV_8U);

    mLeft = disp;
    */   //sgbm


//    IplImage* leftImage = cvCreateImage(cvSize(r,c), IPL_DEPTH_8U, 1);
//    IplImage* rightImage = cvCreateImage(cvSize(r,c), IPL_DEPTH_8U, 1);


    cvtColor(mLeft, le, CV_BGR2GRAY);
    cvtColor(mRight, ri, CV_BGR2GRAY);

    IplImage copy = le;
    IplImage* leftImage = &copy;

    IplImage copyr = ri;
    IplImage* rightImage = &copyr;

    // Create the disparity map for the block matching algorithm.
    IplImage* disparity = cvCreateImage(cvSize(c,r), IPL_DEPTH_16S, 1);


    __android_log_print(ANDROID_LOG_WARN,"NDK", "left w = %d, h = %d// right  w = %d, h = %d // disp  w = %d, h = %d",leftImage->width,leftImage->height,rightImage->width,leftImage->height,disparity->width,disparity->height);


//    CvStereoBMState* stereoBM_state = cvCreateStereoBMState( CV_STEREO_BM_BASIC, 0 );
    CvStereoBMState *BMState = cvCreateStereoBMState();
    BMState->preFilterSize=21;  // 5x5에서 21x21까지
    BMState->preFilterCap=21;	//....뭔지...기억이..-0-
    BMState->SADWindowSize=21;  //스테레오 공부하시는 분들이라면 다 아실거라 생각합니다.(5x5.... 21x21)
    BMState->minDisparity=1;
    BMState->numberOfDisparities=128;  //Searching하기 위한 pixel들의 개수
    BMState->textureThreshold=10;    //minimum allowed
    BMState->uniquenessRatio=5;

    cvFindStereoCorrespondenceBM( rightImage, leftImage, disparity, BMState);

    cvConvertScale( disparity, disparity, 255.0f/5 );


    mLeft = cvarrToMat(disparity);

    char file_name[35];

    time_t timer;
    struct tm *t;

    timer = time(NULL); // 현재 시각을 초 단위로 얻기
    t = localtime(&timer);

    sprintf(file_name,"/sdcard/Lucy/disparity%d.bmp",t->tm_sec);            //파일이름 맹글기
    cvSaveImage(file_name,disparity);

    __android_log_print(ANDROID_LOG_WARN,"NDK", "ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡreturning...");

}

}