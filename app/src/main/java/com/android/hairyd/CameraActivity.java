package com.android.hairyd;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
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

    TextView ing;

    Camera.Size size;           //MUCT는 세로 480x640 이미지임.

    Camera mCamera = null;


    Mat tempA;  //gradient 전달용 Mat
    Jama.Matrix S0; //shape 평균
    Jama.Matrix Si; //shape 데이터
    Jama.Matrix pi; //shape 파라메터

    Jama.Matrix A0; //appearance 평균
    Jama.Matrix Ai; //appearance 데이터
    Jama.Matrix gi; //appearance 파라메터

    HashMap<Integer, String> pointIndex;
    HashMap<String, Integer> textureIndex;

    HashMap<Integer, int[]> delauneyTriangle;    //들로네 삼각형
    HashMap<Integer, double[]> warpingConstants;    //각 삼각형의 warping 상수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cam_activity);
        Log.e("initAPPearance", "test activity!");
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
//
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceView.setClickable(true);
        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        delauneyTriangle = new HashMap<>();
        warpingConstants = new HashMap<>();

        NativeFunc nativeFunc = new NativeFunc();
        Log.e("initAPPearance", "삼각형 내부i = 639, j = 479점 ? = " + nativeFunc.IsIntersect(239.5, 423.73395472703066, 239.5, 428.4593874833555, 239.5, 378.4953395472703, 639, 479));
        // 239.5,423.73395472703066,239.5,428.4593874833555,239.5,378.4953395472703
        //12-15 18:46:35.833 27892-27892/com.android.hairyd E/P'hairy: i = 639, j = 479점
//        Log.e("initAPPearance", "삼각형 내부 ? = " + nativeFunc.IsIntersect(1, 1, 3, 4, 4, 0, (double) 1, (double) 1));
//        Log.e("initAPPearance", "삼각형 내부 ? = " + nativeFunc.IsIntersect(1, 1, 3, 4, 4, 0, (double) 2, (double) 0));

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

//                Log.i(TAG, "CameraActivity: Detecting... ," + facecount);


            }
        });

        Button b = (Button) findViewById(R.id.test);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AAM();
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

//        Log.i("Phairy", "detectFace_ loaded successfully. 크기 " + myBitmap.getWidth() + ", " + myBitmap.getHeight());

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


    public void initShape() {

        String data = "";
        Log.e("initShape", "파싱 시작");

        InputStream inputStream = getResources().openRawResource(R.raw.shapedata);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            int i = inputStream.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }

            data = new String(byteArrayOutputStream.toByteArray(), "UTF-8");
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringTokenizer entertoken = new StringTokenizer(data, "\n");

        entertoken.nextToken(); //첫번째는 날리기
        StringTokenizer tabtoken;

        //ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ데이터 크기

        int dataSize = entertoken.countTokens();
        Log.e("initShape", "데이터 크기 = " + dataSize);

        double[][] arraySi = new double[dataSize][76 * 2];
        double[] alignDatum;
        pointIndex = new HashMap<>();

        for (int i = 0; i < dataSize; i++) {

            tabtoken = new StringTokenizer(entertoken.nextToken(), "\t");    //lat
            String name = tabtoken.nextToken();
//            Log.e("initShape", "name = " + name);
            pointIndex.put(i, name); //name
            tabtoken.nextToken();//tag

            for (int j = 0; j < 76; j++) {
                arraySi[i][2 * j] = Double.parseDouble(tabtoken.nextToken());
                arraySi[i][2 * j + 1] = Double.parseDouble(tabtoken.nextToken());
            }

            //프로크루스테스 정렬
            alignDatum = procrustes(arraySi[i]);
//            alignDatum = arraySi[i];
            for (int j = 0; j < 76; j++) {
                arraySi[i][2 * j] = alignDatum[2 * j];
                arraySi[i][2 * j + 1] = alignDatum[2 * j + 1];
            }
        }


//        평균 구하기
        Jama.Matrix pointSi = new Jama.Matrix(arraySi);
        double[][] arrayS0 = new double[1][76 * 2];
        S0 = new Jama.Matrix(arrayS0);


        double temp = 0;
        for (int i = 0; i < 76 * 2; i++) {

            for (int j = 0; j < dataSize; j++) {

                temp += arraySi[j][i];

            }
            temp = temp / dataSize;
            S0.set(0, i, temp);
            temp = 0;
        }

        String a = "";

        for (int j = 0; j < 76 * 2; j++) {
//                    Log.e(TAG, j+"열 시작");
            a = a + " " + S0.get(0, j);
//                    Log.e(TAG, ""+M.get(i,j));
        }
        Log.e("initShape", "평균  - ");
        Log.e(TAG, "(" + a + ")"); //평균!

