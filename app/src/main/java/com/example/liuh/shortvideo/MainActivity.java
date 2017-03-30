package com.example.liuh.shortvideo;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnTouchListener, BothWayProgressBar.OnProgressEndListener {

    private static final int LISTENER_START = 200;
    private static final String TAG = "MainActivity";
    //预览SurfaceView
    private SurfaceView mSurfaceView;
    private Camera mCamera;
    //底部"按住拍"按钮
    private View mStartButton;
    //进度条
    private BothWayProgressBar mProgressBar;
    //进度条线程
    private Thread mProgressThread;
    //录制视频
    private MediaRecorder mMediaRecorder;
    private SurfaceHolder mSurfaceHolder;
    //屏幕分辨率
    private int videoWidth, videoHeight;
    //判断是否正在录制
    private boolean isRecording;
    //段视频保存的目录
    private File mTargetFile;
    //当前进度/时间
    private int mProgress;
    //录制最大时间
    public static final int MAX_TIME = 10;
    //是否上滑取消
    private boolean isCancel;
    private CamcorderProfile cProfile;
    //是否放大
    private boolean isZoomIn = false;
    double cha2;
    int zoom = 0;
    private MyHandler mHandler;
    private TextView mTvTip;
    private boolean isRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        cProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        videoWidth = cProfile.videoFrameWidth;
        videoHeight = cProfile.videoFrameHeight;
        mSurfaceView = (SurfaceView) findViewById(R.id.main_surface_view);

        /**
         * 单独处理mSurfaceView的双触电缩放功能
         */
        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        cha2 = 0;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    case MotionEvent.ACTION_UP:
                        cha2 = 0;
                        break;
                }
                //多点触摸
                if (MotionEventCompat.getPointerCount(event) > 1) {
                    float touchX_1 = MotionEventCompat.getX(event, 0);
                    float touchY_1 = MotionEventCompat.getY(event, 0);
                    float touchX_2 = MotionEventCompat.getX(event, 1);
                    float touchY_2 = MotionEventCompat.getY(event, 1);
                    double cha = Math.sqrt(Math.pow( Math.abs(touchX_1-touchX_2),2)+ Math.pow( Math.abs(touchY_1-touchY_2),2));
                    if(cha2!=0.0){
                        double fangda = cha-cha2;
                        if(Math.abs(fangda)>8){
                            setZoom(zoom+(new Double(fangda*11/100).intValue()));

                        }
                    }
                    cha2=cha;
                } else {
                    //单点触摸
//                    float touchX = MotionEventCompat.getX(event, actionIndex);
//                    float touchY = MotionEventCompat.getY(event, actionIndex);
//
                }
                return true;
            }
        });

        mSurfaceHolder = mSurfaceView.getHolder();
        //设置屏幕分辨率
        mSurfaceHolder.setFixedSize(videoWidth, videoHeight);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);
        mStartButton = findViewById(R.id.main_press_control);
        mTvTip = (TextView) findViewById(R.id.main_tv_tip);

        mStartButton.setOnTouchListener(this);
        mProgressBar = (BothWayProgressBar) findViewById(R.
                id.main_progress_bar);
        mProgressBar.setOnProgressEndListener(this);
        mHandler = new MyHandler(this);
        mMediaRecorder = new MediaRecorder();
