package com.android.hairyd;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
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
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.VideoView;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
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
    VideoView videoView;
    ImageView animView;

    boolean threadRun = false;


    static int index = 0;
    static int cIndex = 0;

    int getIndex() {
        return ++index;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);

        Log.e(TAG, "--CameraActivity--");

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

        videoView = (VideoView) findViewById(R.id.videoView);
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                videoView.start();
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
//        surfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

    }

private SurfaceHolder.Callback surfaceListener = new SurfaceHolder.Callback() {
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        Log.i(TAG, "CameraActivity : surface Destroyed! 미리보기가 해제됩니다");

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
        Log.i(TAG, "CameraActivity : surface creating! 카메라 null이 아니면 미리보기 시작");

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
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "CameraActivity : restart...");

//        Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
//        startActivity(intent);
//        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        inUse = 0;
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);

            camera.release();
            camera = null;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        inUse = 0;
        Log.e(TAG, "CameraActivity : destroy! should file deleted?");

        finish();

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
                mrec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_1080P));  //가장 높은 화질을 고르게 선택

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


                //애니메이션 시작

                animView = (ImageView) findViewById(R.id.animView);
                animView.setVisibility(View.VISIBLE);
                animView.bringToFront();
                threadRun = true;
                animView.setImageResource(R.drawable.face);

