package com.android.hairyd;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class CameraTest extends Activity implements CvCameraViewListener2{

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

    private MenuItem mItemPreviewRGBA;
    private MenuItem mItemPreviewCanny;
    private MenuItem mItemPreviewFeatures;
    private MenuItem mItemPreviewThresh;

    int i =0;

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
        seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                text.setText(Integer.toString(progress));

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }

        });

        text = (TextView)findViewById(R.id.mainText);
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
            case VIEW_MODE_RGBA:
                mRgba = inputFrame.rgba();
                break;

            case VIEW_MODE_CANNY:
                mRgba = inputFrame.rgba();
                Imgproc.Canny(inputFrame.gray(), mIntermediateMat, 20, 60);
                Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
                break;

            case VIEW_MODE_FEATURES:
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();
                NativeFunc nativeFunc = new NativeFunc();
                nativeFunc.FindFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr());


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

}