//ㅡㅡㅡㅡㅡㅡㅡㅡㅡdata*data 차원의 공분산행렬 구하기
        Log.e(TAG, "공분산 구하기..."); //평균!
        double[][] coveri = new double[76 * 2][76 * 2];
        Jama.Matrix C = new Jama.Matrix(coveri);

        for (int i = 0; i < 76 * 2; i++) {
            for (int j = 0; j < 76 * 2; j++) {
                for (int k = 0; k < dataSize; k++) {
                    temp = temp + (pointSi.get(k, i) - S0.get(0, i)) * (pointSi.get(k, j) - S0.get(0, j));
                }
                temp = temp / (dataSize - 1);   //왜 dataSize-1인지는 모르겠지만...
                C.set(i, j, temp);
                temp = 0;
            }
        }
        Log.e(TAG, "공분산 구하기...ok"); //평균!

//normalize? 필요할 것 같긴 하다...

//ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ고유벡터 뽑아내기


//        Log.e(TAG, "initShape: 공분산 C의 col수 = " + C.getColumnDimension() + " row수 = " + C.getRowDimension());


//        a = "(";
//        try {
//            for (int i = 0; i < C.getColumnDimension(); i++) {
//                for (int j = 0; j < C.getRowDimension(); j++) {
//                    a = a + " " + C.get(i, j);
//                }
//                a += ")";
//                Log.e(TAG, "" + a);
//                a = "(";
//            }
//        } catch (Exception exx) {
//            exx.printStackTrace();
//            Log.e(TAG, "initShape:1 error");
//        }

//ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ고유벡터 크기순 정렬하기

//        Log.e(TAG, "initShape:ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ이하 EigenValueㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ");
        Log.e(TAG, "고유값으로 내림차순...");
        EigenvalueDecomposition ed = C.eig();
        Jama.Matrix x = ed.getD(); //고유값 getV()는 고유벡터

//        Log.e(TAG, "CameraActivity: x value col = " + x.getColumnDimension() + " row = " + x.getRowDimension());

        double Evalues[] = new double[76 * 2];
        int Erank[] = new int[76 * 2];


        for (int i = 0; i < x.getRowDimension(); i++) {
            Evalues[i] = x.get(i, i);
        }
        a = "(";

        temp = 0;

        for (int i = 0; i < 76 * 2; i++) {      //Evalues 내림차순 정렬
            for (int j = 0; j < 76 * 2; j++) {

                if (Evalues[i] < Evalues[j]) {
                    temp = Evalues[j];
                    Evalues[j] = Evalues[i];
                    Evalues[i] = temp;
                }
            }
        }

        for (int i = 0; i < 76 * 2; i++) {      //Evalues 내림차순 정렬
            for (int j = 0; j < 76 * 2; j++) {
                if (Evalues[i] == x.get(j, j)) {
                    Erank[i] = j;
                    break;
                }
            }
        }

        Log.e(TAG, "고유값으로 내림차순...ok");
//        Log.e(TAG, "initShape:ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ이하 EigenVectorㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ");

        Log.e(TAG, "고유벡터 구하기...");
        x = ed.getV();
//        Log.e(TAG, "initShape: x vector col = " + x.getColumnDimension() + " row = " + x.getRowDimension());
        Si = new Jama.Matrix(76 * 2, 76 * 2);

        for (int i = 0; i < 76 * 2; i++) {      //EigenVector를 고유값의 크기에 따라 재정렬.
            for (int j = 0; j < 76 * 2; j++) {
                Si.set(i, j, x.get(Erank[i], j));
            }
        }
//
//        Log.e(TAG, "initShape: EigenVector col = " + x.getColumnDimension() + " row = " + x.getRowDimension());
//        a = "(";

        //ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ


//        try {
//            for (int i = 0; i < x.getColumnDimension(); i++) {
//                for (int j = 0; j < x.getRowDimension(); j++) {
//                    a = a + " " + Si.get(i, j);
////                    Log.e(TAG, ""+x.get(i,j));
//                }
////                a += ")";
////                Log.e(TAG, "" + a);
////                a = "(";
//            }
//        } catch (Exception exx) {
//            exx.printStackTrace();
//            Log.e(TAG, "initShape:3 error");
//        }

