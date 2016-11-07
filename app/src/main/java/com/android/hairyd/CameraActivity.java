package com.android.hairyd;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;


import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import Jama.EigenvalueDecomposition;

public class CameraActivity extends Activity {

    String TAG = Start.TAG;
    private static final int RQS_LOADIMAGE = 1;
    private Button btnLoad, btnDetFace;
    private ImageView imgView;
    private Bitmap myBitmap;

    Integer width;
    Integer height;

    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;

    Camera.Size size;           //MUCT는 세로 480x640 이미지임.

    Camera mCamera = null;
    double[] alignDatum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cam_activity);

        makeFile();

        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
//
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceView.setClickable(true);
        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });


        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(surfaceListener);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


        btnLoad = (Button) findViewById(R.id.btnLoad);
        btnDetFace = (Button) findViewById(R.id.btnDetectFace);
        imgView = (ImageView) findViewById(R.id.imgview);

        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, RQS_LOADIMAGE);
            }
        });

        btnDetFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (myBitmap != null) {
                    int size = detectFace();
                    Toast.makeText(getApplicationContext(), "Done ," + size, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "data == null", Toast.LENGTH_LONG).show();
                }
            }
        });


        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {

//                Camera.Parameters parameters = camera.getParameters();
//                List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();


                width = size.width;//parameters.getPreviewSize().width;
                height = size.height;//parameters.getPreviewSize().height;
                int[] mIntArray = new int[width * height];

                // Decode Yuv data to integer array
                decodeYUV420SP(mIntArray, bytes, width, height);

                //Initialize the bitmap, with the replaced color
                myBitmap = Bitmap.createBitmap(mIntArray, width, height, Bitmap.Config.RGB_565);

                Matrix rotateMatrix = new Matrix();
                rotateMatrix.setScale(-1, 1);

                rotateMatrix.postRotate(90);

                myBitmap = Bitmap.createBitmap(myBitmap, 0, 0, myBitmap.getWidth(), myBitmap.getHeight(), rotateMatrix, false);

                int facecount = detectFace();

                Log.i(TAG, "CameraActivity: Detecting... ," + facecount);


            }
        });

        Button b = (Button) findViewById(R.id.test);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pcaAnalize();
            }
        });
    }


    @Override
    protected void onPause() {
        if (mCamera != null) {

            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RQS_LOADIMAGE
                && resultCode == RESULT_OK) {

            if (myBitmap != null) {
                myBitmap.recycle();
            }

            try {
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                myBitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                imgView.setImageBitmap(myBitmap);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /*
    reference:
    https://search-codelabs.appspot.com/codelabs/face-detection
     */
    private int detectFace() {

        //Create a Paint object for drawing with
        Paint myRectPaint = new Paint();
        myRectPaint.setStrokeWidth(5);
        myRectPaint.setColor(Color.RED);
        myRectPaint.setStyle(Paint.Style.STROKE);

        //Create a Canvas object for drawing on
        Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);

        Log.i("Phairy", "detectFace_ loaded successfully. 크기 " + myBitmap.getWidth() + ", " + myBitmap.getHeight());

        Canvas tempCanvas = new Canvas(tempBitmap);
        tempCanvas.drawBitmap(myBitmap, 0, 0, null);

        //Detect the Faces
        FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext()).setLandmarkType(FaceDetector.ALL_LANDMARKS).build();

        //!!!
        //Cannot resolve method setTrackingEnabled(boolean)
        //skip for now
        //faceDetector.setTrackingEnabled(false);

        Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
        SparseArray<Face> faces = faceDetector.detect(frame);

        Paint landmarksPaint = new Paint();
        landmarksPaint.setStrokeWidth(10);
        landmarksPaint.setColor(Color.RED);
        landmarksPaint.setStyle(Paint.Style.STROKE);

        //Draw Rectangles on the Faces
        for (int i = 0; i < faces.size(); i++) {
            Face thisFace = faces.valueAt(i);
            float x1 = thisFace.getPosition().x;
            float y1 = thisFace.getPosition().y;
            float x2 = x1 + thisFace.getWidth();
            float y2 = y1 + thisFace.getHeight();
            tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);

            List<Landmark> landmarks = thisFace.getLandmarks();
            for (int l = 0; l < landmarks.size(); l++) {
                PointF pos = landmarks.get(l).getPosition();
                tempCanvas.drawPoint(pos.x, pos.y, landmarksPaint);
            }

            Log.i("Phairy", "y각 " + thisFace.getEulerY() + "도, z각 " + thisFace.getEulerZ() + "도 너비 = " + thisFace.getWidth() + ", 높이 = " + thisFace.getHeight() + ", 특징점 개수 " + landmarks.size());
        }
        imgView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));

        return faces.size();
    }


    static public void decodeYUV420SP(int[] rgba, byte[] yuv420sp, int width,
                                      int height) {
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0;
                else if (r > 262143) r = 262143;
                if (g < 0) g = 0;
                else if (g > 262143) g = 262143;
                if (b < 0) b = 0;
                else if (b > 262143) b = 262143;

                // rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) &
                // 0xff00) | ((b >> 10) & 0xff);
                // rgba, divide 2^10 ( >> 10)

//                rgba[yp] = ((r << 14) & 0xff000000) | ((g << 6) & 0xff0000)
//                        | ((b >> 2) | 0xff00);
                rgba[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) &
                        0xff00) | ((b >> 10) & 0xff);

            }
        }
    }

    static public void encodeYUV420SP(byte[] yuv420sp, int[] rgba,
                                      int width, int height) {
        final int frameSize = width * height;

        int[] U, V;
        U = new int[frameSize];
        V = new int[frameSize];

        final int uvwidth = width / 2;

        int r, g, b, y, u, v;
        for (int j = 0; j < height; j++) {
            int index = width * j;
            for (int i = 0; i < width; i++) {
                r = (rgba[index] & 0xff000000) >> 24;
                g = (rgba[index] & 0xff0000) >> 16;
                b = (rgba[index] & 0xff00) >> 8;

                // rgb to yuv
                y = (66 * r + 129 * g + 25 * b + 128) >> 8 + 16;
                u = (-38 * r - 74 * g + 112 * b + 128) >> 8 + 128;
                v = (112 * r - 94 * g - 18 * b + 128) >> 8 + 128;

                // clip y
                yuv420sp[index++] = (byte) ((y < 0) ? 0 : ((y > 255) ? 255 : y));
                U[index] = u;
                V[index++] = v;
            }
        }
    }

    private SurfaceHolder.Callback surfaceListener = new SurfaceHolder.Callback() {
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // TODO Auto-generated method stub

            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // TODO Auto-generated method stub
            // 표시할 영역의 크기를 알았으므로 해당 크기로 Preview를 시작합니다.
            Log.i(TAG, "mCameraActivity : surface creating! 카메라 null이 아니면 미리보기 시작");

            if (mCamera != null) {
                try {

                    Camera.Parameters param = mCamera.getParameters();
                    List<Camera.Size> sizeList = param.getSupportedPreviewSizes();
                    int min = 1000;
                    Integer width = 0;
                    Integer height = 0;
                    for (int i = 0; i < sizeList.size(); i++) {
                        Camera.Size tempSize = sizeList.get(i);


                        if (tempSize.width < min) {
                            size = tempSize;
                            width = size.width;//parameters.getPreviewSize().width;
                            height = size.height;//parameters.getPreviewSize().height;
                            min = size.width;
                        }

                    }

                    param.setPreviewFrameRate(30);
                    param.setPreviewSize(size.width, size.height);

                    Log.i(TAG, "CameraActivity: minimum  Camera Size = " + min);

                    param.setPreviewFrameRate(30);
                    param.setPreviewSize(width, height);

                    mCamera.setParameters(param);
                    mCamera.setDisplayOrientation(90);
                    mCamera.setPreviewDisplay(holder);

                } catch (Exception e) {

                    e.printStackTrace();
                    Log.i(TAG, "mCameraActivity : 카메라 미리보기 화면 에러");

                }
            } else {

                Log.e(TAG, "mCameraActivity : mCamera not available");
            }

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // TODO Auto-generated method stub
            if (mCamera != null) {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setPreviewSize(width, height);
                mCamera.startPreview();
                Log.i(TAG, "mCameraActivity : mCamera startPreview");
            }
        }
    };


    public void pcaAnalize() { //(RGBA input){

        String data = "name,tag,x00,y00,x01,y01,x02,y02,x03,y03,x04,y04,x05,y05,x06,y06,x07,y07,x08,y08,x09,y09,x10,y10,x11,y11,x12,y12,x13,y13,x14,y14,x15,y15,x16,y16,x17,y17,x18,y18,x19,y19,x20,y20,x21,y21,x22,y22,x23,y23,x24,y24,x25,y25,x26,y26,x27,y27,x28,y28,x29,y29,x30,y30,x31,y31,x32,y32,x33,y33,x34,y34,x35,y35,x36,y36,x37,y37,x38,y38,x39,y39,x40,y40,x41,y41,x42,y42,x43,y43,x44,y44,x45,y45,x46,y46,x47,y47,x48,y48,x49,y49,x50,y50,x51,y51,x52,y52,x53,y53,x54,y54,x55,y55,x56,y56,x57,y57,x58,y58,x59,y59,x60,y60,x61,y61,x62,y62,x63,y63,x64,y64,x65,y65,x66,y66,x67,y67,x68,y68,x69,y69,x70,y70,x71,y71,x72,y72,x73,y73,x74,y74,x75,y75\n" +
                "i000qa-fn,0000,201,348,201,381,202,408,209,435,224,461,241,483,264,498,292,501,319,493,338,470,353,448,363,423,367,395,366,371,357,344,355,316,340,311,325,318,309,328,327,324,342,317,217,328,231,323,250,327,269,333,251,334,233,331,229,345,240,337,262,349,242,352,241,344,346,337,330,330,318,341,334,344,330,336,280,344,278,381,264,399,273,406,293,409,316,399,321,392,304,376,296,342,279,402,310,399,251,431,268,427,284,425,293,425,302,423,316,425,329,426,320,442,309,451,295,454,278,452,263,442,277,440,293,442,313,437,313,429,293,432,277,431,293,436,295,395,234.5,341,251,343,252,350.5,235.5,348.5,338,333.5,324,335.5,326,342.5,340,340.5\n" +
                "i000qb-fn,0000,162,357,157,387,160,418,167,446,182,477,199,499,226,514,259,517,280,507,295,484,307,462,318,439,324,415,326,386,319,353,0,0,313,322,296,327,277.6,341.7,299,337,313,332,193,337,211,333,227,337,240,344,226,345,210,342,196,355,211,349,226,359,209,363,210,355,313,349,298,343,285,353,299,356,298,348,249,361,249,390,234,409,240,417,267,422,282,415,285,403,276,387,270,361,250,416,280,414,213,445,232,441,249,439,259,440,271,439,282,439,294,440,284,457,273,467,258,468,243,465,229,458,243,455,259,456,273,454,273,444,259,447,243,446,259,451,270,411,203.5,352,218.5,354,217.5,361,202.5,359,305.5,346,291.5,348,292,354.5,306,352.5\n" +
                "i000qc-fn,0000,212,352,203,380,200,407,211,439,224,479,243,498,270,509,303,511,319,501,326,483,330,466,346,436,355,415,361,389,354,346,0,0,354,319,342,325,325.6,332.9,344,334,356,328,250,334,267,331,280,333,294,339,280,341,267,339,251,352,265,345,277,354,264,358,262,351,350,346,339,341,329,349,341,353,340,344,304,356,309,384,285,401,295,414,316,418,0,0,0,0,330,384,321,356,304,411,0,0,262,441,286,437,303,434,310,435,320,434,328,433,334,435,327,449,320,459,305,462,291,460,278,454,288,451,308,451,319,447,320,438,308,441,290,441,307,446,327,405,258,348.5,271,349.5,270.5,356,257.5,355,344.5,343.5,334,345,335,351,345.5,349.5\n" +
                "i000qd-fn,0000,157,316,155,348,154,373,159,407,172,435,187,463,212,479,242,482,275,473,296,452,311,430,319,406,325,378,325,350,314,325,314,296,300,286,281,291,264,302,283,298,301,292,175,296,193,290,212,293,229,301,211,299,194,295,186,317,198,305,218,321,198,321,199,314,304,316,287,307,273,319,291,321,288,313,238,313,234,346,220,366,228,374,249,377,271,369,276,363,260,342,254,313,237,368,263,368,204,405,223,396,240,393,249,393,257,392,271,398,284,406,274,416,262,421,247,424,232,419,217,413,230,410,246,414,267,411,268,403,248,401,231,400,247,408,251,358,192,311,208,313,208,321,192,319,295.5,311.5,280,313,282,320,297.5,318.5";

//        mLogger.debug("파싱 시작");

//        InputStream inputStream = getResources().openRawResource(R.raw.data2);
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        mLogger.debug("파일 지명");

//        try {
//            int i = inputStream.read();
//            while (i != -1) {
//                byteArrayOutputStream.write(i);
//                i = inputStream.read();
//            }
//
//            data = new String(byteArrayOutputStream.toByteArray(), "UTF-8");
//            inputStream.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        mLogger.debug("data 파일 읽어옴");
        StringTokenizer entertoken = new StringTokenizer(data, "\n");
        int dataSize = entertoken.countTokens() - 1;

//        String[] strs = tabtoken.nextToken().split(":");

        entertoken.nextToken();
        StringTokenizer tabtoken;

        double[][] A = new double[dataSize][76 * 2];// dataSize < 76*2


        for (int i = 0; i < dataSize; i++) {

            tabtoken = new StringTokenizer(entertoken.nextToken(), ",");    //lat
            tabtoken.nextToken();//name
            tabtoken.nextToken();//tag

            for (int j = 0; j < 76; j++) {
//                    double one = Double.parseDouble(tabtoken.nextToken());
//                    double two = Double.parseDouble(tabtoken.nextToken());
//                    Log.e(TAG, "CameraActivity:  "+one + " " + two );

                A[i][2 * j] = Double.parseDouble(tabtoken.nextToken());
                A[i][2 * j + 1] = Double.parseDouble(tabtoken.nextToken());
            }

            //프로크루스테스 정렬
            alignDatum = procrustes(A[i]);

            for (int j = 0; j < 76; j++) {

                A[i][2 * j] = alignDatum[2 * j];
                A[i][2 * j + 1] = alignDatum[2 * j + 1];
            }

        }

//        평균 구하기
        Jama.Matrix Data = new Jama.Matrix(A);

        double[][] Edouble = new double[1][76 * 2];
        Jama.Matrix E = new Jama.Matrix(Edouble);


        double temp = 0;
        for (int i = 0; i < 76 * 2; i++) {

            for (int j = 0; j < dataSize; j++) {

                temp += Data.get(j, i);

            }
            temp = temp / dataSize;
            E.set(0, i, temp);
            Edouble[0][i] = temp;
            temp = 0;
        }

        String a = "";

        for (int j = 0; j < 76 * 2; j++) {
//                    Log.e(TAG, j+"열 시작");
            a = a + " " + E.get(0, j);
//                    Log.e(TAG, ""+M.get(i,j));
        }

        Log.e(TAG, "(" + a + ")");

        a = "(";

//ㅡㅡㅡㅡㅡㅡㅡㅡㅡdataSize*dataSize 차원의 공분산행렬 구하기
        double[][] coveri = new double[dataSize][dataSize];
        Jama.Matrix C = new Jama.Matrix(coveri);

        for (int i = 0; i < dataSize; i++) {//dataSize
            for (int j = 0; j < dataSize; j++) {   //pointSize * 2
                for (int k = 0; k < 76 * 2; k++) {
                    temp = temp + (Data.get(i, k) - E.get(0, k)) * (Data.get(j, k) - E.get(0, k));
                }
                temp = temp / (dataSize - 1);   //왜 dataSize-1인지는 모르겠지만...
                C.set(i, j, temp);
                temp = 0;
            }
        }

//normalize? 필요할 것 같긴 하다...

//ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ고유벡터 뽑아내기


        EigenvalueDecomposition ed = C.eig();


        Log.e(TAG, "CameraActivity: 공분산 C의 col수 = " + C.getColumnDimension() + " row수 = " + C.getRowDimension());


        a = "(";
        try {
            for (int i = 0; i < C.getColumnDimension(); i++) {
//                Log.e(TAG, i+"행 시작");
                for (int j = 0; j < C.getRowDimension(); j++) {
//                    Log.e(TAG, j+"열 시작");
                    a = a + " " + C.get(i, j);
//                    Log.e(TAG, ""+M.get(i,j));
                }

                a += ")";
                Log.e(TAG, "" + a);
                a = "(";
            }
        } catch (Exception exx) {
            exx.printStackTrace();
            Log.e(TAG, "CameraActivity:1 error");

        }

//ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ고유벡터 크기순 정렬하기

        Log.e(TAG, "CameraActivity:ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ이하 EigenValueㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ");
        Jama.Matrix x = ed.getD();

        double Evalues[] = new double[dataSize];
        int Erank[] = new int[dataSize];

        Log.e(TAG, "CameraActivity: x value col = " + x.getColumnDimension() + " row = " + x.getRowDimension());

        for (int i = 0; i < x.getRowDimension(); i++) {
            Evalues[i] = x.get(i, i);
        }
        a = "(";

        temp = 0;

        for (int i = 0; i < dataSize; i++) {      //Evalues 내림차순 정렬
            for (int j = 0; j < dataSize; j++) {

                if (Evalues[i] < Evalues[j]) {
                    temp = Evalues[j];
                    Evalues[j] = Evalues[i];
                    Evalues[i] = temp;
                }
            }
        }

        for (int i = 0; i < dataSize; i++) {      //Evalues 내림차순 정렬
            for (int j = 0; j < dataSize; j++) {

                if (Evalues[i] == x.get(j, j)) {
                    Erank[i] = j;
                    break;
                }

            }
        }


        Log.e(TAG, "CameraActivity:ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ이하 EigenVectorㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ");
        Log.e(TAG, "CameraActivity: x vector col = " + x.getColumnDimension() + " row = " + x.getRowDimension());

        x = ed.getV();
        Jama.Matrix AlignedEigenVector = new Jama.Matrix(dataSize, dataSize);

        for (int i = 0; i < dataSize; i++) {      //EigenVector를 고유값의 크기에 따라 재정렬.
            for (int j = 0; j < dataSize; j++) {
                AlignedEigenVector.set(i, j, x.get(Erank[i], j));
            }
        }

        Log.e(TAG, "CameraActivity: AlignedEigenVector col = " + x.getColumnDimension() + " row = " + x.getRowDimension());
        a = "(";
        x = ed.getV();

        try {
            for (int i = 0; i < x.getColumnDimension(); i++) {
                for (int j = 0; j < x.getRowDimension(); j++) {
                    a = a + " " + AlignedEigenVector.get(i, j);
//                    Log.e(TAG, ""+x.get(i,j));
                }
                a += ")";
                Log.e(TAG, "" + a);
                a = "(";
            }
        } catch (Exception exx) {
            exx.printStackTrace();
            Log.e(TAG, "CameraActivity:3 error");

        }

        HashMap<Integer, double[]> delauney = delauney(Edouble[0]);

        for (int i = 0; i < delauney.size(); i++) {

            Log.e(TAG, i + "번째 삼각형 : " + delauney.get(i)[0] + " , " + delauney.get(i)[1] + ", " + delauney.get(i)[2]);

        }

        int input = 0;

        /*
         grayScaling;
        */

        /*
         //알고리즘
         x = x0 + pi*bi;
       */


    }


    public double[] procrustes(double[] datum) {

        //미리 계산된 평균 average 값
        double[] average = {201, 348, 201, 381, 202, 408, 209, 435, 224, 461, 241, 483, 264, 498, 292, 501, 319, 493, 338, 470, 353, 448, 363, 423, 367, 395, 366, 371, 357, 344, 355, 316, 340, 311, 325, 318, 309, 328, 327, 324, 342, 317, 217, 328, 231, 323, 250, 327, 269, 333, 251, 334, 233, 331, 229, 345, 240, 337, 262, 349, 242, 352, 241, 344, 346, 337, 330, 330, 318, 341, 334, 344, 330, 336, 280, 344, 278, 381, 264, 399, 273, 406, 293, 409, 316, 399, 321, 392, 304, 376, 296, 342, 279, 402, 310, 399, 251, 431, 268, 427, 284, 425, 293, 425, 302, 423, 316, 425, 329, 426, 320, 442, 309, 451, 295, 454, 278, 452, 263, 442, 277, 440, 293, 442, 313, 437, 313, 429, 293, 432, 277, 431, 293, 436, 295, 395, 234.5, 341, 251, 343, 252, 350.5, 235.5, 348.5, 338, 333.5, 324, 335.5, 326, 342.5, 340, 340.5};
//눈 위치는 (x31,y31)과 (x36,y36)

        //프로크루스테스 정렬

        int size = datum.length;
        /*

        translation

        */
        double xm = 0, ym = 0, xa = 0, ya = 0;
        for (int i = 0; i < (size / 2); i++) {         // 모든 (x,y) 점의 평균을 구함
            xm = xm + datum[2 * i];
            ym = ym + datum[2 * i + 1];
            xa = xa + average[2 * i];
            ya = ya + datum[2 * i + 1];
        }
        xm = xm / size;
        ym = ym / size;
        xa = xa / size;
        ya = ya / size;

        for (int i = 0; i < (datum.length / 2); i++) {         // 원점으로 이동
            datum[2 * i] = datum[2 * i] - xm;
            datum[2 * i + 1] = datum[2 * i + 1] - ym;
            average[2 * i] = average[2 * i] - xa;
            average[2 * i + 1] = average[2 * i + 1] - ya;
        }

        /*

        scaling

        */
        double s = 0;
        double sa = 0;
        double rate = 0;
        for (int i = 0; i < (size / 2); i++) {
            s = s + datum[2 * i] * datum[2 * i] + datum[2 * i + 1] * datum[2 * i + 1];
            sa = sa + average[2 * i] * average[2 * i] + average[2 * i + 1] * average[2 * i + 1];
        }
        s = Math.sqrt(s);
        sa = Math.sqrt(sa);
        rate = s / sa;

        /* 비율을 조절할까? 조절할 필요는?
        for(int i = 0; i < (datum.length/2) ; i++){
            datum[2*i] = datum[2*i]/rate;
            datum[2*i+1] = datum[2*i+1]/rate;
            average[2*i] = average[2*i] - xa;
            average[2*i+1] = average[2*i+1] - ya;
        }
        */

        /*

        rotation        //3차원일때는 SVD를 이용하여 rotation mat R을 찾는것이 쉬울것

        */

        double theta = 0;
        double u = 0, v = 0;

        double tempx = 0, tempy = 0;
        for (int i = 0; i < (size / 2); i++) {
            u = Math.atan2(datum[2 * i + 1], datum[2 * i]);
            v = Math.atan2(average[2 * i + 1], average[2 * i]);
            theta = theta + v - u;
        }

        theta = theta / size;

        for (int i = 0; i < (size / 2); i++) {

            tempx = datum[2 * i];
            tempy = datum[2 * i + 1];

            //회전행렬 변환
            datum[2 * i] = tempx * Math.cos(theta) + tempy * Math.sin(theta);
            datum[2 * i + 1] = tempx * (-Math.sin(theta)) + tempy * Math.cos(theta);

        }

//
//
//        /*
//
//        Align
//
//         */
//
//        for(int i = 0; i < (size/2) ; i++) {         // 원점으로 이동
//            datum[2 * i]  = (datum[2 * i] - xm)/s;
//            datum[2 * i+1] = (datum[2 * i+1] - ym)/s;
//        }
//
//        Bundle ret = new Bundle();
//        ret.putDouble("rotation", theta);
//        ret.putDouble("scaling",s);
//        ret.putDouble("Xtranslation",xm);
//        ret.putDouble("Ytranslation",ym);
//
//        ret.putDoubleArray("datum",datum);


        return datum;
    }


    public int getDelauney(HashMap<Integer, int[]> delauney, double[] datum, double[] average) {

        NativeFunc nativeFunc = new NativeFunc();
        //       nativeFunc.FindFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr(), seek);

        int cols = 0;//getCols();
        int rows = 0;//getRows();


        double x1 = 0, x2 = 0, x3 = 0, y1 = 0, y2 = 0, y3 = 0, a = 0, b = 0, c = 0, xn1 = 0, xn2 = 0, xn3 = 0, yn1 = 0, yn2 = 0, yn3 = 0, x, y;
        int triangle;
        int tsize = 0; //int tsize = delauney.size();

        //TODO:탐색범위 제한
        double ep = 0;
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {

            /*
                현재 위치를 affine 변환 후, 얼굴 영역인지 확인
            */

                triangle = -1;

                for (int k = 0; k < delauney.size(); k++) {   //현재 점이 입력영상의 들로네 삼각형 안에 있는지 확인

                    x1 = datum[2 * delauney.get(k)[0]];
                    x2 = datum[2 * delauney.get(k + 1)[0]];
                    x3 = datum[2 * delauney.get(k + 2)[0]];

                    y1 = datum[2 * delauney.get(k)[0] + 1];
                    y2 = datum[2 * delauney.get(k + 1)[0] + 1];
                    y3 = datum[2 * delauney.get(k + 2)[0] + 1];


                    if (((x3 - x1) * (y1 - y2) - (y3 - y1) * (x1 - x2)) * ((i - x1) * (y1 - y2) - (j - y1) * (x1 - x2)) < 0)
                        continue;  //1과2 -> x,3
                    if (((x1 - x2) * (y2 - y3) - (y1 - y3) * (x2 - x3)) * ((i - x2) * (y2 - y3) - (j - y2) * (x2 - x3)) < 0)
                        continue; //2,3 -> x,1
                    if (((x2 - x3) * (y3 - y1) - (y3 - y3) * (x3 - x1)) * ((i - x3) * (y3 - y1) - (j - y3) * (x3 - x1)) < 0)
                        continue;//3,1 -> x,2
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


                x = a * xn1 + b * xn2 + c * xn3;
                y = a * yn1 + b * yn2 + c * yn3;

                //ep 받아오기

            }
        }

        return 0;
    }


    public HashMap delauney(double[] datum) {

        int size = datum.length;
        double x1, y1, x2, y2, x3, y3;
        double x, y, c1, c2;
        double r2;
        boolean pass = true;


        HashMap<Integer, int[]> del = new HashMap<>();
        int[] triangle = new int[3];

        String dela = "";
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {


                    if (i == j || j == k || k == i) break;

                    x1 = datum[2 * i];
                    x2 = datum[2 * j];
                    x3 = datum[2 * k];
                    y1 = datum[2 * i + 1];
                    y2 = datum[2 * j + 1];
                    y3 = datum[2 * k + 1];

                    c1 = (y1 + y2) / 2 + ((x2 - x1) * (x2 + x1) / 2) / (y2 - y1);
                    c2 = (y1 + y3) / 2 + ((x3 - x1) * (x3 + x1) / 2) / (y3 - y1);

                    x = (c2 - c1) * (x2 - x1) * (x3 - x1) / ((y2 - y1) * (x3 - x1) - (y3 - y1) * (x2 - x1));
                    y = (y2 - y1) * x / (x2 - x1) + c1;

                    r2 = ((x - x1) * (x - x1) + (y - y1) * (y - y1));


                    for (int l = 0; l < size; l++) {
                        if (l != i && l != j && l != k) {
                            if (r2 > (x - datum[2 * l]) * (x - datum[2 * l]) + (y - datum[2 * l + 1]) * (y - datum[2 * l + 1])) {
                                pass = false;
                            }
                        }
                    }

                    if (pass) { //들로네 삼각형 추가
                        int temp;
                        if (i > j) {
                            temp = j;
                            j = i;
                            i = temp;
                        }

                        if (j > k) {
                            temp = j;
                            j = i;
                            i = temp;
                        }

                        if (i > k) {
                            temp = k;
                            k = i;
                            i = temp;

                        }
                        triangle[0] = i;
                        triangle[1] = j;
                        triangle[2] = k;


                        if (!del.containsValue(triangle)) {
                            del.put(del.size(), triangle);
                            dela += i + "\t" + "j" + "\t" + "k" + "\t"; //"\n"; //C++ strtok때문에 바꿈
                        }
                    }
                    pass = true;

                }//for(k)
            }//for(j)
        }//for(i)
        try {
            String ex_storage = Environment.getExternalStorageDirectory().getAbsolutePath();
            File dir = new File(ex_storage + File.separator + "Phairy" + File.separator + "dela" + File.separator);
            dir.mkdir();
            File file = new File(ex_storage + File.separator + "Phairy" + File.separator + "dela" + File.separator + "dela.txt");  //파일 생성!
            FileOutputStream fos = new FileOutputStream(file, true);  //mode_append
            fos.write(dela.getBytes());
            fos.close();

        } catch (Exception e) {
        }
        return del;
    }


    public void makeFile() {


        String ex_storage = Environment.getExternalStorageDirectory().getAbsolutePath();
        // Get Absolute Path in External Sdcard
        String folder_name = "/Phairy/image/";

        String string_path = ex_storage + folder_name;
        Drawable drawable;
        File file_path;
        try {
            file_path = new File(string_path);
            if (!file_path.isDirectory()) {
                file_path.mkdirs();
            }
            FileOutputStream out = new FileOutputStream(string_path + "i000qa.jpg");
            drawable = getResources().getDrawable(R.drawable.i000qa);

            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();

        } catch (FileNotFoundException exception) {
            Log.e("FileNotFoundException", exception.getMessage());
        } catch (IOException exception) {
            Log.e("IOException", exception.getMessage());
        }

        Field[] fields = R.raw.class.getFields();

        try {
            file_path = new File(folder_name);
            if (!file_path.isDirectory()) {
                file_path.mkdirs();
            }
        } catch (Exception e) {

        }

        Bitmap[] bitmap = new Bitmap[fields.length - 2];


        try {

            int y = bitmap[5].getHeight();
            int x = bitmap[5].getWidth();

            double bValue = 0;
            double gValue = 0;
            double rValue = 0;
            double grayscale = 0;

            for (int count = 0; count < fields.length; count++) {
                Log.i("Raw Asset: ", fields[count].getName());

                if (fields[count].getName().equals("backg.jpg") || fields[count].getName().equals("deer.jpg"))
                    continue;
                drawable = getResources().getDrawable(getResources().getIdentifier("@drawable/" + fields[count].getName(), "drawable", this.getPackageName()));

                bitmap[count] = ((BitmapDrawable) drawable).getBitmap();
            }
//        gray 화 + 평균 구하기
            double[][][] A = new double[fields.length - 2][x][y];

            double[][] Edouble = new double[1][x * y];//1열
            Jama.Matrix E = new Jama.Matrix(Edouble);


            String aver="";

            for (int j = 0; j < y; j++) {//height
                for (int i = 0; i < x; i++) {//width
                    for (int k = 0; j < fields.length - 2; j++) {
                        bValue += bitmap[k].getPixel(i, j) & 0x000000FF;
                        gValue += (bitmap[k].getPixel(i, j) & 0x0000FF00) >> 8;
                        rValue += (bitmap[k].getPixel(i, j) & 0x00FF0000) >> 16;

                        grayscale = 0.587 * gValue + 0.299 * rValue + 0.114 * bValue;

                        A[k][i][j] = grayscale;

                        aver = "" + grayscale;
                    }
                    grayscale = grayscale / fields.length - 2;
                    E.set(0, j * y + i, grayscale);
                    grayscale = 0;
                    aver = aver + "\n";
                }
            }


            String ext;
            File file;
            FileOutputStream fos;
            File dir;
            ext = Environment.getExternalStorageState();
            String mSdPath;

            if (ext.equals(Environment.MEDIA_MOUNTED)) {
                mSdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            } else {
                mSdPath = Environment.MEDIA_UNMOUNTED;
            }

            dir = new File(mSdPath + File.separator + "pHairy" + File.separator + "imgV" + File.separator);
            dir.mkdir();

            file = new File(mSdPath + File.separator + "pHairy" + File.separator + "imgV" + File.separator + "imgVector");  //파일 생성!

            fos = new FileOutputStream(file, true);  //mode_append

            fos.write(aver.getBytes());
            fos.close();
//ㅡㅡㅡㅡㅡㅡㅡㅡㅡdataSize*dataSize 차원의 공분산행렬 구하기
            double[][] coveri = new double[fields.length - 2][fields.length - 2];
            Jama.Matrix C = new Jama.Matrix(coveri);

            double temp = 0;
            int h, v;
            for (int i = 0; i < fields.length - 2; i++) {//dataSize
                for (int j = 0; j < fields.length - 2; j++) {   //pointSize * 2
                    for (int k = 0; k < x * y; k++) {
                        h = y % k;
                        v = y / k;
                        temp = temp + (A[i][h][v] - E.get(0, k)) * (A[j][h][v] - E.get(0, k));
                    }
                    temp = temp / (fields.length - 2 - 1);   //왜 dataSize-1인지는 모르겠지만...
                    C.set(i, j, temp);
                    temp = 0;
                }
            }

//normalize? 필요할 것 같긴 하다...

//ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ고유벡터 뽑아내기


            EigenvalueDecomposition ed = C.eig();


            Log.e(TAG, "CameraActivity: 공분산 C의 col수 = " + C.getColumnDimension() + " row수 = " + C.getRowDimension());


            String a = "(";
            try {
                for (int i = 0; i < C.getColumnDimension(); i++) {
//                Log.e(TAG, i+"행 시작");
                    for (int j = 0; j < C.getRowDimension(); j++) {
//                    Log.e(TAG, j+"열 시작");
                        a = a + " " + C.get(i, j);
//                    Log.e(TAG, ""+M.get(i,j));
                    }

                    a += ")";
                    Log.e(TAG, "" + a);
                    a = "(";
                }
            } catch (Exception exx) {
                exx.printStackTrace();
                Log.e(TAG, "CameraActivity:1 error");

            }

//ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ고유벡터 크기순 정렬하기

            Log.e(TAG, "CameraActivity:ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ이하 EigenValueㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ");
            Jama.Matrix xx = ed.getD();

            double Evalues[] = new double[fields.length - 2];
            int Erank[] = new int[fields.length - 2];

            Log.e(TAG, "CameraActivity: x value col = " + xx.getColumnDimension() + " row = " + xx.getRowDimension());

            for (int i = 0; i < xx.getRowDimension(); i++) {
                Evalues[i] = xx.get(i, i);
            }
            a = "(";

            temp = 0;

            for (int i = 0; i < fields.length - 2; i++) {      //Evalues 내림차순 정렬
                for (int j = 0; j < fields.length - 2; j++) {

                    if (Evalues[i] < Evalues[j]) {
                        temp = Evalues[j];
                        Evalues[j] = Evalues[i];
                        Evalues[i] = temp;
                    }
                }
            }

            for (int i = 0; i < fields.length - 2; i++) {      //Evalues 내림차순 정렬
                for (int j = 0; j < fields.length - 2; j++) {

                    if (Evalues[i] == xx.get(j, j)) {
                        Erank[i] = j;
                        break;
                    }

                }
            }


            Log.e(TAG, "CameraActivity:ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ이하 EigenVectorㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ");
            Log.e(TAG, "CameraActivity: x vector col = " + xx.getColumnDimension() + " row = " + xx.getRowDimension());

            xx = ed.getV();
            Jama.Matrix AlignedEigenVector = new Jama.Matrix(fields.length - 2, fields.length - 2);

            for (int i = 0; i < fields.length - 2; i++) {      //EigenVector를 고유값의 크기에 따라 재정렬.
                for (int j = 0; j < fields.length - 2; j++) {
                    AlignedEigenVector.set(i, j, xx.get(Erank[i], j));
                }
            }

            Log.e(TAG, "CameraActivity: AlignedEigenVector col = " + xx.getColumnDimension() + " row = " + xx.getRowDimension());

            xx = ed.getV();

            try {
                for (int i = 0; i < xx.getColumnDimension(); i++) {
                    for (int j = 0; j < xx.getRowDimension(); j++) {
                        a = a + " " + AlignedEigenVector.get(i, j);
//                    Log.e(TAG, ""+x.get(i,j));
                    }
                    a+="\n";

                }
                Log.e(TAG, "" + a);
                dir = new File(mSdPath + File.separator + "pHairy" + File.separator + "imgV" + File.separator);
                dir.mkdir();

                file = new File(mSdPath + File.separator + "pHairy" + File.separator + "imgV" + File.separator + "imgVector");  //파일 생성!

                fos = new FileOutputStream(file, true);  //mode_append

                fos.write(a.getBytes());
                fos.close();

            } catch (Exception exx) {
                exx.printStackTrace();
                Log.e(TAG, "CameraActivity:3 error");

            }

        } catch (Exception e) {

        }

    }

    private String[] getTitleList() //알아 보기 쉽게 메소드 부터 시작합니다.
    {
        try {

            //TODO 50kb이하 파일은 그냥 삭제시킬까?
//            FilenameFilter fileFilter = new FilenameFilter()  //이부분은 특정 확장자만 가지고 오고 싶을 경우 사용하시면 됩니다.
//            {
//                public boolean accept(File dir, String name)
//                {
//                    return name.endsWith("gpx"); //이 부분에 사용하고 싶은 확장자를 넣으시면 됩니다.
//                } //end accept
//            };
            String mSdPath;
            String ext = Environment.getExternalStorageState();
            if (ext.equals(Environment.MEDIA_MOUNTED)) {
                mSdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            } else {
                mSdPath = Environment.MEDIA_UNMOUNTED;
            }

            File file = new File(mSdPath + File.separator + "Phairy" + File.separator + "image" + File.separator);

            File[] files = file.listFiles();//위에 만들어 두신 필터를 넣으세요. 만약 필요치 않으시면 fileFilter를 지우세요.

            String[] titleList = new String[files.length]; //파일이 있는 만큼 어레이 생성했구요

            for (int i = 0; i < files.length; i++) {
                titleList[i] = files[i].getName();    //루프로 돌면서 어레이에 하나씩 집어 넣습니다.

            }//end for
            return titleList;

        } catch (Exception e) {

            return null;
        }//end catch()
    }//end getTitleList

    public void PCAImgAnalysis() {
        String[] fileList = getTitleList();

        if (fileList != null || fileList.length == 0) {
            return;
        }

        String mSdPath;
        String ext = Environment.getExternalStorageState();
        if (ext.equals(Environment.MEDIA_MOUNTED)) {
            mSdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            mSdPath = Environment.MEDIA_UNMOUNTED;
        }
        for (int i = 0; i < fileList.length; i++) {
            File file = new File(mSdPath + File.separator + "Phairy" + File.separator + "image" + File.separator + fileList[i]);
        }
    }
}

//http://s-pear.tistory.com/7 intensity