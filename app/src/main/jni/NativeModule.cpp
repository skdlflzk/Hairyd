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
#include <stdlib.h>
#include <math.h>

#include <map>

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
    CvHaarClassifierCascade *eye_cascade = (CvHaarClassifierCascade *) cvLoad(
            "/data/data/com.android.hairyd/files/haarcascade_eye.xml", 0, 0, 0);


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
JNIEXPORT double JNICALL Java_com_android_hairyd_NativeFunc_AAMfitting(JNIEnv *env, jobject,
                                                                             jlong addrGray,
                                                                             jlong addrRgba,
                                                                             jdoubleArray data,
                                                                             jdoubleArray averages) {  //들로네 삼각형 해시맵을 전달...

    __android_log_print(ANDROID_LOG_WARN, "NDK",
                        "ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡAAMfitting...");
/*
  초기화 및 double형 배열 C에 맞게 변형
 */
    Mat &mGr = *(Mat *) addrGray;
    Mat &mRgb = *(Mat *) addrRgba;

    jdouble *datum = (*env)->GetDoubleArrayElements(env, data, NULL);
    if (datum == NULL) {
        return 0;
    }
    jdouble *average = (*env)->GetDoubleArrayElements(env, averages, NULL);
    if (average == NULL) {
        return 0;
    }
    jsize len = (*env)->GetArrayLength(env, datum);


    double x1, x2, x3, y1,y2,y3, a, b, c, xn, yn;
    int triangle;
    int tsize = 0; //int tsize = delauney.size();

    bool p1,p2,p3;
    //TODO:탐색범위 제한
    double ep = 0;

    for (int i = 0; i < mGr.cols; i++) {
        for (int j = 0; j < mGr.rows; j++) {

            /*
                현재 위치를 affine 변환 후, 얼굴 영역인지 확인
            */


            triangle = -1;

            for(int k = 0; k < tsize ; k++) {   //현재 점이 입력영상의 들로네 삼각형 안에 있는지 확인

                x1 = 0; //x1 = datum[2 * delauney[triangle][0]]
                x2 = 0; //x2 = datum[2 * delauney[triangle][1]]
                x3 = 0;
                y1 = 0; //y1 = datum[2 * delauney[triangle][0]+1]
                y2 = 0;
                y3 = 0;


                if (((x3-x1)*(y1-y2)-(y3-y1)*(x1-x2))*((i-x1)*(y1-y2)-(j-y1)*(x1-x2)) < 0) continue;  //1과2 -> x,3
                if (((x1-x2)*(y-y3)-(y1-y3)*(x2-x3))*((i-x2)*(y2-y3)-(j-y2)*(x2-x3)) < 0) continue; //2,3 -> x,1
                if (((x2-x3)*(y3-y1)-(y3-y3)*(x3-x1))*((i-x3)*(y3-y1)-(j-y3)*(x3-x1)) < 0) continue;//3,1 -> x,2
//http://zockr.tistory.com/83
                triangle = k;
                break;
            }

//            if(triangle == -1){ continue;} //affine  변환된 점이 삼각형 내부의 점이 아님

            // k번째 입력영상 들로네 삼각형의 좌표 지정

            c = ((j - y1) - (i - x1) * (y2 - y1)) /
                (y3 - y1 - x3 * y3 + x3 * y1 + x1 * y2 - x1 * y1);
            b = (i - x1) - c * (x3 - x1);
            a = 1 - (b + c);


            // 평균영상으로 affine 변환

            //xn = a*average[2*delauney[triangle][0]] + b*average[2*delauney[triangle][1]] + c*average[2*delauney[triangle][2]];
            //yn = a*average[2*delauney[triangle][0]+1] + b*average[2*delauney[triangle][1]+1] + c*average[2*delauney[triangle][2]+1];


            uchar gn = (uchar) mGr.data[(i * hsv.rows) + j]; //1채널 +0 필요없음
            uchar gm = (uchar) YCrCb_channels[2].data[(i * hsv.rows) + j + 0];

            ep += (gn-gm)*(gn-gm);

        }
    }


    return ep;

}

JNIEXPORT double JNICALL Java_com_android_hairyd_NativeFunc_drawPoint(JNIEnv *, jobject,
                                                                    jlong addrRgba,
                                                                    jfloat x, jfloat y) {


    __android_log_print(ANDROID_LOG_WARN, "NDK",
                        "ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡdrawing (%f,%f)", x, y);


    Mat &mRgb = *(Mat *) addrRgba;


    circle(mRgb, cvPoint((int) x, (int) y), 2, cvScalar(255, 0, 0));
/*
 *
 */

}