//        SharedPreferences pref = getSharedPreferences("pref", Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = pref.edit();
//
//        editor.putInt("timeMinute", );
//        editor.commit();
    }


    public double[] procrustes(double[] datum) {

        //미리 계산된 평균 average 값
        double[] average = {143.50066577896138, 335.62450066577895, 143.3069241011984, 367.762982689747, 146.10852197070574, 396.28362183754996, 151.84753661784288, 427.86391478029293, 165.48868175765645, 456.44007989347534, 185.262982689747, 476.5472703062583, 209.8195739014647, 491.60186418109186, 239.5, 497.1717709720373, 269.1804260985353, 491.60186418109186, 293.737017310253, 476.5472703062583, 313.5113182423435, 456.44007989347534, 327.1524633821571, 427.86391478029293, 332.89147802929426, 396.28362183754996, 335.6930758988016, 367.762982689747, 335.4993342210386, 335.62450066577895, 316.0186418109188, 316.262982689747, 299.09114513981365, 301.7243675099867, 278.54773635153134, 301.81970705725684, 259.3335552596538, 313.2556591211718, 279.3519973368844, 312.81870838881497, 297.83368841544603, 312.40998668442074, 162.98135818908122, 316.262982689747, 179.9088548601864, 301.7243675099866, 200.45233022636486, 301.81970705725706, 219.6664447403462, 313.2556591211718, 199.6480026631159, 312.8187083888149, 181.1663781624501, 312.4099866844208, 178.30419440745666, 335.6703728362182, 194.33894806924098, 326.8832223701731, 211.63641810918776, 335.92703062583223, 194.52296937416784, 341.2171105193078, 194.85585885486017, 333.3802929427431, 300.6958055925434, 335.6703728362183, 284.661051930759, 326.88322237017314, 267.36364846870845, 335.9270306258323, 284.477097203728, 341.2171105193073, 284.14414114513977, 333.38029294274304, 228.7796271637816, 333.193741677763, 225.18908122503328, 362.11984021304926, 209.4074567243675, 382.06790945406124, 217.34953395472704, 392.5832223701731, 239.5, 397.1464713715047, 261.650466045273, 392.5832223701731, 269.59254327563247, 382.06790945406124, 253.81091877496672, 362.11984021304926, 250.2203728362184, 333.193741677763, 224.19973368841545, 387.83688415446073, 254.80026631158455, 387.83688415446073, 202.04527296937417, 427.06458055925435, 216.53715046604526, 416.65352862849534, 230.92696404793608, 413.1930093209055, 239.5, 414.330758988016, 248.0730359520639, 413.1930093209055, 262.46291611185086, 416.65352862849534, 276.9547270306258, 427.06458055925435, 267.6426764314248, 438.40845539280957, 254.67842876165113, 444.03728362183756, 239.5, 445.62849533954727, 224.32157123834887, 444.03728362183756, 211.35732356857525, 438.40845539280957, 222.3561917443409, 431.37350199733686, 239.5, 433.79840213049266, 256.6438082556591, 431.37350199733686, 256.5206391478029, 423.44607190412785, 239.5, 423.73395472703066, 222.47936085219706, 423.44607190412785, 239.5, 428.4593874833555, 239.5, 378.4953395472703, 186.32057256990674, 331.2776964047935, 202.98868175765645, 331.40605858854866, 203.08022636484688, 338.5730359520641, 186.412516644474, 338.44513981358193, 292.67949400798943, 331.27769640479363, 276.01131824234346, 331.40605858854883, 275.91984021304927, 338.5730359520637, 292.587616511318, 338.44513981358193};
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


    public HashMap initDelauney() {   // 1행데이터를 넣으면

        int size = 76;
        double x1, y1, x2, y2, x3, y3, a, b;
        double x, y, c1, c2;
        double r2;
        boolean pass = true;




        String dela = "";
        int counter =0;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {
                    if (i >= j || j >= k || k == i) {
                        continue;
                    }

                    x1 = S0.get(0, 2 * i);
                    x2 = S0.get(0, 2 * j);
                    x3 = S0.get(0, 2 * k);
                    y1 = S0.get(0, 2 * i + 1);
                    y2 = S0.get(0, 2 * j + 1);
                    y3 = S0.get(0, 2 * k + 1);

                    if( x1 == x2  && x2 == x3) { continue; }
                    if( y1 == y2  && y2 == y3) { continue; }
/* ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ
//                    Log.e("initShape"," i = " + i +" j = " + j + " k "+ k + " x1 = " + x1 +" x2 = " + x2 + "x3 = " +x3 +"y1 = " + y1 +" y2 = " + y2 + "y3 = " +y3 ) ;
                    if (y1 == y2) {
//                        Log.e("initShape"," y1==y2" ) ;
                        b = (x1 - x3) / (y3 - y1);
                        x = (x1 + x2) / 2;

                        c2 = (y1 + y3) / 2 - (b * (x3 + x1)) / 2;

                        y = b * x + c2;

                    } else if (y1 == y3) {
//                        Log.e("initShape"," y1==y3" ) ;
                        a = (x1 - x2) / (y2 - y1);
                        x = (x1 + x3) / 2;

                        c1 = (y1 + y2) / 2 - (a * (x2 + x1)) / 2;

                        y = a * x + c1;

                    } else {

                        a = (x1 - x2) / (y2 - y1);
                        b = (x1 - x3) / (y3 - y1);
                        c1 = (y1 + y2) / 2 - (a * (x2 + x1)) / 2;
                        c2 = (y1 + y3) / 2 - (b * (x3 + x1)) / 2;
//                        Log.e("initShape"," a = " + a+ " b = "+ b + " c1 = "+ c1 + "c2" +c2  ) ;
                        x = (c1 - c2) / (b - a);
                        y = a * x + c1;
                    }
                    ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ*/


                    double alpha = Math.sqrt(Math.pow(x2 - x3, 2) + Math.pow(y2 - y3, 2)); //BC
                    double beta = Math.sqrt(Math.pow(x3 - x1, 2) + Math.pow(y3 - y1, 2)); //CA
                    double gamma = Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));//AB
                    double s = (alpha + beta + gamma) / 2;
                    double moth = 16 * s * (s - alpha) * (s - beta) * (s - gamma);

                    x = ((-alpha * alpha + beta * beta + gamma * gamma) * alpha * alpha) / moth * x1
                            + ((alpha * alpha - beta * beta + gamma * gamma) * beta * beta) / moth * x2
                            + ((alpha * alpha + beta * beta - gamma * gamma) * gamma * gamma) / moth * x3;

                    y = ((-alpha * alpha + beta * beta + gamma * gamma) * alpha * alpha) / moth * y1
                            + ((alpha * alpha - beta * beta + gamma * gamma) * beta * beta) / moth * y2
                            + ((alpha * alpha + beta * beta - gamma * gamma) * gamma * gamma) / moth * y3;

                    r2 = ((x - x1) * (x - x1) + (y - y1) * (y - y1));

//                    Log.e("initShape","" + " i = " + i +" j = " + j + "k = " +k +"번째, 원의 중심 = ("+x + "," +y +") ,r2="+ r2) ;
                    pass = true;

                    for (int l = 0; l < size; l++) {    //i,j,k점을 제외한 다른 점들이 내부에 있지 않다면
                        if (l == i || l == j || l == k) {
                            continue;
                        }
                        if (r2 > Math.pow(x - S0.get(0, 2 * l), 2) + Math.pow(y - S0.get(0, 2 * l + 1), 2)) {
//                                Log.e("initShape", "내부에 점이 존재! r2= " + ( ( x - S0.get(0,2 * l)) * (x - S0.get(0,2 * l) )) + ( (y - S0.get(0,2 * l + 1)) * (y - S0.get(0,2 * l + 1)))+" ");
                            pass = false;
                            break;
                        }
                    }//모든 점이 내부에 있지 않다면

                    //올림차순
                    if (!pass) {
//                        Log.e("initShape", " i = " + i + " j = " + j + " k " + k + " 에서 들로네 실패~ ");
                        continue;
                    }

                    int tempi, tempj, tempk;
                    tempi = i;
                    tempj = j;
                    tempk = k;
//                        Log.e("initShape", "통과!"+ " i = " + i +" j = " + j + "k = " +k);
                    int temp;
                    if (tempi > tempj) {
                        temp = tempj;
                        tempj = tempi;
                        tempi = temp;
                    }

                    if (tempi > tempk) {
                        temp = tempi;
                        tempi = tempk;
                        tempk = temp;
                    }

                    if (tempj > tempk) {
                        temp = tempk;
                        tempk = tempj;
                        tempj = temp;

                    }
                    int[] triangle = {tempi,tempj,tempk};


                    delauneyTriangle.put(counter, triangle);

                        Log.e("initShape", counter + "번째  - " + tempi + "\t" + tempj + "\t" + tempk + " 추가요~");
                    counter++;
                    dela += tempi + "\t" + tempj + "\t" + tempk + "\t"; //"\n"; //C++ strtok때문에 바꿈

                }//for(k)
            }//for(j)
        }//for(i)


