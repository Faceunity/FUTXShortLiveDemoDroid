package com.tencent.qcloud.xiaoshipin.videoeditor;

import android.app.ProgressDialog;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.utils.TCConstants;
import com.tencent.qcloud.xiaoshipin.common.widget.VideoWorkProgressFragment;
import com.tencent.qcloud.xiaoshipin.login.TCUserMgr;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.TCConfirmDialog;
import com.tencent.qcloud.xiaoshipin.videoeditor.cutter.TCVideoEditView;
import com.tencent.qcloud.xiaoshipin.videoeditor.utils.DialogUtil;
import com.tencent.qcloud.xiaoshipin.videoeditor.utils.Edit;
import com.tencent.qcloud.xiaoshipin.videoeditor.utils.PlayState;
import com.tencent.ugc.TXVideoEditConstants;
import com.tencent.ugc.TXVideoEditer;
import com.tencent.ugc.TXVideoInfoReader;

import java.io.IOException;
import java.lang.ref.WeakReference;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by hans on 2017/11/7.
 * 裁剪视频Activity
 */
public class TCVideoCutterActivity extends FragmentActivity implements
        View.OnClickListener,
        TCVideoEditerWrapper.TXVideoPreviewListenerWrapper,
        TXVideoEditer.TXVideoProcessListener {
    private static final String TAG = "TCVideoCutterActivity";
    private String mInVideoPath;                                // 编辑的视频源路径

    private int mCurrentState = -1;       // 播放器当前状态

    private TXVideoEditer mTXVideoEditer;                       // SDK接口类
    private TXVideoInfoReader mTXVideoInfoReader;

    private VideoWorkProgressFragment mWorkProgressFragment;    // 生成进度条
    private VideoMainHandler mVideoMainHandler;                 // 加载完信息后的回调主线程Hanlder
    private Thread mLoadBackgroundThread;                       // 后台加载视频信息的线程
    private int mVideoResolution = -1;                          // 视频分辨率相关（从录制过来的这个参数才有效） -1说明不是从录制过来的
    private boolean mGenerateSuccess;                           // 是否预处理成功
    private int mVideoFrom;
    private int mCustomBitrate;
    private long mCutterStartTime;
    private long mCutterEndTime;

    private ImageView mBtnBack;
    private Button mBtnNext;
    private FrameLayout mPlayer;
    private TCVideoEditView mTCVideoEditView;
    private TextView mTvChoose;

    private ProgressDialog mLoadingProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_cutter);
        TCVideoEditerWrapper.getInstance().clear();

        mInVideoPath = getIntent().getStringExtra(TCConstants.VIDEO_EDITER_PATH);
        if (TextUtils.isEmpty(mInVideoPath)) {
            Toast.makeText(this, "发生未知错误,路径不能为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mVideoResolution = getIntent().getIntExtra(TCConstants.VIDEO_RECORD_RESOLUTION, -1);
        mCustomBitrate = getIntent().getIntExtra(TCConstants.RECORD_CONFIG_BITE_RATE, 0);
        mVideoFrom = getIntent().getIntExtra(TCConstants.VIDEO_RECORD_TYPE, TCConstants.VIDEO_RECORD_TYPE_EDIT);

        mTXVideoEditer = new TXVideoEditer(this);
        int ret = mTXVideoEditer.setVideoPath(mInVideoPath);
        mTXVideoInfoReader = TXVideoInfoReader.getInstance();

        TCVideoEditerWrapper wrapper = TCVideoEditerWrapper.getInstance();
        wrapper.setEditer(mTXVideoEditer);

        initViews();
        initPhoneListener();

        // 开始加载视频信息
        mVideoMainHandler = new VideoMainHandler(this);
        mLoadBackgroundThread = new Thread(new LoadVideoRunnable(this));
        mLoadBackgroundThread.start();

        mLoadingProgressDialog = new ProgressDialog(this);
        mLoadingProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);// 设置进度条的形式为圆形转动的进度条
        mLoadingProgressDialog.setCancelable(false);// 设置是否可以通过点击Back键取消
        mLoadingProgressDialog.setCanceledOnTouchOutside(false);// 设置在点击Dialog外是否取消Dialog进度条

        mLoadingProgressDialog.show();
    }

    private void initViews() {
        mBtnBack = (ImageView) findViewById(R.id.btn_back);
        mBtnNext = (Button) findViewById(R.id.btn_next);
        mPlayer = (FrameLayout) findViewById(R.id.layout_palyer);
        mTvChoose = (TextView) findViewById(R.id.tv_choose_duration);
        mTCVideoEditView = (TCVideoEditView) findViewById(R.id.video_edit_view);
        mTCVideoEditView.setCutChangeListener(mCutChangeListener);
        mTCVideoEditView.setVisibility(View.GONE);

        mBtnBack.setOnClickListener(this);
        mBtnNext.setOnClickListener(this);
    }

    private void initWorkLoadingProgress() {
        if (mWorkProgressFragment == null) {
            mWorkProgressFragment = VideoWorkProgressFragment.newInstance("视频预处理中...");
            mWorkProgressFragment.setOnClickStopListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cancelProcessVideo();
                    if (mCurrentState == PlayState.STATE_STOP) {
                        if (mTXVideoEditer != null) {
                            mTXVideoEditer.startPlayFromTime(mCutterStartTime, mCutterEndTime);
                            mCurrentState = PlayState.STATE_PLAY;
                        }
                    }
                }
            });
        }
        mWorkProgressFragment.setProgress(0);
        mWorkProgressFragment.show(getSupportFragmentManager(), "work_progress");
    }

    @Override
    protected void onResume() {
        super.onResume();
        TCVideoEditerWrapper.getInstance().addTXVideoPreviewListenerWrapper(this);
        if (mCurrentState == PlayState.STATE_STOP) {
            if (mTXVideoEditer != null) {
                mTXVideoEditer.startPlayFromTime(mCutterStartTime, mCutterEndTime);
                mCurrentState = PlayState.STATE_PLAY;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        TCVideoEditerWrapper.getInstance().removeTXVideoPreviewListenerWrapper(this);
        if (mCurrentState == PlayState.STATE_PLAY) {
            if (mTXVideoEditer != null) {
                mTXVideoEditer.stopPlay();
                mCurrentState = PlayState.STATE_STOP;
            }
        }

        cancelProcessVideo();
    }

    private void loadVideoSuccess(TXVideoEditConstants.TXVideoInfo videoInfo) {
        if (mLoadingProgressDialog != null) {
            mLoadingProgressDialog.dismiss();
        }
        int duration = (int) (videoInfo.duration / 1000); //s
        int thumbCount = duration / 3;
        mTXVideoInfoReader.getSampleImages(thumbCount, mInVideoPath, mOnSampleProcessListener);
        mTCVideoEditView.setMediaFileInfo(videoInfo);

        TXVideoEditConstants.TXPreviewParam param = new TXVideoEditConstants.TXPreviewParam();
        param.renderMode = TXVideoEditConstants.PREVIEW_RENDER_MODE_FILL_EDGE;
        param.videoView = mPlayer;
        mTXVideoEditer.initWithPreview(param);

        mCutterStartTime = 0;
        mCutterEndTime = videoInfo.duration;

        mTXVideoEditer.startPlayFromTime(0, videoInfo.duration);
        mCurrentState = PlayState.STATE_PLAY;

        if (duration >= 16) {
            duration = 16;
        }
        mTvChoose.setText("已选取" + String.valueOf(duration) + "s");
        mTCVideoEditView.setCount(thumbCount);
        mTCVideoEditView.setVisibility(View.VISIBLE);
    }

    /**
     * ===========================================开始预处理相关 ===========================================
     */
    private void startProcess() {
        mTXVideoEditer.stopPlay();
        mCurrentState = PlayState.STATE_STOP;

        initWorkLoadingProgress();
        mWorkProgressFragment.setProgress(0);
        mTXVideoEditer.setVideoProcessListener(this);

        int thumbnailCount = (int) (mCutterEndTime - mCutterStartTime) / 1000;
        Log.i(TAG, "thumbnailCount:" + thumbnailCount);

        TXVideoEditConstants.TXThumbnail thumbnail = new TXVideoEditConstants.TXThumbnail();
        thumbnail.count = thumbnailCount;
        thumbnail.width = 100;
        thumbnail.height = 100;

        mTXVideoEditer.setThumbnail(thumbnail);
        mTXVideoEditer.setThumbnailListener(mThumbnailListener);
        mTXVideoEditer.setCutFromTime(mTCVideoEditView.getSegmentFrom(), mTCVideoEditView.getSegmentTo());
        mTXVideoEditer.processVideo();
    }

    private TXVideoEditer.TXThumbnailListener mThumbnailListener = new TXVideoEditer.TXThumbnailListener() {
        @Override

        public void onThumbnail(int index, long timeMs, Bitmap bitmap) {
            Log.i(TAG, "onThumbnail index：" + index + ",timeMs:" + timeMs);
            TCVideoEditerWrapper.getInstance().addThumbnailBitmap(timeMs, bitmap);
        }
    };

    @Override
    public void onProcessProgress(final float progress) {
        Log.i(TAG, "onProcessProgress: progress = " + progress);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWorkProgressFragment.setProgress((int) (progress * 100));
            }
        });
    }

    @Override
    public void onProcessComplete(final TXVideoEditConstants.TXGenerateResult result) {
        Log.i(TAG, "onProcessComplete");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mWorkProgressFragment != null && mWorkProgressFragment.isAdded()) {
                    mWorkProgressFragment.dismiss();
                }
                if (result.retCode == TXVideoEditConstants.GENERATE_RESULT_OK) {
                    startEditActivity();
                    mGenerateSuccess = true;
                } else {
                    TCConfirmDialog confirmDialog = TCConfirmDialog.newInstance("错误", result.descMsg, false, "取消", "取消");
                    confirmDialog.setCancelable(false);
                    confirmDialog.setOnConfirmCallback(new TCConfirmDialog.OnConfirmCallback() {
                        @Override
                        public void onSureCallback() {
                            finish();
                        }

                        @Override
                        public void onCancelCallback() {
                        }
                    });
                    confirmDialog.show(getSupportFragmentManager(), "confirm_dialog");
                }
            }
        });
    }

    /**
     * ===========================================播放器预览相关 ===========================================
     */
    private void startEditActivity() {
        // 更新一下VideoInfo的时间
        Intent intent = new Intent(this, TCVideoEditerActivity.class);
        // 如果是从录制过来的话，需要传递一个分辨率参数下去。
        intent.putExtra(TCConstants.VIDEO_RECORD_RESOLUTION, mVideoResolution);
        intent.putExtra(TCConstants.VIDEO_RECORD_TYPE, mVideoFrom);
        intent.putExtra(TCConstants.VIDEO_EDITER_PATH, mInVideoPath);
        intent.putExtra(TCConstants.RECORD_CONFIG_BITE_RATE, mCustomBitrate);
        startActivity(intent);
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPhoneListener != null) {
            TelephonyManager tm = (TelephonyManager) this.getApplicationContext().getSystemService(Service.TELEPHONY_SERVICE);
            tm.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
        }
        if (mLoadBackgroundThread != null && !mLoadBackgroundThread.isInterrupted() && mLoadBackgroundThread.isAlive()) {
            mLoadBackgroundThread.interrupt();
            mLoadBackgroundThread = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mTXVideoEditer != null) {
            mTXVideoEditer.stopPlay();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back:// 返回
                onBackPressed();
                finish();
                break;
            case R.id.btn_next:// 开始预处理
                startProcess();
                break;
        }
    }


    /**
     * 取消预处理视频
     */
    private void cancelProcessVideo() {
        if (!mGenerateSuccess) {
            if (mWorkProgressFragment != null)
                mWorkProgressFragment.dismiss();
            if (mTXVideoEditer != null)
                mTXVideoEditer.cancel();
        }
    }

    private Edit.OnCutChangeListener mCutChangeListener = new Edit.OnCutChangeListener() {

        @Override
        public void onCutClick() {

        }

        @Override
        public void onCutChangeKeyDown() {
            TXCLog.i(TAG, "mCutChangeListener, onCutChangeKeyDown, stopPlay()");
            mTXVideoEditer.stopPlay();
        }

        @Override
        public void onCutChangeKeyUp(long startTime, long endTime, int type) {
            TXCLog.i(TAG, "mCutChangeListener, onCutChangeKeyUp, startPlayFromTime");
            mCutterStartTime = startTime;
            mCutterEndTime = endTime;
            mTXVideoEditer.startPlayFromTime(startTime, endTime);
            mTvChoose.setText("已选取" + String.valueOf((endTime - startTime) / 1000) + "s");
            mCurrentState = PlayState.STATE_PLAY;
            TCVideoEditerWrapper.getInstance().setCutterStartTime(mCutterStartTime, mCutterEndTime);
        }
    };

    @Override
    public void onPreviewProgressWrapper(int time) {
    }

    @Override
    public void onPreviewFinishedWrapper() {
        TXCLog.i(TAG, "onPreviewFinishedWrapper startPlayFromTime mCutterStartTime:" + mCutterStartTime + ",mCutterEndTime:" + mCutterEndTime);
        mTXVideoEditer.startPlayFromTime(mCutterStartTime, mCutterEndTime);
    }
    /**
     * ===========================================加载视频相关 ===========================================
     */

    /**
     * 加在视频信息的runnable
     */
    private static class LoadVideoRunnable implements Runnable {
        private WeakReference<TCVideoCutterActivity> mWekActivity;

        LoadVideoRunnable(TCVideoCutterActivity activity) {
            mWekActivity = new WeakReference<TCVideoCutterActivity>(activity);
        }

        @Override
        public void run() {
            if (mWekActivity == null || mWekActivity.get() == null) {
                return;
            }
            TCVideoCutterActivity activity = mWekActivity.get();
            if (activity == null) return;
            TXVideoEditConstants.TXVideoInfo info = TXVideoInfoReader.getInstance().getVideoFileInfo(activity.mInVideoPath);

            if (info == null) {// error 发生错误
                activity.mVideoMainHandler.sendEmptyMessage(VideoMainHandler.LOAD_VIDEO_ERROR);
            } else {
                TCVideoEditerWrapper.getInstance().setTXVideoInfo(info);
                Message msg = Message.obtain();
                msg.what = VideoMainHandler.LOAD_VIDEO_SUCCESS;
                msg.obj = info;
                activity.mVideoMainHandler.sendMessage(msg);
            }
        }
    }

    /**
     * 主线程的Handler 用于处理load 视频信息的完后的动作
     */
    private static class VideoMainHandler extends Handler {
        static final int LOAD_VIDEO_SUCCESS = 0;
        static final int LOAD_VIDEO_ERROR = -1;
        private WeakReference<TCVideoCutterActivity> mWefActivity;


        VideoMainHandler(TCVideoCutterActivity activity) {
            mWefActivity = new WeakReference<TCVideoCutterActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            TCVideoCutterActivity activity = mWefActivity.get();
            if (activity == null) return;
            switch (msg.what) {
                case LOAD_VIDEO_ERROR:
                    DialogUtil.showDialog(activity, "编辑失败", "暂不支持Android 4.3以下的系统", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                        }
                    });
                    break;
                case LOAD_VIDEO_SUCCESS:
                    activity.loadVideoSuccess((TXVideoEditConstants.TXVideoInfo) msg.obj);
                    break;
            }
        }
    }

    private TXVideoInfoReader.OnSampleProgrocess mOnSampleProcessListener = new TXVideoInfoReader.OnSampleProgrocess() {
        @Override
        public void sampleProcess(int number, Bitmap bitmap) {
            mTCVideoEditView.addBitmap(number, bitmap);
        }
    };

    /*********************************************监听电话状态**************************************************/
    private void initPhoneListener() {
        //设置电话监听
        if (mPhoneListener == null) {
            mPhoneListener = new TXPhoneStateListener(this);
            TelephonyManager tm = (TelephonyManager) this.getApplicationContext().getSystemService(Service.TELEPHONY_SERVICE);
            tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private TXPhoneStateListener mPhoneListener;

    static class TXPhoneStateListener extends PhoneStateListener {
        WeakReference<TCVideoCutterActivity> mWefActivity;

        public TXPhoneStateListener(TCVideoCutterActivity activity) {
            mWefActivity = new WeakReference<TCVideoCutterActivity>(activity);
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            TCVideoCutterActivity activity = mWefActivity.get();
            if (activity == null) return;
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:  //电话等待接听
                case TelephonyManager.CALL_STATE_OFFHOOK:  //电话接听
                    // 直接停止播放
                    activity.cancelProcessVideo();
                    break;
                //电话挂机
                case TelephonyManager.CALL_STATE_IDLE:
                    break;
            }
        }
    }


}
