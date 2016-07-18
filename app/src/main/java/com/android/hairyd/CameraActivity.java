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

import org.opencv.android.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class CameraActivity extends Activity {

    String TAG = Start.TAG;
    private static final int RQS_LOADIMAGE = 1;
    private Button btnLoad, btnDetFace;
    private ImageView imgView;
    private Bitmap myBitmap;


    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;

    Camera.Size size;

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


        btnLoad = (Button)findViewById(R.id.btnLoad);
        btnDetFace = (Button)findViewById(R.id.btnDetectFace);
        imgView = (ImageView)findViewById(R.id.imgview);

        btnLoad.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, RQS_LOADIMAGE);
            }
        });

        btnDetFace.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                if(myBitmap != null){
                    int size = detectFace();
                    Toast.makeText(getApplicationContext(),"Done ,"+size, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),"data == null",Toast.LENGTH_LONG).show();
                }
            }
        });


        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {

//                Camera.Parameters parameters = camera.getParameters();
//                List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();

                Integer width=0;
                Integer height=0;

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

                Log.i(TAG, "CameraActivity: Detecting... ," + facecount );

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
                && resultCode == RESULT_OK){

            if(myBitmap != null){
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
    private int detectFace(){

        //Create a Paint object for drawing with
        Paint myRectPaint = new Paint();
        myRectPaint.setStrokeWidth(5);
        myRectPaint.setColor(Color.RED);
        myRectPaint.setStyle(Paint.Style.STROKE);

        //Create a Canvas object for drawing on
        Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);

        Log.i("Phairy", "detectFace_ loaded successfully. 크기 "+myBitmap.getWidth() + ", "+myBitmap.getHeight());

        Canvas tempCanvas = new Canvas(tempBitmap);
        tempCanvas.drawBitmap(myBitmap, 0, 0, null);

        //Detect the Faces
        FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext()) .setLandmarkType(FaceDetector.ALL_LANDMARKS) .build();

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
        for(int i=0; i<faces.size(); i++) {
            Face thisFace = faces.valueAt(i);
            float x1 = thisFace.getPosition().x;
            float y1 = thisFace.getPosition().y;
            float x2 = x1 + thisFace.getWidth();
            float y2 = y1 + thisFace.getHeight();
            tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);

            List<Landmark> landmarks = thisFace.getLandmarks();
            for(int l=0; l<landmarks.size(); l++){
                PointF pos = landmarks.get(l).getPosition();
                tempCanvas.drawPoint(pos.x, pos.y, landmarksPaint);
            }

            Log.i("Phairy", "y각 " + thisFace.getEulerY() + "도, z각 " + thisFace.getEulerZ() + "도 너비 = " + thisFace.getWidth() + ", 높이 = " + thisFace.getHeight()+", 특징점 개수 "+landmarks.size() );
        }
        imgView.setImageDrawable(new BitmapDrawable(getResources(),tempBitmap));

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

                if (r < 0) r = 0; else if (r > 262143) r = 262143;
                if (g < 0) g = 0; else if (g > 262143) g = 262143;
                if (b < 0) b = 0; else if (b > 262143) b = 262143;

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
                    int min=1000;
                    Integer width=0;
                    Integer height=0;
                    for (int i =0; i < sizeList.size() ; i++) {
                        Camera.Size tempSize = sizeList.get(i);


                        if(tempSize.width < min) {
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



}