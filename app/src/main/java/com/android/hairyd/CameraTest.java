package com.android.hairyd;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.io.File;

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

    private Mat left;
    private Mat right;
    Mat disp;

    private Mat screen[] = new Mat[3];

    private MenuItem mItemPreviewRGBA;
    private MenuItem mItemPreviewCanny;
    private MenuItem mItemPreviewFeatures;
    private MenuItem mItemPreviewThresh;

    int seek = 0;

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
        };
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
                nativeFunc.FindFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr(), seek);

                //Log.i("Android Tuorial", "OpenCV seek = " + seek);
/*
                if(seek == 0){

                    left = new Mat(mRgba.height(), mRgba.width(), CvType.CV_8UC4);
                    left = mRgba.clone();

                    seek++;
                }else if ( seek == 30 ) {

                    right = new Mat(mRgba.height(), mRgba.width(), CvType.CV_8UC4);
                    right = mRgba.clone();

                    try {

                        nativeFunc.getDisparity(left.getNativeObjAddr(), right.getNativeObjAddr(), seek);
*/

/*
                        File path =
                                Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_PICTURES);
                        String filename = "barry.png";
                        File file = new File(path, filename);

                        Boolean bool = null;
                        filename = file.toString();
                        bool = Highgui.imwrite(filename, mIntermediateMat);

                        if (bool == true) {
                            Log.d("phairy", "SUCCESS writing image to external storage");
                        } else {
                            Log.d("phairy", "Fail writing image to external storage");
                        }

                        Log.d("phairy", "SUCCESS writing image to external storage");

                    } catch (Exception e) {
                        Log.d("phairy", "Fail writing image to external storage");
                    }

                    seek ++;
                }else{
                    seek++;
                }
*/
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
        if(b.getText().toString().equals("찍기")){
            b.setText("또 찍기");

            screen[0] = mRgba.clone();

        }else if (b.getText().toString().equals("또 찍기")){
            b.setText("마지막 찍기");

            screen[1] = mRgba.clone();

        }else if (b.getText().toString().equals("마지막 찍기")){
            b.setText("잠시만 기다려주세요...");

            screen[2] = mRgba.clone();

            /*

            스레드에서 하는게 낫지 않을까?

             */

            NativeFunc nativeFunc = new NativeFunc();
            nativeFunc.getDisparity(screen[0].getNativeObjAddr(), screen[1].getNativeObjAddr(), seek);
            nativeFunc.getDisparity(screen[1].getNativeObjAddr(), screen[2].getNativeObjAddr(), seek+1);
            b.setText("완료!");

        }else {

        }
// Fatal signal 11 (SIGSEGV) at 0x7840d000 (code=1), thread 13027 (.android.hairyd)
        //stack corruption detected
        //06-04 19:54:55.671 13582-13582/com.android.hairyd A/libc: Fatal signal 6 (SIGABRT) at 0x0000350e (code=-6), thread 13582 (.android.hairyd)
    }

}