JNIEXPORT Mat JNICALL Java_com_android_hairyd_NativeFunc_getCols(JNIEnv *, jobject,
                                                                      jintArray inputImg, jdoubleArray inputShape, jdoubleArray baseShape) {

    jint *cintArray = env->GetIntArrayElements(inputImg, nullptr);
    jsize length = env->GetArrayLength(inputImg);

// 입력영상의 너비 높이
    int width = 0, height = 0;

    Mat InputImage(width, height, CV_8UC4, 0);  //받아온 intArray 를 담을 공간

    for (int i = 0; i < height ; i++) {
      for (int j = 0; j < width; j++) {


          InputImage.data[(i * width) + j + 0] = mcintArray[i * width + j + 0];
          InputImage.data[(i * width) + j + 1] = mcintArray[i * width + j + 1];
          InputImage.data[(i * width) + j + 2] = mcintArray[i * width + j + 2];

        }
    }

    env->ReleaseIntArrayElements(inputImg, cintArray, 0);
    //https://skyfe79.gitbooks.io/jni-tutorial/content/chapter17.html

    jsize length = env->GetArrayLength(inputShape);
    if(length == 0)
        return nullptr;

    //convert java-array to c-array jdouble[]
    jdouble *cdoubleArray = env->GetDoubleArrayElements(inputShape, nullptr);

    double datum[152] ;  //
    //add value

    for(int i = 0; i<length; i++) {
        datum[i]= cdoubleArray[i];
    }

    length = env->GetArrayLength(inputShape);

    if(length == 0)
        return nullptr;

    cdoubleArray = env->GetDoubleArrayElements(baseShape, nullptr);

    double average[152] ;

    for(int i = 0; i<length; i++) {
        average[i]= cdoubleArray[i];
    }
//TODO    env->ReleaseDoubleArrayElements(inputArray, cdoubleArray, 0); 해줘야되지않나? 메모리누수
//    env->ReleaseDoubleArrayElements(doubleArray, cdoubleArray, 0);


//입력영상 흑백

    cvtColor(InputImage, InputImage, CV_RGB2GRAY);

//평균영상 불러오기
    FILE* file = fopen("/sdcard//Phairy/image/i000qa.jpg" , "rb" );//전역변수로 어플 실행 마다 한번만.

    Mat baseImage = imread(file, IMREAD_GRAYSCALE);

//들로네 정보 파싱

    FILE* pFile = fopen("/sdcard//Phairy/dela/dela.txt" , "rb" );//전역변수로 어플 실행 마다 한번만.
    char str [1000];

    fscanf (pFile, "%s", str);
    fclose (pFile);


    char *token1 = NULL;
    char *token2 = NULL;
//    char str1[] = "\n";
    char str2[] = "\t";     // 모두 \t로 변경

    token1 = strtok( str, str2 );

    int p1,p2,p3;
    std::map<int, int> delauney;    //전역변수로 어플 실행 마다 한번만.
    int tri=0;
    while( token1 != NULL )
    {
        token2 = strtok( token1, str2 );
        p1 = (int) *token2;
        delauney[tri*10+0] = p1;

        token2 = strtok( NULL, str2 );
        p2 = (int) *token2;
        delauney[tri*10+1] = p2;

        token2 = strtok( NULL, str2 );
        p3 = (int) *token2;
        delauney[tri*10+2] = p3;

        tri++;
        token1 = strtok( NULL, str2 );
    }

// delauney를 이용해 inputSh->baseSh affine 변환  이미지 마다 확인

    int cols = 0;//getCols();
    int rows = 0;//getRows();


    double x1 = 0, x2 = 0, x3 = 0, y1 = 0, y2 = 0, y3 = 0, a = 0, b = 0, c = 0, xn1 = 0, xn2 = 0, xn3 = 0, yn1 = 0, yn2 = 0, yn3 = 0, x, y;
    int triangle;
    int tsize = delauney.size(); //int tsize = delauney.size();

    //TODO:탐색범위 제한
    double rp = 0;

    for (int i = 0; i < cols; i++) {
        for (int j = 0; j < rows; j++) {

            /*
                현재 위치를 affine 변환 후, 얼굴 영역인지 확인
            */
            //현재 점(i,j)가 입력영상의 들로네 삼각형 안에 있는지 확인
            triangle = -1;

            for (int k = 0; k < delauney.size(); k++) {

                x1 = datum[2 * delauney[k*10+0]];
                x2 = datum[2 * delauney[k*10+1]];
                x3 = datum[2 * delauney[k*10+2]];

                y1 = datum[2 * delauney[k*10+0]+0];
                y2 = datum[2 * delauney[k*10+1]+1];
                y3 = datum[2 * delauney[k*10+2]+2];


                if (((x3 - x1) * (y1 - y2) - (y3 - y1) * (x1 - x2)) * ((i - x1) * (y1 - y2) - (j - y1) * (x1 - x2)) < 0)
                    continue;  //1과2 -> x,3
                if (((x1 - x2) * (y2 - y3) - (y1 - y3) * (x2 - x3)) * ((i - x2) * (y2 - y3) - (j - y2) * (x2 - x3)) < 0)
                    continue; //2,3 -> x,1
                if (((x2 - x3) * (y3 - y1) - (y3 - y3) * (x3 - x1)) * ((i - x3) * (y3 - y1) - (j - y3) * (x3 - x1)) < 0)
                    continue;//3,1 -> x,2
//http://zockr.tistory.com/83
                triangle = k;       //현재 k번째 삼각형 내부에 있는 것!
                break;
            }

    // k번째 입력영상 들로네 삼각형의 좌표 지정


            c = ((j - y1) - (i - x1) * (y2 - y1)) / (y3 - y1 - x3 * y3 + x3 * y1 + x1 * y2 - x1 * y1);
            b = (i - x1) - c * (x3 - x1);
            a = 1 - (b + c);


            x = a * xn1 + b * xn2 + c * xn3;
            y = a * yn1 + b * yn2 + c * yn3;
// 얻어온 좌표로 부터 r(p) 구해오기
            rp += (InputImage.data[i][j]-baseImage.data[x][y])*(InputImage.data[i][j]-baseImage.data[x][y]);//rp 받아오기
        }
    }
//if rp < w까지 반복하여
    __android_log_print(ANDROID_LOG_WARN, "NDK","r(p)  = %f",rp);

    if(rp < 100){ return data; }

    return data;

}



