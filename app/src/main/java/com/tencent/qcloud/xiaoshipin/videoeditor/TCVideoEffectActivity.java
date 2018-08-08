package com.tencent.qcloud.xiaoshipin.videoeditor;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.utils.TCConstants;
import com.tencent.qcloud.xiaoshipin.common.utils.TCUtils;
import com.tencent.qcloud.xiaoshipin.videoeditor.bgm.TCBGMSettingFragment;
import com.tencent.qcloud.xiaoshipin.videoeditor.bubble.TCBubbleFragment;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.videotimeline.VideoProgressController;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.videotimeline.VideoProgressView;
import com.tencent.qcloud.xiaoshipin.videoeditor.filter.TCStaticFilterFragment;
import com.tencent.qcloud.xiaoshipin.videoeditor.motion.TCMotionFragment;
import com.tencent.qcloud.xiaoshipin.videoeditor.paster.TCPasterFragment;
import com.tencent.qcloud.xiaoshipin.videoeditor.time.TCTimeFragment;
import com.tencent.qcloud.xiaoshipin.videoeditor.utils.DraftEditer;
import com.tencent.qcloud.xiaoshipin.videoeditor.utils.EffectEditer;
import com.tencent.qcloud.xiaoshipin.videoeditor.utils.PlayState;
import com.tencent.ugc.TXVideoEditConstants;
import com.tencent.ugc.TXVideoEditer;

import java.lang.ref.WeakReference;
import java.util.List;


/**
 * Created by hans on 2017/11/6.
 */

