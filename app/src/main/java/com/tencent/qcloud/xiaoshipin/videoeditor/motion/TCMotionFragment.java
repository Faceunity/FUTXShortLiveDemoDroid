package com.tencent.qcloud.xiaoshipin.videoeditor.motion;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.videoeditor.BaseEditFragment;
import com.tencent.qcloud.xiaoshipin.videoeditor.TCVideoEditerWrapper;
import com.tencent.qcloud.xiaoshipin.videoeditor.TCVideoEffectActivity;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.videotimeline.ColorfulProgress;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.videotimeline.VideoProgressController;
import com.tencent.ugc.TXVideoEditConstants;
import com.tencent.ugc.TXVideoEditer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by hans on 2017/11/7.
 * <p>
 * 动态滤镜特效的设置Fragment
 */
public class TCMotionFragment extends BaseEditFragment implements View.OnClickListener, View.OnTouchListener {
    private static final String TAG = "TCMotionFragment";

    private TextView mRlPlayer;
    private boolean mIsOnTouch; // 是否已经有按下的
    private TXVideoEditer mTXVideoEditer;

    private long mVideoDuration;
    private ColorfulProgress mColorfulProgress;
    private VideoProgressController mActivityVideoProgressController;
    private ImageView mIvDelete;
    private boolean mStartMark;


    private Map<Integer, TCMotionItem> mMotionMap;

    private static class TCMotionItem {
        public int btnID;
        public int btnSelectID;
        public int animID;
        public int effectID;

        TCMotionItem(int btnID, int btnSelectID, int animID, int effectID) {
            this.btnID = btnID;
            this.btnSelectID = btnSelectID;
            this.animID = animID;
            this.effectID = effectID;
        }
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_motion, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TCVideoEditerWrapper wrapper = TCVideoEditerWrapper.getInstance();
        mTXVideoEditer = wrapper.getEditer();
        if (mTXVideoEditer != null) {
            mVideoDuration = wrapper.getTXVideoInfo().duration;
        }
        mActivityVideoProgressController = ((TCVideoEffectActivity) getActivity()).getVideoProgressViewController();
        initViews(view);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (mColorfulProgress != null) {
            mColorfulProgress.setVisibility(hidden ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        List<ColorfulProgress.MarkInfo> markInfoList = mColorfulProgress.getMarkInfoList();
        TCMotionViewInfoManager.getInstance().setMarkInfoList(markInfoList);

        for (Map.Entry<Integer, TCMotionItem> entry : mMotionMap.entrySet()) {
            TCMotionItem item = entry.getValue();
            ImageButton btn = (ImageButton) getActivity().findViewById(item.btnID);
            AnimationDrawable drawable = (AnimationDrawable) btn.getDrawable();
            drawable.stop();
        }
    }

    private void initViews(View view) {
        mMotionMap = new HashMap<>();
        mMotionMap.put(R.id.btn_soul, new TCMotionItem(R.id.btn_soul, R.id.btn_soul_select, R.drawable.anim_effect1, TXVideoEditConstants.TXEffectType_SOUL_OUT));
        mMotionMap.put(R.id.btn_split, new TCMotionItem(R.id.btn_split, R.id.btn_split_select, R.drawable.anim_effect2, TXVideoEditConstants.TXEffectType_SPLIT_SCREEN));
        mMotionMap.put(R.id.btn_light_wave, new TCMotionItem(R.id.btn_light_wave, R.id.btn_light_wave_select, R.drawable.anim_effect3, TXVideoEditConstants.TXEffectType_ROCK_LIGHT));
        mMotionMap.put(R.id.btn_black, new TCMotionItem(R.id.btn_black, R.id.btn_black_select, R.drawable.anim_effect4, TXVideoEditConstants.TXEffectType_DARK_DRAEM));
        mMotionMap.put(R.id.btn_win_shaddow, new TCMotionItem(R.id.btn_win_shaddow, R.id.btn_win_shaddow_select, R.drawable.anim_effect3, TXVideoEditConstants.TXEffectType_WIN_SHADDOW));
        mMotionMap.put(R.id.btn_ghost_shaddow, new TCMotionItem(R.id.btn_ghost_shaddow, R.id.btn_ghost_shaddow_select, R.drawable.anim_effect3, TXVideoEditConstants.TXEffectType_GHOST_SHADDOW));
        mMotionMap.put(R.id.btn_phantom, new TCMotionItem(R.id.btn_phantom, R.id.btn_phantom_select, R.drawable.anim_effect3, TXVideoEditConstants.TXEffectType_PHANTOM_SHADDOW));
        mMotionMap.put(R.id.btn_ghost, new TCMotionItem(R.id.btn_ghost, R.id.btn_ghost_select, R.drawable.anim_effect3, TXVideoEditConstants.TXEffectType_GHOST));
        mMotionMap.put(R.id.btn_lightning, new TCMotionItem(R.id.btn_lightning, R.id.btn_lightning_select, R.drawable.anim_effect3, TXVideoEditConstants.TXEffectType_LIGHTNING));
        mMotionMap.put(R.id.btn_mirror, new TCMotionItem(R.id.btn_mirror, R.id.btn_mirror_select, R.drawable.anim_effect3, TXVideoEditConstants.TXEffectType_MIRROR));
        mMotionMap.put(R.id.btn_illusion, new TCMotionItem(R.id.btn_illusion, R.id.btn_illusion_select, R.drawable.anim_effect3, TXVideoEditConstants.TXEffectType_ILLUSION));

        for (Map.Entry<Integer, TCMotionItem> entry : mMotionMap.entrySet()) {
            TCMotionItem item = entry.getValue();
            ImageButton btn = (ImageButton) view.findViewById(item.btnID);
            btn.setOnTouchListener(this);
            btn.setImageResource(item.animID);
            AnimationDrawable drawable = (AnimationDrawable) btn.getDrawable();
            drawable.start();
        }

        mIvDelete = (ImageView) view.findViewById(R.id.iv_undo);
        mIvDelete.setOnClickListener(this);

        mRlPlayer = (TextView) view.findViewById(R.id.motion_rl_play);
        mRlPlayer.setOnClickListener(this);

        mColorfulProgress = new ColorfulProgress(getContext());
        mColorfulProgress.setWidthHeight(mActivityVideoProgressController.getThumbnailPicListDisplayWidth(), getResources().getDimensionPixelOffset(R.dimen.video_progress_height));
        mColorfulProgress.setMarkInfoList(TCMotionViewInfoManager.getInstance().getMarkInfoList());
        mActivityVideoProgressController.addColorfulProgress(mColorfulProgress);

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_undo:
                deleteLastMotion();
                break;
            case R.id.motion_rl_play:

                ((TCVideoEffectActivity) getActivity()).switchPlayVideo();
                break;
        }
    }