JNIEXPORT double JNICALL Java_com_android_hairyd_NativeFunc_getRows(JNIEnv *, jobject,
                                                                    jlong addrGray) {

    _finddata_t fd;
    long handle;
    int result = 1;
    handle = _findfirst(".\\*.*", &fd);  //현재 폴더 내 모든 파일을 찾는다.

    if (handle == -1)
    {
        printf("There were no files.\n");
        return;
    }

    String name="/sdcard//Phairy/image/";
    while (result != -1)
    {

        FILE* file = fopen( name + fd.name , "rb" );//전역변수로 어플 실행 마다 한번만.
        Mat baseImage = imread(file, IMREAD_GRAYSCALE);



        result = _findnext(handle, &fd);
    }

    _findclose(handle);

    Mat &mGr = *(Mat *) addrGray;

    return mGr.rows;

}

JNIEXPORT void JNICALL Java_com_android_hairyd_NativeFunc_getPCA(JNIEnv *, jobject,
                                                                 jlong left,
                                                                 jlong right, jint seek) {

    __android_log_print(ANDROID_LOG_WARN, "NDK","starting getPCA()...");
//파일 입력
    /*

    for(int i=0; i< width; i++) {
    File file;
    String filePath = App::GetInstance()->GetAppRootPath() + L"data/muct76-opencv.csv";   //assets?muct76-opencv

    result r = file.Construct(filePath, "r");

    FileAttributes att;
    r = File::GetAttributes(filePath, att);

    long long size = att.GetFileSize();

    ByteBuffer buf;
    r = buf.Construct(size + 1);

    r = file.Read(buf);

    String str;
    str += (char*)buf.GetPointer();

//  파싱

    String token = "\n";
    String token2 = ",";

    StringTokenizer strTok(str,token);
    String tokStr;

    while(strTok.HasMoreTokens()){
        strTok.GetNextToken(tokStr);

        StringTokenizer strTok2(tokStr,token2);

        int itemNum = 0;
        String stationInfrom;
        while(strTok2.HasMoreTokens()){
            itemNum++;
            strTok2.GetNextToken(stationInfrom);

            if(itemNum == 1){
                busNameList->Add(stationInfrom);
            }else if(itemNum == 2){
                busNumList->Add(stationInfrom);
                itemNum=0;
            }
        }
    }


    flaot ** data = matrix(n, m); //n개 세트의 m/2개 좌표
    __android_log_print(ANDROID_LOG_WARN, "NDK",
                        "ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡreturning...");
                        */

}
}