public class TCVideoEffectActivity extends FragmentActivity implements
        View.OnClickListener,
        TCVideoEditerWrapper.TXVideoPreviewListenerWrapper {
    private static final String TAG = "TCVideoEffectActivity";

    private TXVideoEditer mTXVideoEditer;                   // SDK接口类
    /**
     * 布局相关
     */
    private ImageView mLlBack;                              // 左上角返回
    private FrameLayout mVideoPlayerLayout;                 // 视频承载布局
    private ImageView mIvPlay;                              // 播放按钮
    private Button mTvDone;
    private TextView mTvCurrent;

    private BaseEditFragment mCurrentFragment,              // 标记当前的Fragment
            mTimeFragment,                                  // 时间特效的Fragment
            mStaticFilterFragment,                          // 静态滤镜的Fragment
            mMotionFragment,                                // 动态滤镜的Fragment
            mBGMSettingFragment,                            // BGM设置的Fragment
            mPasterFragment,                                // 贴纸的Fragment
            mBubbleFragment;                                // 气泡字幕的Fragment
    private int mCurrentState = PlayState.STATE_NONE;       // 播放器当前状态

    private long mVideoDuration;                            // 视频的总时长
    private long mPreviewAtTime;                            // 当前单帧预览的时间

    private TXPhoneStateListener mPhoneListener;            // 电话监听

    private KeyguardManager mKeyguardManager;
    private int mFragmentType;

    public boolean isPreviewFinish;

    private DraftEditer mDraftEditer;
    private EffectEditer mEffectEditer;

    /**
     * 缩略图进度条相关
     */
    private VideoProgressView mVideoProgressView;
    public VideoProgressController mVideoProgressController;
    private VideoProgressController.VideoProgressSeekListener mVideoProgressSeekListener = new VideoProgressController.VideoProgressSeekListener() {
        @Override
        public void onVideoProgressSeek(long currentTimeMs) {
            TXCLog.i(TAG, "onVideoProgressSeek, currentTimeMs = " + currentTimeMs);

            previewAtTime(currentTimeMs);
        }

        @Override
        public void onVideoProgressSeekFinish(long currentTimeMs) {
            TXCLog.i(TAG, "onVideoProgressSeekFinish, currentTimeMs = " + currentTimeMs);

            previewAtTime(currentTimeMs);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_effect);
        TCVideoEditerWrapper wrapper = TCVideoEditerWrapper.getInstance();
        wrapper.addTXVideoPreviewListenerWrapper(this);

        mTXVideoEditer = wrapper.getEditer();
        if (mTXVideoEditer == null || wrapper.getTXVideoInfo() == null) {
            Toast.makeText(this, "状态异常，结束编辑", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        long cutterStartTime = wrapper.getCutterStartTime();
        long cutterEndTime = wrapper.getCutterEndTime();
        if (cutterEndTime - cutterStartTime != 0) {
            mVideoDuration = cutterEndTime - cutterStartTime;
        } else {
            mVideoDuration = wrapper.getTXVideoInfo().duration;
        }
        TCVideoEditerWrapper.getInstance().setCutterStartTime(0, mVideoDuration);

        mFragmentType = getIntent().getIntExtra(TCConstants.KEY_FRAGMENT, 0);

        loadConfigToDraft();

        initViews();
        initPhoneListener();
        initVideoProgressLayout();
        previewVideo();// 开始预览视频
        mKeyguardManager = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
    }

    //将配置加载到草稿箱
    private void loadConfigToDraft() {
        mDraftEditer = DraftEditer.getInstance();
        mEffectEditer = EffectEditer.getInstance();

        mDraftEditer.setBgmPath(mEffectEditer.getBgmPath());
        mDraftEditer.setBgmPos(mEffectEditer.getBgmPos());
        mDraftEditer.setBgmVolume(mEffectEditer.getBgmVolume());
        mDraftEditer.setVideoVolume(mEffectEditer.getVideoVolume());
        mDraftEditer.setBgmStartTime(mEffectEditer.getBgmStartTime());
        mDraftEditer.setBgmEndTime(mEffectEditer.getBgmEndTime());
        mDraftEditer.setBgmDuration(mEffectEditer.getBgmDuration());
    }

    private void initPhoneListener() {
        //设置电话监听
        if (mPhoneListener == null) {
            mPhoneListener = new TXPhoneStateListener(this);
            TelephonyManager tm = (TelephonyManager) this.getApplicationContext().getSystemService(Service.TELEPHONY_SERVICE);
            tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }


    private void initViews() {
        mLlBack = (ImageView) findViewById(R.id.editer_back_ll);
        mLlBack.setOnClickListener(this);
        mTvDone = (Button) findViewById(R.id.editer_tv_done);
        mTvDone.setOnClickListener(this);
        mVideoPlayerLayout = (FrameLayout) findViewById(R.id.editer_fl_video);

        mIvPlay = (ImageView) findViewById(R.id.iv_play);
        mIvPlay.setOnClickListener(this);

        mTvCurrent = (TextView) findViewById(R.id.tv_current);
    }

    /**
     * ==========================================SDK播放器生命周期==========================================
     */
    private void previewVideo() {
        showFragmentByType(mFragmentType);
        initVideoProgressLayout();  // 初始化进度布局
        initPlayerLayout();         // 初始化预览视频布局
        startPlay(0, mVideoDuration);  // 开始播放
    }

    private void showFragmentByType(int type) {
        switch (type) {
            case TCConstants.TYPE_EDITER_BGM:
                showBGMFragment();
                break;
            case TCConstants.TYPE_EDITER_MOTION:
                showMotionFragment();
                break;
            case TCConstants.TYPE_EDITER_SPEED:
                showTimeFragment();
                break;
            case TCConstants.TYPE_EDITER_FILTER:
                showFilterFragment();
                break;
            case TCConstants.TYPE_EDITER_PASTER:
                showPasterFragment();
                break;
            case TCConstants.TYPE_EDITER_SUBTITLE:
                showBubbleFragment();
                break;
        }
    }

    private void initVideoProgressLayout() {
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        int screenWidth = point.x;
        mVideoProgressView = (VideoProgressView) findViewById(R.id.editer_video_progress_view);
        mVideoProgressView.setViewWidth(screenWidth);

        List<Bitmap> thumbnailList = TCVideoEditerWrapper.getInstance().getAllThumbnails();
        mVideoProgressView.setThumbnailData(thumbnailList);

        mVideoProgressController = new VideoProgressController(mVideoDuration);
        mVideoProgressController.setVideoProgressView(mVideoProgressView);
        mVideoProgressController.setVideoProgressSeekListener(mVideoProgressSeekListener);
        mVideoProgressController.setVideoProgressDisplayWidth(screenWidth);
    }

    private void initPlayerLayout() {
        TXVideoEditConstants.TXPreviewParam param = new TXVideoEditConstants.TXPreviewParam();
        param.videoView = mVideoPlayerLayout;
        param.renderMode = TXVideoEditConstants.PREVIEW_RENDER_MODE_FILL_EDGE;
        mTXVideoEditer.initWithPreview(param);
    }

    /**
     * 调用mTXVideoEditer.previewAtTime后，需要记录当前时间，下次播放时从当前时间开始
     *
     * @param timeMs
     */
    public void previewAtTime(long timeMs) {
        pausePlay();
        isPreviewFinish = false;
        mTXVideoEditer.previewAtTime(timeMs);
        mPreviewAtTime = timeMs;
        mCurrentState = PlayState.STATE_PREVIEW_AT_TIME;
    }

    /**
     * 给子Fragment调用 （子Fragment不在意Activity中对于播放器的生命周期）
     */
    public void startPlayAccordingState(long startTime, long endTime) {
        if (mCurrentState == PlayState.STATE_STOP || mCurrentState == PlayState.STATE_NONE || mCurrentState == PlayState.STATE_PREVIEW_AT_TIME) {
            startPlay(startTime, endTime);
        } else if (mCurrentState == PlayState.STATE_PAUSE) {
            resumePlay();
        }
    }

    /**
     * 给子Fragment调用 （子Fragment不在意Activity中对于播放器的生命周期）
     */
    public void restartPlay() {
        stopPlay();
        startPlay(0, mVideoDuration);
    }

    public void startPlay(long startTime, long endTime) {
        mTXVideoEditer.startPlayFromTime(startTime, endTime);
        mCurrentState = PlayState.STATE_PLAY;
        isPreviewFinish = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mIvPlay.setImageResource(R.drawable.ic_pause_normal);
            }
        });
        mCurrentFragment.notifyStartPlay();
    }


    public void resumePlay() {
        if (mCurrentState == PlayState.STATE_PAUSE) {
            mTXVideoEditer.resumePlay();
            mCurrentState = PlayState.STATE_RESUME;
            mIvPlay.setImageResource(R.drawable.ic_pause_normal);

            mCurrentFragment.notifyResumePlay();
        }
    }

    public void pausePlay() {
        if (mCurrentState == PlayState.STATE_RESUME || mCurrentState == PlayState.STATE_PLAY) {
            mTXVideoEditer.pausePlay();
            mCurrentState = PlayState.STATE_PAUSE;
            mIvPlay.setImageResource(R.drawable.ic_play_normal);

            mCurrentFragment.notifyPausePlay();
        }
    }

    public void stopPlay() {
        if (mCurrentState == PlayState.STATE_RESUME || mCurrentState == PlayState.STATE_PLAY ||
                mCurrentState == PlayState.STATE_PREVIEW_AT_TIME || mCurrentState == PlayState.STATE_PAUSE) {
            mTXVideoEditer.stopPlay();
            mCurrentState = PlayState.STATE_STOP;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mIvPlay.setImageResource(R.drawable.ic_play_normal);
                }
            });
        }
    }

    /**
     * ==========================================activity生命周期==========================================
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (!mKeyguardManager.inKeyguardRestrictedInputMode()) {
            restartPlay();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pausePlay();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mBGMSettingFragment != null && mBGMSettingFragment.isAdded()) {
            mBGMSettingFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPhoneListener != null) {
            TelephonyManager tm = (TelephonyManager) this.getApplicationContext().getSystemService(Service.TELEPHONY_SERVICE);
            tm.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
        }
        if (mTXVideoEditer != null) {
            mTXVideoEditer.setVideoGenerateListener(null);
        }
        TCVideoEditerWrapper.getInstance().removeTXVideoPreviewListenerWrapper(this);
    }

    /**
     * ==========================================SDK回调==========================================
     */
    @Override // 预览进度回调
    public void onPreviewProgressWrapper(final int timeMs) {
        // 视频的进度回调是异步的，如果不是处于播放状态，那么无需修改进度
        if (mCurrentState == PlayState.STATE_RESUME || mCurrentState == PlayState.STATE_PLAY) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mVideoProgressController.setCurrentTimeMs(timeMs);
                    mTvCurrent.setText(TCUtils.duration(timeMs));
                }
            });
        }
    }

    @Override // 预览完成回调
    public void onPreviewFinishedWrapper() {
        TXCLog.d(TAG, "---------------onPreviewFinished-----------------");
        isPreviewFinish = true;
        stopPlay();
        if ((mMotionFragment != null && mMotionFragment.isAdded() && !mMotionFragment.isHidden()) ||
                (mTimeFragment != null && mTimeFragment.isAdded() && !mTimeFragment.isHidden())) {
            // 处于动态滤镜或者时间特效界面,忽略 不做任何操作
        } else {
            // 如果当前不是动态滤镜界面或者时间特效界面，那么会自动开始重复播放
            startPlay(0, mVideoDuration);
        }
    }

    /**
     * ==========================================工具栏的点击回调==========================================
     */
    private void showTimeFragment() {
        if (mTimeFragment == null) {
            mTimeFragment = new TCTimeFragment();
        }
        showFragment(mTimeFragment, "time_fragment");
    }

    private void showFilterFragment() {
        if (mStaticFilterFragment == null) {
            mStaticFilterFragment = new TCStaticFilterFragment();
        }
        showFragment(mStaticFilterFragment, "static_filter_fragment");
    }

    private void showMotionFragment() {
        if (mMotionFragment == null) {
            mMotionFragment = new TCMotionFragment();
        }
        showFragment(mMotionFragment, "motion_fragment");
    }

    private void showPasterFragment() {
        if (mPasterFragment == null) {
            mPasterFragment = new TCPasterFragment();
        }
        showFragment(mPasterFragment, "paster_fragment");
    }

    private void showBubbleFragment() {
        if (mBubbleFragment == null) {
            mBubbleFragment = new TCBubbleFragment();
        }
        showFragment(mBubbleFragment, "bubble_fragment");
    }

    private void showBGMFragment() {
        if (mBGMSettingFragment == null) {
            mBGMSettingFragment = new TCBGMSettingFragment();
        }
        showFragment(mBGMSettingFragment, "bgm_setting_fragment");
    }

    private void showFragment(BaseEditFragment fragment, String tag) {
        if (fragment == mCurrentFragment) return;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (mCurrentFragment != null) {
            transaction.hide(mCurrentFragment);
        }
        if (!fragment.isAdded()) {
            transaction.add(R.id.editer_fl_container, fragment, tag);
        } else {
            transaction.show(fragment);
        }
        mCurrentFragment = fragment;
        if (mCurrentFragment == mBGMSettingFragment) {
            mIvPlay.setVisibility(View.GONE);
            mVideoProgressView.setVisibility(View.GONE);
        } else {
            mIvPlay.setVisibility(View.VISIBLE);
            mVideoProgressView.setVisibility(View.VISIBLE);
        }
        transaction.commit();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.editer_back_ll:// 返回
                clickBack();
                break;
            case R.id.editer_tv_done:// 保存修改的配置
                saveConfigFromDraft();
                // 退出界面必须要调stopPlay()
                stopPlay();
                finish();
                break;
            case R.id.iv_play:// 播放
                TXCLog.i(TAG, "editer_ib_play clicked, mCurrentState = " + mCurrentState);
                switchPlayVideo();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        clickBack();
    }

    private void clickBack(){
        // 退出界面必须要调stopPlay()
        stopPlay();
        DraftEditer.getInstance().clear();
        finish();
    }

    //将草稿箱变更的配置保存
    private void saveConfigFromDraft() {
        mEffectEditer.setBgmPath(mDraftEditer.getBgmPath());
        mEffectEditer.setBgmPos(mDraftEditer.getBgmPos());
        mEffectEditer.setBgmVolume(mDraftEditer.getBgmVolume());
        mEffectEditer.setVideoVolume(mDraftEditer.getVideoVolume());
        mEffectEditer.setBgmStartTime(mDraftEditer.getBgmStartTime());
        mEffectEditer.setBgmEndTime(mDraftEditer.getBgmEndTime());
        mEffectEditer.setBgmDuration(mDraftEditer.getBgmDuration());
    }

    public void switchPlayVideo() {
        if (mCurrentState == PlayState.STATE_NONE || mCurrentState == PlayState.STATE_STOP) {
            TXVideoEditConstants.TXVideoInfo info = TCVideoEditerWrapper.getInstance().getTXVideoInfo();
            startPlay(0, info.duration);
        } else if (mCurrentState == PlayState.STATE_RESUME || mCurrentState == PlayState.STATE_PLAY) {
            pausePlay();
        } else if (mCurrentState == PlayState.STATE_PAUSE) {
            resumePlay();
        } else if (mCurrentState == PlayState.STATE_PREVIEW_AT_TIME) {
            startPlay(mPreviewAtTime, mVideoDuration);
        }
    }

    /**
     * ==========================================进度条==========================================
     */
    public VideoProgressController getVideoProgressViewController() {
        return mVideoProgressController;
    }

    /*********************************************监听电话状态**************************************************/
    static class TXPhoneStateListener extends PhoneStateListener {
        WeakReference<TCVideoEffectActivity> mEditer;

        public TXPhoneStateListener(TCVideoEffectActivity editer) {
            mEditer = new WeakReference<TCVideoEffectActivity>(editer);
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            TCVideoEffectActivity activity = mEditer.get();
            if (activity == null) return;
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:  //电话等待接听
                case TelephonyManager.CALL_STATE_OFFHOOK:  //电话接听
                    // 直接停止播放
                    activity.stopPlay();
                    break;
                //电话挂机
                case TelephonyManager.CALL_STATE_IDLE:
                    // 重新开始播放
                    activity.restartPlay();
                    break;
            }
        }
    }
}
