package com.android.hairyd;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.File;

public class CameraTest extends AppCompatActivity implements CvCameraViewListener2{

    private CameraBridgeViewBase mOpenCvCameraView;
    private SeekBar seekbar;
    private TextView text;

    private static final int VIEW_MODE_RGBA = 0;
    private static final int VIEW_MODE_CANNY = 1;
    private static final int VIEW_MODE_FEATURES = 2;
    private static final int VIEW_MODE_THRESH = 3;

    private int mViewMode;

    private Mat mRgba;
    private Mat mIntermediateMat;
    private Mat mGray;

    private Mat left;
    private Mat right;
    Mat disp;

    private Mat screen[] = new Mat[3];

    private MenuItem mItemPreviewRGBA;
    private MenuItem mItemPreviewCanny;
    private MenuItem mItemPreviewFeatures;
    private MenuItem mItemPreviewThresh;

    int seek = 0;

    FaceDetector faceDetector;

    TextView tView;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this){
        @Override
        public void onManagerConnected(int status){
            switch (status){
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("Android Tutorial", "OpenCV loaded successfully");
                    System.loadLibrary("NativeModule");
                    mOpenCvCameraView.enableView();
                } break;
                default: {
                    super.onManagerConnected(status);
                }break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.surfaceView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);


        seekbar = (SeekBar)findViewById(R.id.seekBar);
        seekbar.setMax(255);
        seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                text.setText(Integer.toString(progress));
                seek = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }

        });

        text = (TextView)findViewById(R.id.mainText);
        tView = (TextView)findViewById(R.id.textView);
        faceDetector = new
                FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false)
                .build();
        if(!faceDetector.isOperational()){
            new AlertDialog.Builder(getApplicationContext()).setMessage("Could not set up the face detector!").show();
            return;
        }



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mItemPreviewRGBA = menu.add("Preview RGBA");
        mItemPreviewCanny = menu.add("Canny Edges");
        mItemPreviewFeatures = menu.add("Find Features");
        mItemPreviewThresh = menu.add("Threshold");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        if (item == mItemPreviewRGBA){
            mViewMode = VIEW_MODE_RGBA;
        } else if (item == mItemPreviewCanny){
            mViewMode = VIEW_MODE_CANNY;
        } else if (item == mItemPreviewFeatures){
            mViewMode = VIEW_MODE_FEATURES;
        } else if (item == mItemPreviewThresh){
            mViewMode = VIEW_MODE_THRESH;
        }

        return true;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);

        for(int i=0; i <3 ; i++){
            screen[i] = new Mat(height, width, CvType.CV_8UC4);
        }
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mIntermediateMat.release();
        mGray.release();
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        final int viewMode = mViewMode;
        switch (viewMode){
            case VIEW_MODE_FEATURES:

                mRgba = inputFrame.rgba();

                break;

            case VIEW_MODE_CANNY:

                mRgba = inputFrame.rgba();
                Imgproc.Canny(inputFrame.gray(), mIntermediateMat, 20, 60);
                Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);

              break;

            case  VIEW_MODE_RGBA:
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();

                NativeFunc nativeFunc = new NativeFunc();
//                nativeFunc.FindFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr(), seek);
//
//
//
//
//                Bitmap bitmap =Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
//                Utils.matToBitmap( mRgba ,bitmap);
//
//                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
//                SparseArray<Face> faces = faceDetector.detect(frame);
//
//                //Draw Rectangles on the Faces
//
//
//
//                for(int i=0; i<faces.size(); i++) {
//                    Face thisFace = faces.valueAt(i);
//                    float x1 = thisFace.getPosition().x;
//                    float y1 = thisFace.getPosition().y;
//                    float x2 = x1 + thisFace.getWidth();
//                    float y2 = y1 + thisFace.getHeight();
//
//                    nativeFunc.drawPoint( mRgba.getNativeObjAddr(), x1, y1);
//                    nativeFunc.drawPoint( mRgba.getNativeObjAddr(), x2,y2);
//
//
//                }
//
//                if(faces.size()!=0) {
//                    tView.setText("인식 수 = " + faces.size() + "/ y 각 -" + faces.valueAt(0).getEulerY() + ", z 각 -"+ faces.valueAt(0).getEulerZ());
//
//                }
                break;

            case VIEW_MODE_THRESH:
                Imgproc.threshold(inputFrame.gray(), mRgba, seekbar.getProgress(),
                        seekbar.getMax(), Imgproc.THRESH_BINARY);
                break;
        }

        return mRgba;
    }


    @Override
    public void onPause(){
        super.onPause();
        if (mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
    }

    public void onRecordButtonClicked(View view){
        Button b =(Button) findViewById(R.id.recordButton);





//        if(b.getText().toString().equals("찍기")){
//            b.setText("또 찍기");
//
//            screen[0] = mRgba.clone();
//
//        }else if (b.getText().toString().equals("또 찍기")){
//            b.setText("마지막 찍기");
//
//            screen[1] = mRgba.clone();
//
//        }else if (b.getText().toString().equals("마지막 찍기")){
//            b.setText("잠시만 기다려주세요...");
//
//            screen[2] = mRgba.clone();
//
//            /*
//
//            스레드에서 하는게 낫지 않을까?
//
//             */
//
//            NativeFunc nativeFunc = new NativeFunc();
//            nativeFunc.getDisparity(screen[0].getNativeObjAddr(), screen[1].getNativeObjAddr(), 1);
//            nativeFunc.getDisparity(screen[1].getNativeObjAddr(), screen[2].getNativeObjAddr(), 2);
//            b.setText("완료!");
//
//        }else {
//
//        }
    }

}