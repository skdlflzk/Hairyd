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
import android.hardware.Camera;


import android.os.Bundle;
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
import com.mkobos.pca_transform.PCA;

import org.opencv.android.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.StringTokenizer;

import Jama.EigenvalueDecomposition;

public class CameraActivity extends Activity {

    String TAG = Start.TAG;
    private static final int RQS_LOADIMAGE = 1;
    private Button btnLoad, btnDetFace;
    private ImageView imgView;
    private Bitmap myBitmap;


    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;

    Camera.Size size;           //MUCT는 세로 480x640 이미지임.

    Camera mCamera = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cam_activity);

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

                Integer width = 0;
                Integer height = 0;

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


    public void pcaAnalize() {

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

        double[][] A = new double[dataSize][76 * 2];

        for (int i = 0; i < dataSize; i++) {

            tabtoken = new StringTokenizer(entertoken.nextToken(), ",");    //lat
            tabtoken.nextToken();//name
            tabtoken.nextToken();//tag

            for (int j = 0; j < 76; j++) {

                A[i][2 * j] = Double.parseDouble(tabtoken.nextToken());
                A[i][2 * j + 1] = Double.parseDouble(tabtoken.nextToken());
            }
        }
        Jama.Matrix M = new Jama.Matrix(A);
        EigenvalueDecomposition E = new EigenvalueDecomposition(M.eig());

        Jama.Matrix vec = E.getV();
        try {
            for (int i = 0; i < vec.getColumnDimension(); i++) {
                for (int j = 0; i < vec.getRowDimension(); j++) {
                    vec.print(i, j);
                }
            }
        }catch (Exception e){

        }


    }


//        double [] average = new double[76 * 2];
//        double [][] covMatrix = new double[76 * 2][76 * 2];
//
//        for (int i = 0; i < 76; i++){
//            for (int j = 0; j < dataSize; j++) {
//                average[2 * i] += point[j][2 * i];     // x  j는 이름, j+1은 태그
//                average[2 * i + 1] += point[j][2 * i+1];   // y
//            }
//            average[2 * i] = average[2 * i] / dataSize;
//            average[2 * i + 1] = average[2 * i + 1] / dataSize;
//        }
//
//        for ( int i = 0;  i < 142; i++){
//            for (int j = 0; j < 142; j++) {
//                covMatrix[i][j] += (point[i][j] - average[j])*(point[j][i] - average[i]);   //142*142
//            }
//        }
//        for ( int i = 0;  i < 142; i++){
//            for (int j = 0; j < 142; j++) {
//                covMatrix[i][j] = covMatrix[i][j] / (76 - 1);
//            }
//        }


//                                    mLogger.debug("좌표 = (" + lattitude2 + ", " + longitude2 + "), 이동 거리 = " + intervalDistance + "m, 속도 = " + 3.6 * intervalDistance / intervalTime + "m/s, 총 거리 = " + distance + ", 지금 시각 = " + h + "시 " + m + "분 " + s + "초 , " + intervalTime + "초 간격");


//        String [] list = data.split("[\\r\\n]+");
//        String[][] values = new String[list.length][76*2+2];
//        for(int i = 0; i < list.length ; i++){
//            values[i] = list[i].split(",");// n은 이름, n+1은 태그, n+2k은 xk-1, n+2k+1은 yk-1
//        }
//
//        double [] average = new double[76*2];
//눈 위치는 (x31,y31)과 (x36,y36)



    public double[] procrustes(double x1, double y1, double x2, double y2) {
        //미리 계산된 평균 average 값
        double[] average = {201, 348, 201, 381, 202, 408, 209, 435, 224, 461, 241, 483, 264, 498, 292, 501, 319, 493, 338, 470, 353, 448, 363, 423, 367, 395, 366, 371, 357, 344, 355, 316, 340, 311, 325, 318, 309, 328, 327, 324, 342, 317, 217, 328, 231, 323, 250, 327, 269, 333, 251, 334, 233, 331, 229, 345, 240, 337, 262, 349, 242, 352, 241, 344, 346, 337, 330, 330, 318, 341, 334, 344, 330, 336, 280, 344, 278, 381, 264, 399, 273, 406, 293, 409, 316, 399, 321, 392, 304, 376, 296, 342, 279, 402, 310, 399, 251, 431, 268, 427, 284, 425, 293, 425, 302, 423, 316, 425, 329, 426, 320, 442, 309, 451, 295, 454, 278, 452, 263, 442, 277, 440, 293, 442, 313, 437, 313, 429, 293, 432, 277, 431, 293, 436, 295, 395, 234.5, 341, 251, 343, 252, 350.5, 235.5, 348.5, 338, 333.5, 324, 335.5, 326, 342.5, 340, 340.5};
//눈 위치는 (x31,y31)과 (x36,y36)

        //프로크루스테스 정렬

        return average;
    }

}
