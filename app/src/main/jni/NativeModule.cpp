//
// Created by aa on 2016-03-04.
//
#include <jni.h>
#include "com_android_hairyd_NativeFunc.h"
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/imgproc/imgproc_c.h>
#include <android/log.h>
#include <vector>
#include <stdio.h>

using namespace std;
using namespace cv;


extern "C" {


JNIEXPORT jstring JNICALL Java_com_android_hairyd_NativeFunc_getStringFromNative
        (JNIEnv *env, jobject obj) {

        return (*env).NewStringUTF("My STring! yeah!");
}

JNIEXPORT void JNICALL Java_com_android_hairyd_NativeFunc_FindFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrRgba)
{


        Mat& mGr  = *(Mat*)addrGray;
        Mat& mRgb = *(Mat*)addrRgba;
        vector<KeyPoint> v;

//hsb의 살색 범위는 약 h 22~36까지
        Mat hsv(mGr.rows, mGr.cols, CV_8UC3,0);
    //__android_log_print(ANDROID_LOG_WARN,"NDK", "h<22||H>36 rows = %d, cols = %d", (int) mGr.rows, (int) mGr.cols);
        cvtColor( mRgb, hsv, CV_RGB2HSV); //rgb를 hsv로 변환 CV_RGB2HSV
        uchar H, S, V;

    Mat hsv_channels[3];
    split( hsv, hsv_channels );

    Mat mask;

    for ( int i = 0 ; i < hsv.cols ; i++ )
    {
        for ( int j = 0 ;j < hsv.rows ; j++ )
        {
//
            H = (uchar)hsv_channels[0].data[(i*hsv.rows)+j+0];//H = (uchar)hsv.data[(i*hsv.rows)+j*3+0];
//            S = (uchar)hsv.data[(i*hsv.rows)+j*3+1];
//            V = (uchar)hsv.data[(i*hsv.rows)+j*3+2];

//            mRgb.data[(i*hsv.rows)+j*3+0] = (uchar)hsv.data[(i*hsv.rows)+j*3+0];
//            mRgb.data[(i*hsv.rows)+j*3+1] =(uchar)hsv.data[(i*hsv.rows)+j*3+1];
//            mRgb.data[(i*hsv.rows)+j*3+2]= (uchar)hsv.data[(i*hsv.rows)+j*3+2];

//
            if( H < 4 || H > 50){
//               __android_log_print(ANDROID_LOG_WARN,"NDK", "h<22||H>36 H = %d", (int) H);
//                hsv_channels[1].data[(i*hsv.rows)+j*3+0] = 0; //살색이 아니면 검정으로 변환
                hsv_channels[2].data[(i*hsv.rows)+j+0] = 0;
            }
        }
    }
        vector<Mat> channelslzm;

   //Mat gray=0.299*channels[2]+0.587*channels[1]+0.114*channels[0];
   // Changes the Red and Blue sites:

               __android_log_print(ANDROID_LOG_WARN,"NDK", "1");
//    CvMemStorage *storage = cvCreateMemStorage(0);
    CvSeq * contours = 0;
//
//    CvArr *tmp = &hsv_channels[2];
//
//    __android_log_print(ANDROID_LOG_WARN,"NDK", "2");
//
//    int count = cvFindContours(tmp, storage, &contour,sizeof(CvContour), CV_RETR_EXTERNAL , CV_CHAIN_APPROX_SIMPLE,cvPoint(0,0));
//
//
//    cv::findContours(hsv_channels[2], contours, CV_RETR_EXTERNAL,  // 외부 외곽선 검색
//                     CV_CHAIN_APPROX_NONE); // 각 외곽선의 모든 화소 탐색
    // 지정된 플래그 - 첫 번째는 외부 외곽선이 필요함을 나타내며 객체의 구멍을 무시
    // 두 번째 플래그는 외곽선의 형태를 지정 - 현재 옵션으로 벡터는 외곽선 내의 모든 화소 목록
    // CV_CHAIN_APPROX_SIMPLE 플래그로 하면 마지막 점이 수평 또는 수직, 대각선 외곽선에 포함됨
    // 다른 플래그는 간결한 표현을 얻기 위해 외곽선의 정교하게 연결된 근사치를 제공


    // 이전 영상으로 9개 외곽선을 contours.size()로 얻음
    // drawContours() 함수는 영상 내의 각 외곽선을 그릴 수 있는 함수
    // 하얀 영상 내 검은 외곽선 그리기


    __android_log_print(ANDROID_LOG_WARN,"NDK", "3");
    channelslzm.push_back(hsv_channels[0]);//h
    channelslzm.push_back(hsv_channels[1]);//s
    channelslzm.push_back(hsv_channels[2]);//v
    merge(channelslzm,mRgb);


    cvtColor(mRgb,mRgb,CV_HSV2RGB);
//
//    cv::drawContours(mRgb, contours,
//                     -1,    // 모든 외곽선 그리기
//                     cv::Scalar(0), // 검게
//                     2);    // 두께를 2로
    // 세 번째 파라미터가 음수라면 모든 외곽선이 그려짐
    // 반면 그려져야 하는 외곽선의 첨자를 지정할 수 있음
//
//    int i,j,k;
//
//    Mat *tmp2 = &mRgb;
//    __android_log_print(ANDROID_LOG_WARN,"NDK", "4");
//    for( i = 0; i< 4 ; i ++){
//        for( j = 0 ; j < contour->total; j++){
//            if( contour->total<200) continue;
//            __android_log_print(ANDROID_LOG_WARN,"NDK", "5");
//            double arc = cvArcLength( contour, CV_WHOLE_SEQ, -1);
//            CvPoint *point = (CvPoint*) cvGetSeqElem( contour, j);
//            //cvDrawCircle( contour
//            cvDrawContours(tmp2, contour, CV_RGB(0,255,0), CV_RGB(255,0,0),-1,2,8);
//        }
//        __android_log_print(ANDROID_LOG_WARN,"NDK", "6");
//        contour = contour->h_next;
//    }
//    __android_log_print(ANDROID_LOG_WARN,"NDK", "7");
//    mRgb = *tmp2;
//
//    cvClearSeq(contour);
//    cvReleaseMemStorage(&storage);



//    FastFeatureDetector detector(15, true);
//    detector.detect(hsv_channels[2], v);
//    for( unsigned int i = 0; i < v.size(); i++ )
//    {
//        const KeyPoint& kp = v[i];
//        circle(mRgb, Point(kp.pt.x, kp.pt.y), 10, Scalar(255,0,0,255));
//    }
//
    //cvtColor( hsv_channels[2], mRgb, CV_GRAY2RGB); //rgb를 hsv로 변환 CV_RGB2HSV

//
//        CvMemStorage* storage = cvCreateMemStorage(0);
//        CvSeq* imageKeypoints = 0;
//        CvSeq* imageDescriptors = 0;
//        CvSURFParams params = cvSURFParams(500,1);
//
//        cvExtractSURF(mRgb, 0, &imageKeypoints, &imageDescriptors, storage, params);
//
//        for(int i = 0 ; i< imageKeypoints->total; i++){
//                CvSURFPoint * point = (CvSURFPoint*) cvGetSeqElem(imageKeypoints,i);
//                center.x = cvRount(point->pt.x);
//                center.y = cvRount(point->pt.y);
//                cvCircle(mRgb, center, 2, Scalar(255,0,0,255), CV_FILLED);
//        }
//
//        cvClearSeq(imageDescriptors);
//        cvClearSeq(imageKeypoints);
//        cvReleaseMemStorage(&storage);

}
}