//        Log.e("initShape", "delauney Size = " + delauneyTriangle.size());// + ", 10번째? = " + delauneyTriangle.get(10)[0]+"." +delauneyTriangle.get(10)[1] + "." + delauneyTriangle.get(10)[2]);
//        try {
//            String ex_storage = Environment.getExternalStorageDirectory().getAbsolutePath();
//            File dir = new File(ex_storage + File.separator + "Phairy" + File.separator + "dela" + File.separator);
//            dir.mkdir();
//            File file = new File(ex_storage + File.separator + "Phairy" + File.separator + "dela" + File.separator + "delauney.txt");  //파일 생성!
//            FileOutputStream fos = new FileOutputStream(file, true);  //mode_append
//            fos.write(dela.getBytes());
//            fos.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return delauneyTriangle;
    }


    public void initAppearance() {


        Drawable drawable;
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

        Field[] fields = R.drawable.class.getFields();
        int dataSize = 0;
        int resID;

        for (int i = 0; i < fields.length; i++) {

            if (fields[i].toString().contains("datai")) {
//                Log.e("initShape", "name = "+ fields[i].toString().substring(54));
                dataSize++;//dataSize개의 데이터
            }
        }

        //dataSize = getSizeOfData(); //데이터 수를 구해올수도
        //dataSize = 751;

        // 이미지 받아오기
        Bitmap[] appearanceData = new Bitmap[dataSize];
        textureIndex = new HashMap<>();
//        String resName,packName;
        String resName = "@drawable/" + fields[100].toString().substring(54);
//            Log.e("initShape", ""+ fields[i].toString());
        textureIndex.put(fields[100].toString().substring(54), 0); //name

        String packName = this.getPackageName(); // 패키지명
        resID = getResources().getIdentifier(resName, "drawable", packName);
        drawable = getResources().getDrawable(resID);

        appearanceData[0] = ((BitmapDrawable) drawable).getBitmap();
        Log.e("initShape", "image 불러오기...ok");

        int sampleSize = 0;  //sampleSize 개수

        for (int i = 0; i < fields.length; i++) {
            if (!fields[i].toString().contains("datai")) {
                continue;
            }

            /*ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ메모리 오류...!
             resName = "@drawable/" + fields[i].toString().substring(54);
            Log.e("initShape", i + " = > " + fields[i].toString());
            textureIndex.put(fields[i].toString().substring(54), i); //name

            packName = this.getPackageName(); // 패키지명
            resID = getResources().getIdentifier(resName, "drawable", packName);
            drawable = ResourcesCompat.getDrawable(getResources(), resID, null);
            //getResources().getDrawable(resID);

            appearanceData[sampleSize] = ((BitmapDrawable) drawable).getBitmap();

            */
            sampleSize++;
        }


        Log.e("initShape", "SampleSize = " + sampleSize);


        int height = appearanceData[0].getHeight();
        int width = appearanceData[0].getWidth();
        double bValue = 0;
        double gValue = 0;
        double rValue = 0;
        double grayscale = 0;


//
        try {


            String aver = "";


            double x1 = 0, x2 = 0, x3 = 0, y1 = 0, y2 = 0, y3 = 0, xn1 = 0, xn2 = 0, xn3 = 0, yn1 = 0, yn2 = 0, yn3 = 0, x, y;
            double[] warpConstants = new double[6];

             /*
                  평균 영상 A0와 delauney 상수 계산해놓기
                        현재 위치를 affine 변환 후, 얼굴 영역인지 확인
             */
            int textureSize = 0;
            int index =0;
            boolean pass;
//            double[][][] Abitmap = new double[dataSize][width][height];
//            tempA = new Mat(480, width, CvType.CV_8UC1);

            int Asize = 30955;  //appearanceTexture의 크기
            double[][] arrayAi = new double[1][Asize];
            Jama.Matrix appearanceAi = new Jama.Matrix(arrayAi);


            String txt="";
            for (int j = 0; j < 480; j++) {//height
                for (int i = 0; i < 640; i++) {//width
                    int triangle = -1;
                    double temp = 0;
//                    Log.e(TAG, "delauneyTriangle size =  " + delauneyTriangle.size());

                    //몇 번째 삼각형 내부에 있는지 확인
                    for (int k = 0; k < delauneyTriangle.size(); k++) {

                        x1 = S0.get(0, 2 * delauneyTriangle.get(k)[0]);
                        x2 = S0.get(0, 2 * delauneyTriangle.get(k)[1]);
                        x3 = S0.get(0, 2 * delauneyTriangle.get(k)[2]);

                        y1 = S0.get(0, 2 * delauneyTriangle.get(k)[0] + 1);
                        y2 = S0.get(0, 2 * delauneyTriangle.get(k)[1] + 1);
                        y3 = S0.get(0, 2 * delauneyTriangle.get(k)[2] + 1);

                        NativeFunc nativeFunc = new NativeFunc();

//                        Log.e(TAG, "x1, y1, x2, y2, x3, y3 = " +x1 +","+ y1 +","+ x2+","+ y2+","+ x3+","+ y3);
                        if (nativeFunc.IsIntersect(x1, y1, x2, y2, x3, y3, (double) i, (double) j)) { //true라면
                            triangle = k;       //현재 k번째 삼각형 내부에 있음!
//                            Log.e(TAG, "i = " + i + ", j = " + j + "점은  k = " + k + " = " + triangle + "  번째 삼각형 내부에 있음!");
                            textureSize++;
                            break;
                        }

                    }//k

                    if (triangle == -1) {        //삼각형 내부에 없다면?
//                        Log.e(TAG, "외부!에 있음!");
                        continue;
                    }

                    // A0구하기, Warping 시키기
//                    Log.e(TAG, "A0구하기, Warping...");

                    for (int k = 0; k < appearanceData.length; k++) {//주석삭제


                        xn1 = Si.get(k,2 * delauneyTriangle.get(triangle)[0]);
                        xn2 = Si.get(k,2 * delauneyTriangle.get(triangle)[1]);
                        xn3 = Si.get(k,2 * delauneyTriangle.get(triangle)[2]);

                        yn1 = Si.get(k,2 * delauneyTriangle.get(triangle)[0] + 1);
                        yn2 = Si.get(k,2 * delauneyTriangle.get(triangle)[1] + 1);
                        yn3 = Si.get(k,2 * delauneyTriangle.get(triangle)[2] + 1);


                        // k번째 입력영상 들로네 삼각형의 좌표 지정
                        double alpha, beta;
                        double c = (x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1);
                        alpha = ((i - x1) * (y3 - y1) - (j - y1) * (x3 - x1)) / c;
//                    = ((y3-y1)/c)*i - x1*(y3-y1)/c - ((x3-x1)/c)*j + y1*(x3-x1)/c;

                        beta = ((j - y1) * (x2 - x1) - (i - x1) * (y2 - y1)) / c;

//                   =  ((x2-x1)/c)*j - y1*(x2-x1)/c - ((y2-y1)/c)*i + x1*(y2-y1)/c;

                        x = xn1 + alpha * xn2 + beta * xn3;// = a1 + a2*i + a3*j;
                        y = yn1 + alpha * yn2 + beta * yn3;// = a4 + a5*i + a6*j;


                        /*
                        double a1, a2, a3, a4, a5, a6;       //각 삼각형마다 미리 계산해놓으면 빠름!

                        a1 = (y1 * (x3 - x1) / c - x1 * (y3 - y1) / c) * xn2 + (x1 * (y2 - y1) / c - y1 * (x2 - x1) / c) * xn3 + xn1;
                        a2 = ((y3 - y1) / c) * xn2 - ((y2 - y1) / c) * xn3;
                        a3 = ((x2 - x1) / c) * xn3 - ((x3 - x1) / c) * xn2;
                        a4 = (y1 * (x3 - x1) / c - x1 * (y3 - y1) / c) * yn2 + (x1 * (y2 - y1) / c - y1 * (x2 - x1) / c) * yn3 + yn1;
                        a5 = ((y3 - y1) / c) * yn2 - ((y2 - y1) / c) * yn3;
                        a6 = ((x2 - x1) / c) * yn3 - ((x3 - x1) / c) * yn2;


                        warpConstants[0] = a1;
                        warpConstants[1] = a2;
                        warpConstants[2] = a3;
                        warpConstants[3] = a4;
                        warpConstants[4] = a5;
                        warpConstants[5] = a6;

                        if (!warpingConstants.containsValue(warpConstants)) {
                            warpingConstants.put(warpingConstants.size(), warpConstants);
                        }

                        */


                       int current = textureIndex.get(pointIndex.get(k)); //shape와 appearance 매칭
//
//                        //i, j의 warping 한 x,y의 데이터를 뽑아와야함
                        bValue += appearanceData[current].getPixel((int)x,(int) y) & 0x000000FF;
                        gValue += (appearanceData[current].getPixel((int)x,(int) y) & 0x0000FF00) >> 8;
                        rValue += (appearanceData[current].getPixel((int)x,(int) y) & 0x00FF0000) >> 16;

                        grayscale = 0.587 * gValue + 0.299 * rValue + 0.114 * bValue;

                        temp += grayscale;

                        appearanceAi.set(current,index,grayscale);
                        index++;
                    }//k

                    temp = temp / sampleSize;

                    A0.set(0, textureSize, temp);
                    txt = txt + " " + temp;
                    textureSize++;
                    tempA.put(height, width,temp);


                }//j
            }//i

            Log.e(TAG, "counter = " + textureSize);

            Log.e(TAG, "Appearance ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ이하 EigenValueㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ");


            String ext;
            File file;
            FileOutputStream fos;
            ext = Environment.getExternalStorageState();
            String mSdPath;


            if (ext.equals(Environment.MEDIA_MOUNTED)) {
                mSdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            } else {
                mSdPath = Environment.MEDIA_UNMOUNTED;
            }


                try {
                    File dir = new File(mSdPath + File.separator + "Phairy" + File.separator + "Temp" + File.separator);
                    dir.mkdir();

                    file = new File(mSdPath + File.separator + "Phairy" + File.separator + "Temp" + File.separator + "A0.txt");  //파일 생성!

                    fos = new FileOutputStream(file, true);  //mode_append

//                    String header = "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" " +
//                            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"1.1\" " +
//                            "creator=\"TAXIONLY\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">" + System.lineSeparator() + "" +
//                            "<trk>" + System.lineSeparator() + "" +
//                            "<name>TAXIONLY</name>" + System.lineSeparator() + "<trkseg>" + System.lineSeparator() + "";

                    fos.write(txt.getBytes());
                    fos.close();
                }catch (Exception e){
                    e.printStackTrace();
                }

//ㅡㅡㅡㅡㅡㅡㅡㅡㅡdataSize*dataSize 차원의 공분산행렬 구하기
            double[][] coveri = new double[Asize][Asize];
            Jama.Matrix C = new Jama.Matrix(coveri);

            double temp = 0;
            int h, v;
            for (int i = 0; i < Asize; i++) {//dataSize
                for (int j = 0; j < Asize; j++) {   //pointSize * 2
                    for (int k = 0; k < height * width; k++) {
                        h = height % k;
                        v = width / k;
                        temp = temp + (appearanceAi.get(i, k) - A0.get(0, k)) * (appearanceAi.get(j, k) - A0.get(0, k));
                    }
                    temp = temp / (dataSize - 1);   //왜 dataSize-1인지는 모르겠지만...
                    C.set(i, j, temp);
                    temp = 0;
                }
            }

//normalize

//ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ고유벡터 뽑아내기


            EigenvalueDecomposition ed = C.eig();


            Log.e(TAG, "Appearance 공분산 C의 col수 = " + C.getColumnDimension() + " row수 = " + C.getRowDimension());


            String a = "(";

//ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ고유벡터 크기순 정렬하기

            Log.e(TAG, "Appearance ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ이하 EigenValueㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ");
            Jama.Matrix xx = ed.getD();

            double Evalues[] = new double[sampleSize];
            int Erank[] = new int[sampleSize];

            Log.e(TAG, "Appearance: x value col = " + xx.getColumnDimension() + " row = " + xx.getRowDimension());

            for (int i = 0; i < xx.getRowDimension(); i++) {
                Evalues[i] = xx.get(i, i);
            }
            a = "(";

            temp = 0;

            for (int i = 0; i < sampleSize; i++) {      //Evalues 내림차순 정렬
                for (int j = 0; j < sampleSize; j++) {

                    if (Evalues[i] < Evalues[j]) {
                        temp = Evalues[j];
                        Evalues[j] = Evalues[i];
                        Evalues[i] = temp;
                    }
                }
            }

            for (int i = 0; i < sampleSize; i++) {      //Evalues 내림차순 정렬
                for (int j = 0; j < sampleSize; j++) {

                    if (Evalues[i] == xx.get(j, j)) {
                        Erank[i] = j;
                        break;
                    }

                }
            }


            Log.e(TAG, "Appearance:ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ이하 EigenVectorㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ");
            Log.e(TAG, "Appearance: x vector col = " + xx.getColumnDimension() + " row = " + xx.getRowDimension());

            xx = ed.getV();
            Ai = new Jama.Matrix(sampleSize, Asize); // dataSize x ASize

            for (int i = 0; i < sampleSize; i++) {      //EigenVector를 고유값의 크기에 따라 재정렬.
                for (int j = 0; j < Asize; j++) {
                    Ai.set(i, j, xx.get(Erank[i], j));
                }
            }

            Log.e(TAG, "Appearance: AlignedEigenVector col = " + xx.getColumnDimension() + " row = " + xx.getRowDimension());

            xx = ed.getV();

            try {
                for (int i = 0; i < xx.getColumnDimension(); i++) {
                    for (int j = 0; j < xx.getRowDimension(); j++) {
                        a = a + " " + Ai.get(i, j);
//                    Log.e(TAG, ""+x.get(i,j));
                    }
                    a += "\n";

                }
                Log.e(TAG, "" + a);
                File dir = new File(mSdPath + File.separator + "Phairy" + File.separator + "imgV" + File.separator);
                dir.mkdir();

                file = new File(mSdPath + File.separator + "Phairy" + File.separator + "imgV" + File.separator + "imgVector");  //파일 생성!

                fos = new FileOutputStream(file, true);  //mode_append

                fos.write(a.getBytes());
                fos.close();

            } catch (Exception exx) {
                exx.printStackTrace();
                Log.e(TAG, "Appearance:3 error");

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

    public void AAM() {
        //ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡA(0)구하기ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ
        //DELAUNAY 가져와서 각 이미지 별의 합계/사진수로 평균 A0(x)구하기
        Log.e("initShape", "시작");
        ing = (TextView) findViewById(R.id.ing);

        ing.setText("initiating shape parameter...");
        initShape();

        ing.setText("initiating delauney triangle...");
        Log.e("initDelauney", "ㅡㅡㅡㅡㅡ들로네");
        initDelauney();

//        for (int k = 0; k < delauneyTriangle.size(); k++) {
//
//            double x1 = S0.get(0, 2 * delauneyTriangle.get(k)[0]);
//            double x2 = S0.get(0, 2 * delauneyTriangle.get(k)[1]);
//            double x3 = S0.get(0, 2 * delauneyTriangle.get(k)[2]);
//
//            double y1 = S0.get(0, 2 * delauneyTriangle.get(k)[0] + 1);
//            double y2 = S0.get(0, 2 * delauneyTriangle.get(k)[1] + 1);
//            double y3 = S0.get(0, 2 * delauneyTriangle.get(k)[2] + 1);
//
////            Log.e(TAG, ""+k+"번째 x1, y1, x2, y2, x3, y3 = " + x1 + "," + y1 + "," + x2 + "," + y2 + "," + x3 + "," + y3);
//        }
        ing.setText("initiating appearance parameter...");
        initAppearance();

//        Pre-compute:
//        (3) Evaluate the gradient ∇A0 of the template A0(x)
        NativeFunc nativeFunc = new NativeFunc();
//        Mat gradient = nativeFunc.getGradient(0);

//        (4) Evaluate the Jacobian @W
//                @p at (x; 0)
//        (5) Compute the modified steepest descent images using Equation (41)
//        (6) Compute the Hessian matrix using modified steepest descent images
//        Iterate:
//        (1) Warp I withW(x; p) to compute I(W(x; p))
//        (2) Compute the error image I(W(x; p)) − A0(x)
//        (7) Compute dot product of modified steepest descent images with error image
//                (8) Compute p by multiplying by inverse Hessian
//        (9) Update the warpW(x; p) ←W(x; p) ◦W(x;p)−1
//        Post-computation:
//        (10) Compute i using Equation (40). [Optional step]
    }


}

//http://s-pear.tistory.com/7 intensity