    private void deleteLastMotion() {
        ColorfulProgress.MarkInfo markInfo = mColorfulProgress.deleteLastMark();
        if (markInfo != null) {
            mActivityVideoProgressController.setCurrentTimeMs(markInfo.startTimeMs);
            TCVideoEffectActivity parentActivity = (TCVideoEffectActivity) getActivity();
            parentActivity.previewAtTime(markInfo.startTimeMs);
        }

        mTXVideoEditer.deleteLastEffect();
        if (mColorfulProgress.getMarkListSize() > 0) {
            showDeleteBtn();
        } else {
            hideDeleteBtn();
        }
    }

    public void showDeleteBtn() {
        if (mColorfulProgress.getMarkListSize() > 0) {
            mIvDelete.setVisibility(View.VISIBLE);
        }
    }

    public void hideDeleteBtn() {
        if (mColorfulProgress.getMarkListSize() == 0) {
            mIvDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (mIsOnTouch && action == MotionEvent.ACTION_DOWN) {
            return false;
        }

        TCMotionItem item = mMotionMap.get(view.getId());
        if (item != null) {
            CircleImageView btnSelect = (CircleImageView) getActivity().findViewById(item.btnSelectID);
            if (action == MotionEvent.ACTION_DOWN) {
                btnSelect.setVisibility(View.VISIBLE);
                pressMotion(item.effectID);
                mIsOnTouch = true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                btnSelect.setVisibility(View.INVISIBLE);
                upMotion(item.effectID);
                mIsOnTouch = false;
            }
            return false;
        }

        return false;
    }

    private void pressMotion(int type) {
        // 未开始播放 则开始播放
        long currentTime = mActivityVideoProgressController.getCurrentTimeMs();

        if (((TCVideoEffectActivity) getActivity()).isPreviewFinish) {
            TXCLog.i(TAG, "pressMotion, preview finished, ignore");
            mStartMark = false;
            return;
        }
        mStartMark = true;
        ((TCVideoEffectActivity) getActivity()).startPlayAccordingState(currentTime, TCVideoEditerWrapper.getInstance().getCutterEndTime());
        mTXVideoEditer.startEffect(type, currentTime);

        switch (type) {
            case TXVideoEditConstants.TXEffectType_SOUL_OUT:
                // 进度条开始变颜色
                mColorfulProgress.startMark(getResources().getColor(R.color.spirit_out_color_press));
                break;
            case TXVideoEditConstants.TXEffectType_SPLIT_SCREEN:
                mColorfulProgress.startMark(getResources().getColor(R.color.screen_split_press));
                break;
            case TXVideoEditConstants.TXEffectType_ROCK_LIGHT:
                mColorfulProgress.startMark(getResources().getColor(R.color.light_wave_press));
                break;
            case TXVideoEditConstants.TXEffectType_DARK_DRAEM:
                mColorfulProgress.startMark(getResources().getColor(R.color.dark_illusion_press));
                break;
        }
    }

    private void upMotion(int type) {
        if (!mStartMark) {
            return;
        }
        // 暂停播放
        ((TCVideoEffectActivity) getActivity()).pausePlay();
        // 进度条结束标记
        mColorfulProgress.endMark();

        // 特效结束时间
        long currentTime = mActivityVideoProgressController.getCurrentTimeMs();
        mTXVideoEditer.stopEffect(type, currentTime);
        // 显示撤销的按钮
        showDeleteBtn();
    }
}
