package com.example.liuh.shortvideo;

import android.content.pm.ActivityInfo;
import android.media.CamcorderProfile;
import android.os.Bundle;
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

import java.lang.ref.WeakReference;


public class Main2Activity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnTouchListener, BothWayProgressBar.OnProgressEndListener {

    private static final int LISTENER_START = 200;
    private static final String TAG = "MainActivity";
    //预览SurfaceView
    private SurfaceView mSurfaceView;
    //底部"按住拍"按钮
    private View mStartButton;
    //进度条
    private BothWayProgressBar mProgressBar;
    //进度条线程
    private Thread mProgressThread;
    //录制视频
    private SurfaceHolder mSurfaceHolder;
    //屏幕分辨率
    private int videoWidth, videoHeight;
    //当前进度/时间
    private int mProgress;

    //是否上滑取消
    private boolean isCancel;

    double cha2;
    int zoom = 0;
    private MyHandler mHandler;
    private TextView mTvTip;
    private boolean isRunning;
    private MyMedioRecorder myMedioRecorder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        videoWidth = mProfile.videoFrameWidth;
        videoHeight = mProfile.videoFrameHeight;
        mSurfaceView = (SurfaceView) findViewById(R.id.main_surface_view);

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
        myMedioRecorder = new MyMedioRecorder(mSurfaceHolder);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        myMedioRecorder.startPreView(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
      myMedioRecorder.destroyPreview();
    }

    @Override
    public void onProgressEndListener() {
        //视频停止录制
       myMedioRecorder.stopRecordSave();

    }

    private static class MyHandler extends Handler {
        private WeakReference<Main2Activity> mReference;
        private Main2Activity mActivity;

        public MyHandler(Main2Activity activity) {
            mReference = new WeakReference<Main2Activity>(activity);
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
            //点击画面
            case R.id.main_surface_view:
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
                        if(Math.abs(fangda)>8)
                            myMedioRecorder.setZoom(zoom+(new Double(fangda*11/100).intValue()));
                    }
                    cha2=cha;
                } else {
                    //单点触摸
//                    float touchX = MotionEventCompat.getX(event, actionIndex);
//                    float touchY = MotionEventCompat.getY(event, actionIndex);
                }
                break;
            //点击按钮
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
                            //记录按下的Y坐标
                            // TODO: 2016/10/20 开始录制视频, 进度条开始走
                            mProgressBar.setVisibility(View.VISIBLE);
                            //开始录制
                            Log.i(TAG, "开始录制");
                            myMedioRecorder.startRecord();

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

//                            mTvTip.setVisibility(View.INVISIBLE);
                            mProgressBar.setVisibility(View.INVISIBLE);
                            //判断是否为录制结束, 或者为成功录制(时间过短)
                            if (!isCancel) {
                                if (mProgress < 120) {
                                    //时间太短不保存
                                    myMedioRecorder.stopRecordUnSave();
                                    Toast.makeText(this, "时间太短", Toast.LENGTH_SHORT).show();
                                    break;
                                }
                                //停止录制
                                isRunning = myMedioRecorder.stopRecordSave();
                            } else {
                                //现在是取消状态,不保存
                                myMedioRecorder.stopRecordUnSave();
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