//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            while (threadRun == true) {
//                                animView.setImageResource(R.drawable.face);
//                                Thread.sleep(1000);
//                                animView.setImageResource(R.drawable.facel1);
//                                Thread.sleep(1000);
//                                animView.setImageResource(R.drawable.facel2);
//                                Thread.sleep(1000);
//                            }
//                        } catch (Exception e) {
//                            Log.e(TAG, "CameraActivity : Animation 에러");
//                        }
//                    }
//                }).start();

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
            threadRun = false;
            try {

                //애니메이션 중단
                ImageView animView = (ImageView) findViewById(R.id.animView);
                animView.setVisibility(View.GONE);


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


            //surfaceView.setVisibility(View.INVISIBLE);

            recordButton = (Button) findViewById(R.id.recordButton);
            sendButton = (Button) findViewById(R.id.sendButton);


            videoView = (VideoView) findViewById(R.id.videoView);
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);


            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    videoView.start();
                }
            });

            try {
                videoView.setVisibility(View.VISIBLE);
                videoView.setEnabled(true);

                recordButton.setVisibility(View.VISIBLE);
                recordButton.setEnabled(true);
                sendButton.setVisibility(View.VISIBLE);
                sendButton.setEnabled(true);
                surfaceView.setVisibility(View.INVISIBLE);
                surfaceView.setBackgroundColor(Color.parseColor("#00000000"));

                playVideo();

            } catch (Exception e) {
                inUse = 0;
            }
        } else if (inUse == 2) {


            try {
                if (videoView.isPlaying() == false) {
                    playVideo();
                } else {
                    Log.i(TAG, "CameraActivity : 비디오가 재생 중입니다");
                    videoView.resume();
                }
            } catch (Exception e) {
                Log.e(TAG, "CameraActivity : 영상 재생 실패...");
            }
        }
    }

    public void playVideo() {
        Log.i(TAG, "CameraActivity : 저장된 영상을 재생, 추출합니다");
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();  //객체를 생성 해 주고
        mmr.setDataSource(mRootPath + FileName);  //파일 패스를 넣어주 다음에


        String time = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long timeInmillisec = Long.parseLong(time);
        final long interval = timeInmillisec * 1000 / 20;
//        long duration = timeInmillisec / 1000;
//        long hours = duration / 3600;
//        long minutes = (duration - hours * 3600) / 60;
//        long seconds = duration - (hours * 3600 + minutes * 60);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int i = 1;
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();  //객체를 생성 해 주고
                    mmr.setDataSource(mRootPath + FileName);  //파일 패스를 넣어주 다음에

                    while (i < 21) {

                        Bitmap bitmap = mmr.getFrameAtTime(interval * i, MediaMetadataRetriever.OPTION_CLOSEST);
                        try {
                            FileOutputStream out = new FileOutputStream(mRootPath + "/capture/" + i + ".jpg");
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        i++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "CameraActivity : 캡쳐 에러");
                }
            }
        }).start();

        try {

            Uri uri = Uri.parse(mRootPath + FileName);
            if (uri != null) {

                videoView.setVideoURI(uri);
                videoView.requestFocus();
                videoView.start();
            } else {
                Log.e(TAG, "CameraActivity : 비디오 생성 중");
            }
        } catch (Exception e) {
            Log.e(TAG, "CameraActivity : 비디오 재생 에러");
        }
    }


    public void onRecordButtonClicked(View v) {
        Log.e(TAG, "CameraActivity : 영상을 다시 촬영합니다");

        File fileName = new File(mRootPath + FileName);
        if (fileName.delete()) {
            Log.e(TAG, "CameraActivity : 영상이 삭제됨");
        } else {
            Log.e(TAG, "CameraActivity : 영상 삭제 실패!!");
        }


        Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
        startActivity(intent);
        finish();

        inUse = 0;
/*

        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if (mrec != null) {
            mrec.release();
            mrec = null;
        }

        surfaceView.setVisibility(View.VISIBLE);
        surfaceView.setEnabled(true);

        videoView.pause();
        videoView.setVisibility(View.GONE);
        videoView = null;



        recordButton.setVisibility(View.GONE);
        sendButton.setVisibility(View.GONE);

        if (camera == null) {
            try {
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                Camera.Parameters param = camera.getParameters();
                camera.setParameters(param);
                camera.setDisplayOrientation(90);
                camera.setPreviewDisplay(surfaceHolder);
                Log.d(TAG, "CameraActivity : camara ReOperating...");
                if(camera==null) Log.e(TAG, "CameraActivity : cam null");

                camera.startPreview();

            } catch (Exception e) {
                Log.i(TAG, "CameraActivity : 카메라 구동 에러");
            }
        }

*/
    }


    public void onSendButtonClicked(View v) {
        sendVideo();
    }

    public void sendVideo() {
        Log.i(TAG, "CameraActivity : 보내기 시작");
        inUse = 0;

        try {
            videoView.pause();
            videoView = null;

            Intent mIntent = new Intent(Intent.ACTION_SEND);
            //mIntent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
            mIntent.setType("video/*");
            mIntent.putExtra(Intent.EXTRA_EMAIL, "skdlflzk@naver.com");
            File f = new File(mRootPath + FileName);
            if (!f.exists()) {
                Toast.makeText(getApplicationContext(), "영상 파일이 없습니다", Toast.LENGTH_LONG).show();
            }

            Uri fileUri = Uri.fromFile(f);
            mIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            startActivityForResult(mIntent, 1001);

        } catch (Exception e) {
            Log.e(TAG, "CameraActivity : 전송 에러");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            switch (data.getIntExtra("ErrCode", 10)) {
                case 10:
                    Log.e(TAG, "CameraActivity : 디폴트");
                    Toast.makeText(getApplicationContext(), "디폴트 값?", Toast.LENGTH_LONG).show();
                    break;
                case 1:
                    Log.e(TAG, "CameraActivity : 이상한 값");
                    Toast.makeText(getApplicationContext(), "전송이 안됨?", Toast.LENGTH_LONG).show();
                    break;
                case 0:
                    Log.e(TAG, "CameraActivity : 이상한 값");
                    Toast.makeText(getApplicationContext(), "전송이 안됨?", Toast.LENGTH_LONG).show();
                    break;
                default:
                    Log.e(TAG, "CameraActivity : 이상한 값");
                    Toast.makeText(getApplicationContext(), "이상한 값?", Toast.LENGTH_LONG).show();
                    break;

            }
            Log.e(TAG, "CameraActivity :  스위치?");
        } catch (Exception e) {
            File fileName = new File(mRootPath + FileName);
            if (fileName.delete()) {
                Log.e(TAG, "CameraActivity : ActivityResult...영상이 삭제됨");
            } else {
                Log.e(TAG, "CameraActivity : 영상 삭제 실패!!");
            }
            Intent intent = getIntent();
            startActivity(intent);
            finish();
        }
    }
}

