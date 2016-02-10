package com.android.hairyd;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.util.Calendar;

public class CameraActivity extends Activity {
    //사진 찍기 가이드(1회), 촬영 후 저장, 서버로 전송(일단 파일로 아웃)
    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    MediaRecorder mrec = new MediaRecorder();

    File fRoot;
    static int inUse = 0;

    String mRootPath;
    String FileName;
    String TAG = Start.TAG;
    String VIDEOFOLDER = Start.VIDEOFOLDER;

    Button recordButton;
    Button sendButton;
    VideoView mVideoView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);

        Log.d(TAG, "--CameraActivity--");

        mRootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + VIDEOFOLDER;
        fRoot = new File(mRootPath);
        if (fRoot.exists() == false) {

            if (fRoot.mkdir() == false) {
                Log.i(TAG, "CameraActivity : 저장 폴더 생성 실패");
                return;
            }
        } else {
            //Log.i(TAG, "CameraActivity : 저장 폴더가 존재함 정상");
        }

        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        mVideoView = (VideoView) findViewById(R.id.videoView);
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mVideoView.start();
            }
        });
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceView.setClickable(true);
        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //녹화 시작
                screenClicked();
            }
        });
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(surfaceListener);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    private SurfaceHolder.Callback surfaceListener = new SurfaceHolder.Callback() {
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // TODO Auto-generated method stub
            Log.i(TAG, "CameraActivity : 미리보기 해제...");

            if (mrec != null) {
                mrec.release();
                mrec = null;
            }
            if (camera != null) {
                camera.stopPreview();
                camera.release();
                camera = null;
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // TODO Auto-generated method stub
            // 표시할 영역의 크기를 알았으므로 해당 크기로 Preview를 시작합니다.
            Log.i(TAG, "CameraActivity : surface creating...");

            if (camera != null) {
                try {
                    Camera.Parameters param = camera.getParameters();
                    camera.setParameters(param);
                    camera.setDisplayOrientation(90);

                    camera.setPreviewDisplay(holder);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "CameraActivity : 카메라 미리보기 화면 에러");
                }
            } else {
                //		Toast.makeText(getApplicationContext(), "camera not available", Toast.LENGTH_LONG).show();
                Log.e(TAG, "CameraActivity : camera not available");
                finish();
            }

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // TODO Auto-generated method stub
            if (camera != null) {
                Camera.Parameters parameters = camera.getParameters();
                parameters.setPreviewSize(width, height);
                camera.startPreview();
                Log.i(TAG, "CameraActivity : camera startPreview");
            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if (mrec != null) {
            mrec.release();
            mrec = null;
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (camera == null) {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }
    }

    public void screenClicked() {

        if (inUse == 0) {
            inUse = 1;
            Log.i(TAG, "CameraActivity : clicked! ready to record...");

            try {

                if (mrec == null) mrec = new MediaRecorder();

                camera.unlock();

                mrec.setCamera(camera);
                mrec.setPreviewDisplay(surfaceHolder.getSurface());
                mrec.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                mrec.setAudioSource(MediaRecorder.AudioSource.MIC);//오디오는 필요없음
                mrec.setOrientationHint(270);
                mrec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));

                Calendar calendar = Calendar.getInstance();
                FileName = String.format("/SH%02d%02d%02d%02d%02d%02d.mp4", calendar.get(Calendar.YEAR) % 100,
                        calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
                        calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
                        calendar.get(Calendar.SECOND));
                mrec.setOutputFile(mRootPath + FileName);
                mrec.setMaxDuration(40000);

                mrec.prepare();

                Log.d(TAG, "CameraActivity : record start!");

                mrec.start();

            } catch (Exception e) {
                Log.e(TAG, "CameraActivity : record fail");
                mrec.stop();
                mrec.reset();//없으면 mediarecorder went away with unhandled
                mrec.release();

                mrec = null;
                camera.lock();
                camera = null;
            }

        } else if (inUse == 1) {
            inUse = 2;
            try {

                Log.i(TAG, "CameraActivity : clicked! record finishing...");
                mrec.stop();
                mrec.reset();
                mrec.release();

                mrec = null;

                camera.lock();
                camera.release();
                camera = null;
            } catch (Exception e) {
                Log.e(TAG, "CameraActivity : fail to finish record...");

            } finally {
                Toast.makeText(getApplicationContext(), "../DCIM/hairyd/에 저장되었습니다", Toast.LENGTH_LONG).show();

            }

            try {
                //surfaceView.setVisibility(View.INVISIBLE);
               mVideoView.setVisibility(View.VISIBLE);

                playVideo();

                recordButton = (Button) findViewById(R.id.recordButton);
                sendButton = (Button) findViewById(R.id.sendButton);
                recordButton.setVisibility(View.VISIBLE);
                sendButton.setVisibility(View.VISIBLE);
                recordButton.setEnabled(true);
                sendButton.setEnabled(true);

            } catch (Exception e) {
                Log.e(TAG, "CameraActivity : 영상 처리 실패...");
            }
        } else if (inUse == 2) {
            playVideo();
        }
    }

    public void onRecordButtonClicked(View v) {
        Log.e(TAG, "CameraActivity : 영상을 다시 촬영합니다");

        mVideoView.setVisibility(View.INVISIBLE);
        inUse = 0;

        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if (mrec != null) {
            mrec.release();
            mrec = null;
        }

//        surfaceView.setVisibility(View.VISIBLE);

        if (camera == null) {
            try {
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                Camera.Parameters param = camera.getParameters();
                camera.setParameters(param);
                camera.setDisplayOrientation(90);
                camera.setPreviewDisplay(surfaceHolder);

                if(camera==null) Log.e(TAG, "CameraActivity : cam null");

                camera.startPreview();

            } catch (Exception e) {
                Log.i(TAG, "CameraActivity : 카메라 구동 에러");
            }
        }

        recordButton.setVisibility(View.INVISIBLE);
        sendButton.setVisibility(View.INVISIBLE);
        recordButton.setEnabled(false);
        sendButton.setEnabled(false);

    }

    public void onSendButtonClicked(View v) {
        sendVideo();
    }

    public void sendVideo() {
        Log.i(TAG, "CameraActivity : 보내기 시작");
        try {
            Intent mIntent = new Intent(Intent.ACTION_SEND);
            //mIntent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
            mIntent.setType("video/*");
            mIntent.putExtra(Intent.EXTRA_EMAIL,"skdlflzk@naver.com");
            File f = new File(mRootPath + FileName);
            if(!f.exists()){
                Toast.makeText(getApplicationContext(), "영상 파일이 없습니다", Toast.LENGTH_LONG).show();
            }
            Uri fileUri = Uri.fromFile(f);
            mIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            startActivity(mIntent);
        } catch (Exception e) {
            Log.e(TAG, "CameraActivity : 전송 에러");
        }
    }

    public void playVideo() {
        Log.i(TAG, "CameraActivity : 저장된 영상을 재생합니다");

        try {
            mVideoView.setVideoURI(Uri.parse(mRootPath + FileName));
            mVideoView.requestFocus();
            mVideoView.start();
        } catch (Exception e) {
            Log.i(TAG, "CameraActivity : 비디오 재생 에러");
        }
    }
}

