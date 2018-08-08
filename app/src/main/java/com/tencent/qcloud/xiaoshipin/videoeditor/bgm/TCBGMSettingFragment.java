package com.tencent.qcloud.xiaoshipin.videoeditor.bgm;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.utils.TCConstants;
import com.tencent.qcloud.xiaoshipin.videoeditor.BaseEditFragment;
import com.tencent.qcloud.xiaoshipin.videoeditor.utils.DraftEditer;
import com.tencent.qcloud.xiaoshipin.videoeditor.TCVideoEditerWrapper;
import com.tencent.qcloud.xiaoshipin.videoeditor.bgm.view.TCBGMPannel;
import com.tencent.qcloud.xiaoshipin.videoeditor.utils.DialogUtil;
import com.tencent.ugc.TXVideoEditer;

import java.io.IOException;

/**
 * RangeSlider
 * Created by hans on 2017/11/6.
 * <p>
 * bgm设置的fragment
 */
public class TCBGMSettingFragment extends BaseEditFragment {
    private static final String TAG = "TCBGMSettingFragment";
    private View mContentView;

    /**
     * 控制面板相关
     */
    private int mBgmPosition = -1;
    private TCBGMPannel mTCBGMPannel;
    private String mBGMPath;
    private int mBgmDuration;
    private DraftEditer mDraftEditer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDraftEditer = DraftEditer.getInstance();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String path = mDraftEditer.getBgmPath();
        if (!TextUtils.isEmpty(path)) {
            mBGMPath = path;
        } else {
            chooseBGM();
        }
        mBgmPosition = mDraftEditer.getBgmPos();

        float vidoVolume = mDraftEditer.getVideoVolume();
        if (vidoVolume != -1) {
            mTCBGMPannel.setVideoVolume(vidoVolume);
        }
        float bgmVolume = mDraftEditer.getBgmVolume();
        if (bgmVolume != -1) {
            mTCBGMPannel.setBgmVolume(bgmVolume);
        }

        long bgmDuration = mDraftEditer.getBgmDuration();
        if (bgmDuration != 0) {
            mTCBGMPannel.setBgmDuration(bgmDuration);
        }
        long startTime = mDraftEditer.getBgmStartTime();
        long endTime = mDraftEditer.getBgmEndTime();
        if (startTime != -1 && endTime != -1) {
            mTCBGMPannel.setCutRange(startTime, endTime);
        }
    }

    private void chooseBGM() {
        Intent bgmIntent = new Intent(getActivity(), BGMSelectActivity.class);
        bgmIntent.putExtra(TCConstants.BGM_POSITION, mBgmPosition);
        startActivityForResult(bgmIntent, TCConstants.ACTIVITY_BGM_REQUEST_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            return;
        }
        mBGMPath = data.getStringExtra(TCConstants.BGM_PATH);
        mBgmPosition = data.getIntExtra(TCConstants.BGM_POSITION, -1);
        TCVideoEditerWrapper.getInstance().saveBGM(mBGMPath);
        if (TextUtils.isEmpty(mBGMPath)) {
            return;
        }
        TXVideoEditer editer = TCVideoEditerWrapper.getInstance().getEditer();
        int result = editer.setBGM(mBGMPath);
        if (result != 0) {
            DialogUtil.showDialog(getContext(), "视频编辑失败", "背景音仅支持MP3格式或M4A音频", new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(mBGMPath);
            mediaPlayer.prepare();
            mBgmDuration = mediaPlayer.getDuration();
            TXCLog.i(TAG, "onActivityResult, BgmDuration = " + mBgmDuration);
            mediaPlayer.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
        editer.setBGMStartTime(0, mBgmDuration);
        editer.setBGMVolume(0.5f);
        editer.setVideoVolume(0.5f);
        if (mTCBGMPannel != null) {
            mTCBGMPannel.setVideoVolume(0.5f);
            mTCBGMPannel.setBgmVolume(0.5f);
            mTCBGMPannel.setBgmDuration(mBgmDuration);
        }
        //保存配置
        DraftEditer draftEditer = DraftEditer.getInstance();
        draftEditer.setBgmPath(mBGMPath);
        draftEditer.setBgmPos(mBgmPosition);
        draftEditer.setBgmVolume(0.5f);
        draftEditer.setVideoVolume(0.5f);
        draftEditer.setBgmDuration(mBgmDuration);

        mTCBGMPannel.setBgmDuration(mBgmDuration);
        mTCBGMPannel.setVideoVolume(0.5f);
        mTCBGMPannel.setBgmVolume(0.5f);
        mTCBGMPannel.setCutRange(0, mBgmDuration);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bgm, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mContentView = view;
        initMusicPanel(view);
    }

    /**
     * bgm 播放时间区间设置
     */
    private void onSetBGMStartTime(long startTime, long endTime) {
        TXVideoEditer editer = TCVideoEditerWrapper.getInstance().getEditer();
        editer.setBGMStartTime(startTime, endTime);
    }

    /**
     * ==============================================音乐列表相关==============================================
     */
    private void initMusicPanel(View view) {
        mTCBGMPannel = (TCBGMPannel) view.findViewById(R.id.tc_record_bgm_pannel);
        mTCBGMPannel.hideOkButton();
        mTCBGMPannel.setOnBGMChangeListener(new TCBGMPannel.BGMChangeListener() {
            @Override
            public void onMicVolumeChanged(float volume) {
                mDraftEditer.setVideoVolume(volume);

                TXVideoEditer editer = TCVideoEditerWrapper.getInstance().getEditer();
                editer.setVideoVolume(volume);
            }

            @Override
            public void onBGMVolumChanged(float volume) {
                mDraftEditer.setBgmVolume(volume);

                TXVideoEditer editer = TCVideoEditerWrapper.getInstance().getEditer();
                editer.setBGMVolume(volume);
            }

            @Override
            public void onBGMTimeChanged(long startTime, long endTime) {
                mDraftEditer.setBgmStartTime(startTime);
                mDraftEditer.setBgmEndTime(endTime);

                onSetBGMStartTime(startTime, endTime);
                if (mTCBGMPannel != null) {
                    mTCBGMPannel.updateBGMStartTime(startTime);
                }
            }

            @Override
            public void onClickReplace() {
                chooseBGM();
            }

            @Override
            public void onClickDelete() {
                mDraftEditer.setBgmPath(null);

                TXVideoEditer editer = TCVideoEditerWrapper.getInstance().getEditer();
                editer.setBGM(null);

                TCVideoEditerWrapper.getInstance().saveBGM(null);
            }

            @Override
            public void onClickConfirm() {
            }

            @Override
            public void onClickVoiceChanger(int type) {
            }

            @Override
            public void onClickReverb(int type) {
            }
        });
    }

}