//      Button button = (Button)findViewById(R.id.button);
//        button.setVisibility(View.GONE);
//        button.setOnClickListener(new View.OnClickListener() {
//          @Override
//          public void onClick(View v) {
//              Intent intent = new Intent();
//              intent.setClass(MainActivity.this,Main2Activity.class);
//              startActivity(intent);
//          }
//      });

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");
    }

    ///////////////////////////////////////////////////////////////////////////
    // SurfaceView回调
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        startPreView(holder);
    }

    /**
     * 开启预览
     *
     * @param holder
     */
    private void startPreView(SurfaceHolder holder) {
        Log.d(TAG, "startPreView: ");

        if (mCamera == null) {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }
        if (mCamera != null) {
            mCamera.setDisplayOrientation(90);
            try {
                mCamera.setPreviewDisplay(holder);
                Camera.Parameters parameters = mCamera.getParameters();
                //实现Camera自动对焦
                List<String> focusModes = parameters.getSupportedFocusModes();
                if (focusModes != null) {
                    for (String mode : focusModes) {
                        mode.contains("continuous-video");
                        parameters.setFocusMode("continuous-video");
                    }
                }
                mCamera.setParameters(parameters);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            Log.d(TAG, "surfaceDestroyed: ");
            //停止预览并释放摄像头资源
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // 进度条结束后的回调方法
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onProgressEndListener() {
        //视频停止录制
        stopRecordSave();

    }

    /**
     * 开始录制
     */
    private void startRecord() {
        if (mMediaRecorder != null) {
            //没有外置存储, 直接停止录制
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return;
            }
            try {
                //mMediaRecorder.reset();
                mCamera.unlock();
                mMediaRecorder.setCamera(mCamera);
                //从相机采集视频
                mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                // 从麦克采集音频信息
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                //  设置视频格式
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                //每秒的帧数
         //       mMediaRecorder.setVideoFrameRate(16);
                //编码格式
                mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                // 设置帧频率，然后就清晰了
                mMediaRecorder.setVideoSize(cProfile.videoFrameWidth, cProfile.videoFrameHeight);//设置视频分辨率大小10*1024*1024
                mMediaRecorder.setAudioEncodingBitRate(44100);
                System.out.println(1* 1024 * 1024+"##########"+cProfile.videoBitRate);
                if (cProfile.videoBitRate >1* 1024 * 1024)
                    mMediaRecorder.setVideoEncodingBitRate(1*1024*1024);
                else
                mMediaRecorder.setVideoEncodingBitRate(cProfile.videoBitRate );
                Intent intent = new Intent();
                Date date = new Date();
                mTargetFile = new File(makedir(),
                        date.getHours()+"时"+date.getMinutes()+"分"+date.getSeconds()+ ".mp4");
                mMediaRecorder.setOutputFile(mTargetFile.getAbsolutePath());
                mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
                //解决录制视频, 播放器横向问题
                mMediaRecorder.setOrientationHint(90);
                // Step 4: start and return
                mMediaRecorder.prepare();
                mMediaRecorder.start();




//                mCamera.unlock();
//                mMediaRecorder.setCamera(mCamera);
//                mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
//
//                // Step 2: Set sources
//                mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);//before setOutputFormat()
//                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);//before setOutputFormat()
//
//                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//
//                //设置视频输出的格式和编码
//                CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
//                //                mMediaRecorder.setProfile(mProfile);
//                mMediaRecorder.setVideoSize(640, 480);//after setVideoSource(),after setOutFormat()
//                mMediaRecorder.setAudioEncodingBitRate(44100);
//                if (mProfile.videoBitRate > 2 * 1024 * 1024)
//                    mMediaRecorder.setVideoEncodingBitRate(2 * 1024 * 1024);
//                else
//                    mMediaRecorder.setVideoEncodingBitRate(mProfile.videoBitRate);
//                mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);//after setVideoSource(),after setOutFormat()
//
//                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);//after setOutputFormat()
//                mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);//after setOutputFormat()
//
//                //mMediaRecorder.setVideoEncodingBitRate(800);
//
//                Date date = new Date();
//                mTargetFile = new File(makedir(),
//                        date.getHours()+"时"+date.getMinutes()+"分"+date.getSeconds()+ ".mp4");
//                mMediaRecorder.setOutputFile(mTargetFile.getAbsolutePath());
//
//                mMediaRecorder.prepare();
//                mMediaRecorder.start();
                isRecording = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public File makedir() {
        String path = Environment.getExternalStorageDirectory()+"/MOVIE";

        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
            Log.i(TAG, path + "创建成功");
        } else {
            Log.i(TAG, path + "已经存在，无需创建");
        }
        return file;
    }
    /**
     * 停止录制 并且保存
     */
    private void stopRecordSave() {
        if (isRecording) {
            isRunning = false;
            mMediaRecorder.stop();
            isRecording = false;
            System.out.println("#########@@@");
            Log.i(TAG, "视频已经放至" + mTargetFile.getAbsolutePath());
        }
    }

    /**
     * 停止录制, 不保存
     */
    private void stopRecordUnSave() {
        if (isRecording) {
            isRunning = false;
            try{
              Thread.sleep(500);
            }catch (Exception e){
            }
            mMediaRecorder.stop();
            isRecording = false;
            if (mTargetFile.exists()) {
                //不保存直接删掉
                mTargetFile.delete();
            }
        }
    }

    /**
     * 相机变焦
     *
     * @param zoomValue
     */
    public void setZoom(int zoomValue) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.isZoomSupported()) {//判断是否支持
                int maxZoom = parameters.getMaxZoom();
                if(0<=zoomValue&&zoomValue<=maxZoom){
                    parameters.setZoom(zoomValue);
                    mCamera.setParameters(parameters);
                    zoom = zoomValue;
                }

            }
        }

    }


    ///////////////////////////////////////////////////////////////////////////
    // Handler处理                                                          //
    ///////////////////////////////////////////////////////////////////////////
    private static class MyHandler extends Handler {
        private WeakReference<MainActivity> mReference;
        private MainActivity mActivity;

        public MyHandler(MainActivity activity) {
            mReference = new WeakReference<MainActivity>(activity);
            mActivity = mReference.get();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    mActivity.mProgressBar.setProgress(mActivity.mProgress);
                    break;
            }

        }
    }

    /**
     * 触摸事件的触发
     *
     * @param v
     * @param event
     * @return
     */
    double nLenStart = 0;
    @Override
    public boolean onTouch(View v, MotionEvent event) {


        boolean ret = false;
        int action = event.getAction();
        float ey = event.getY();
        float ex = event.getX();
        //只监听中间的按钮处
        int vW = v.getWidth();
        int left = LISTENER_START;
        int right = vW - LISTENER_START;

        float downY = 0;

        switch (v.getId()) {
            case R.id.main_press_control: {
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        if (ex > left && ex < right) {
                            mProgressBar.setCancel(false);
                            //显示上滑取消
                            mTvTip.setVisibility(View.VISIBLE);
                            mTvTip.setText("↑ 上滑取消");
                            //记录按下的Y坐标
                            downY = ey;
                            // TODO: 2016/10/20 开始录制视频, 进度条开始走
                            mProgressBar.setVisibility(View.VISIBLE);
                            //开始录制
                            Log.i(TAG, "开始录制");
                            startRecord();

                            mProgressThread = new Thread() {
                                @Override
                                public void run() {
                                    super.run();
                                    try {
                                        mProgress = 0;
                                        isRunning = true;
                                        while (isRunning) {
                                            mProgress++;
                                            mHandler.obtainMessage(0).sendToTarget();
                                            Thread.sleep(10);//设置拍摄时间。因为把描绘进度条分成1000份，所以设置的时间是指在此时间段中将mProgress累加到1000.
                                        }
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            };

                            mProgressThread.start();
                            ret = true;
                        }


                        break;
                    case MotionEvent.ACTION_UP:
                            mTvTip.setVisibility(View.INVISIBLE);
                            mProgressBar.setVisibility(View.INVISIBLE);
                            //判断是否为录制结束, 或者为成功录制(时间过短)
                            if (!isCancel) {
                                if (mProgress < 120) {
                                    //时间太短不保存
                                    stopRecordUnSave();
                                    Toast.makeText(this, "时间太短", Toast.LENGTH_SHORT).show();
                                    break;
                                }
                                //停止录制
                                stopRecordSave();
                            } else {
                                //现在是取消状态,不保存
                                stopRecordUnSave();
                                isCancel = false;
                                Log.i(TAG, "取消录制");
                                mProgressBar.setCancel(false);
                            }
                            ret = false;

                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (ex > left && ex < right) {
                            float currentY = event.getY();
                            if (downY - currentY > 10) {
                                isCancel = true;
                                mProgressBar.setCancel(true);
                            }
                        }
                        break;
                }
                break;

            }

        }
        return ret;
    }
}
