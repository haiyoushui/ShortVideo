package com.example.liuh.shortvideo;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Created by cassiopeia on 2017/2/10.
 */
public class MyMedioRecorder {
    private static final String TAG = "MainActivity";

    private Camera mCamera;

    //录制视频
    private MediaRecorder mMediaRecorder;
    private SurfaceHolder mSurfaceHolder;
    private boolean isRunning = true;
    //判断是否正在录制
    private boolean isRecording;
    //段视频保存的目录
    private File mTargetFile;
    int zoom = 0;



    public  MyMedioRecorder(SurfaceHolder smSurfaceHolder){
        mSurfaceHolder = smSurfaceHolder;
        mMediaRecorder = new MediaRecorder();
    }
    public void startPreView(SurfaceHolder holder) {
        isRunning = true;
        Log.d(TAG, "startPreView: ");
        mSurfaceHolder = holder;
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
    public void destroyPreview(){
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
    /**
     * 开始录制
     */
    public void startRecord() {
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
                mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                // 设置帧频率，然后就清晰了
                CamcorderProfile cProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);


                mMediaRecorder.setVideoSize(cProfile.videoFrameWidth, cProfile.videoFrameHeight);//设置视频分辨率大小10*1024*1024
                mMediaRecorder.setAudioEncodingBitRate(44100);
                if (cProfile.videoBitRate > 1024 * 1024)
                    mMediaRecorder.setVideoEncodingBitRate(1024*1024);
                else
                    mMediaRecorder.setVideoEncodingBitRate(cProfile.videoBitRate );//设置码率，码率越小视频越小，但是越花

                Date date = new Date();
                mTargetFile = new File(makedir(),
                        date.getHours()+"时"+date.getMinutes()+"分"+date.getSeconds()+ ".mp4");
                mMediaRecorder.setOutputFile(mTargetFile.getAbsolutePath());
                mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
                //解决录制视频, 播放器横向问题
                mMediaRecorder.setOrientationHint(0);
                // Step 4: start and return
                mMediaRecorder.prepare();
                mMediaRecorder.start();
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
            Log.i("", path + "创建成功");
        } else {
            Log.i("", path + "已经存在，无需创建");
        }
        return file;
    }
    /**
     * 停止录制 并且保存
     */
    public boolean stopRecordSave() {
        if (isRecording) {
            isRunning =false;
            mMediaRecorder.stop();
            isRecording = false;
            Log.i(TAG, "视频已经放至" + mTargetFile.getAbsolutePath());
        }
        return isRunning;
    }

    /**
     * 停止录制, 不保存
     */
    public boolean stopRecordUnSave() {
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
        return isRunning;
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




}
