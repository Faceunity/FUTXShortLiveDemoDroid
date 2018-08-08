package com.tencent.qcloud.xiaoshipin.videoeditor.bubble;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.videoeditor.BaseEditFragment;
import com.tencent.qcloud.xiaoshipin.videoeditor.TCVideoEditerWrapper;
import com.tencent.qcloud.xiaoshipin.videoeditor.TCVideoEffectActivity;
import com.tencent.qcloud.xiaoshipin.videoeditor.bubble.ui.bubble.TCBubbleViewParams;
import com.tencent.qcloud.xiaoshipin.videoeditor.bubble.ui.bubble.TCWordBubbleView;
import com.tencent.qcloud.xiaoshipin.videoeditor.bubble.ui.bubble.TCWordBubbleViewFactory;
import com.tencent.qcloud.xiaoshipin.videoeditor.bubble.ui.others.TCWordInputDialog;
import com.tencent.qcloud.xiaoshipin.videoeditor.bubble.ui.popwin.TCBubbleSettingView;
import com.tencent.qcloud.xiaoshipin.videoeditor.bubble.ui.popwin.TCWordParamsInfo;
import com.tencent.qcloud.xiaoshipin.videoeditor.bubble.utils.TCBubbleManager;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.BaseRecyclerAdapter;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.layer.TCLayerOperationView;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.layer.TCLayerViewGroup;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.videotimeline.RangeSliderViewContainer;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.videotimeline.ViewConst;
import com.tencent.ugc.TXVideoEditConstants;
import com.tencent.ugc.TXVideoEditer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vinsonswang on 2017/12/15.
 */

public class TCBubbleFragment extends BaseEditFragment implements BaseRecyclerAdapter.OnItemClickListener,
        TCLayerViewGroup.OnItemClickListener,
        TCBubbleSettingView.OnWordInfoCallback,
        TCWordBubbleView.IOperationViewClickListener,
        TCWordInputDialog.OnWordInputCallback,
        View.OnClickListener {
    private final String TAG = "TCBubbleFragment";

    private TXVideoEditer mTXVideoEditer;

    private RelativeLayout mRlDelete;
    private ImageView mIvBubbleDel;
    private RecyclerView mRvBubble;
    private View mFootView;
    private AddBubbleAdapter mAddBubbleAdapter;
    private List<TCBubbleViewParams> mAddBubbleInfoList;

    private TCLayerViewGroup mTCBubbleViewGroup; // 图层父布局，承载字幕
    private int mCurrentSelectedPos = -1;// 当前被选中的气泡字幕控件
    private TCWordInputDialog mWordInputDialog;
    private TCBubbleSettingView mBubblePopWin; // 气泡字幕的 背景、颜色的配置板
    private boolean mIsEditWordAgain = false;// 用于判定当前是否修改字幕内容
    private RangeSliderViewContainer.OnDurationChangeListener mOnDurationChangeListener;

    //================================== 默认的时间 ==============================
    private long mDuration;
    private long mDefaultWordStartTime;
    private long mDefaultWordEndTime;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bubble_word, container, false);

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView(view);

        initData();

        initRangeDurationChangeListener();

        recoverFromManager();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            mTCBubbleViewGroup.setVisibility(View.GONE);
            ((TCVideoEffectActivity) getActivity()).mVideoProgressController.showAllRangeSliderView(ViewConst.VIEW_TYPE_WORD, false);
        } else {
            ((TCVideoEffectActivity) getActivity()).mVideoProgressController.showAllRangeSliderView(ViewConst.VIEW_TYPE_WORD, true);
        }
    }

    private void initData() {
        mTXVideoEditer = TCVideoEditerWrapper.getInstance().getEditer();
        mDuration = TCVideoEditerWrapper.getInstance().getTXVideoInfo().duration;
        updateDefaultTime();
    }

    private void initView(View view) {
        mFootView = LayoutInflater.from(view.getContext()).inflate(R.layout.item_add, null);
        mAddBubbleInfoList = new ArrayList<>();
        mRvBubble = (RecyclerView) view.findViewById(R.id.bubble_rv_list);
        mRvBubble.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        mAddBubbleAdapter = new AddBubbleAdapter(mAddBubbleInfoList, getActivity());
        mAddBubbleAdapter.setOnItemClickListener(this);
        mRvBubble.setAdapter(mAddBubbleAdapter);
        mAddBubbleAdapter.setFooterView(mFootView);

        mTCBubbleViewGroup = (TCLayerViewGroup) getActivity().findViewById(R.id.bubble_container);
        mTCBubbleViewGroup.setOnItemClickListener(this);
        mTCBubbleViewGroup.enableChildSingleClick(false); // 在容器里不响应子控件的单击事件
        mTCBubbleViewGroup.enableDoubleChildClick(true); // 在容器里响应子控件的双击事件

        // 展示气泡样式修改的面板
        mBubblePopWin = (TCBubbleSettingView) getActivity().findViewById(R.id.word_bubble_setting_view);
        mBubblePopWin.setBubbles(TCBubbleManager.getInstance(getActivity()).loadBubbles());
        mBubblePopWin.setOnWordInfoCallback(this);

        mIvBubbleDel = (ImageView) view.findViewById(R.id.iv_bubble_del);
        mIvBubbleDel.setOnClickListener(this);
    }

    private void initRangeDurationChangeListener() {
        mOnDurationChangeListener = new RangeSliderViewContainer.OnDurationChangeListener() {
            @Override
            public void onDurationChange(long startTime, long endTime) {
                // 获取当选中的贴纸，并且将时间设置进去
                TCWordBubbleView view = (TCWordBubbleView) mTCBubbleViewGroup.getSelectedLayerOperationView();
                if (view != null) {
                    view.setStartTime(startTime, endTime);
                }
            }
        };
    }

    /**
     * 根据当前控件数量 更新默认的一个控件开始时间和结束时间
     */
    private void updateDefaultTime() {
        int count = mTCBubbleViewGroup != null ? mTCBubbleViewGroup.getChildCount() : 0;
        mDefaultWordStartTime = count * 3000; // 两个之间间隔3秒
        mDefaultWordEndTime = mDefaultWordStartTime + 2000;

        if (mDefaultWordStartTime > mDuration) {
            mDefaultWordStartTime = mDuration - 2000;
            mDefaultWordEndTime = mDuration;
        } else if (mDefaultWordEndTime > mDuration) {
            mDefaultWordEndTime = mDuration;
        }
    }

    // mAddBubbleAdapter底部的已添加的字幕列表选中
    @Override
    public void onItemClick(View view, int position) {
        if (position == mAddBubbleInfoList.size()) {
            // 新增
            clickBtnAdd();
        } else {
            if (!mTCBubbleViewGroup.isShown()) {
                mTCBubbleViewGroup.setVisibility(View.VISIBLE);
                mIvBubbleDel.setVisibility(View.VISIBLE);
                // 暂停播放
                ((TCVideoEffectActivity) getActivity()).pausePlay();
                mTXVideoEditer.refreshOneFrame();
            }
            // 列表选中
            mAddBubbleAdapter.setCurrentSelectedPos(position);
            // 预览界面选中
            mTCBubbleViewGroup.selectOperationView(position);
            // 进度条范围选中
            RangeSliderViewContainer lastSlider = ((TCVideoEffectActivity) getActivity()).mVideoProgressController.getRangeSliderView(ViewConst.VIEW_TYPE_WORD, mCurrentSelectedPos);
            if (lastSlider != null) {
                lastSlider.setEditComplete();
            }

            RangeSliderViewContainer currentSlider = ((TCVideoEffectActivity) getActivity()).mVideoProgressController.getRangeSliderView(ViewConst.VIEW_TYPE_WORD, position);
            if (currentSlider != null) {
                currentSlider.showEdit();
            }

            mCurrentSelectedPos = position;
        }
    }

    private void clickBtnAdd() {
        mBubblePopWin.show(null);
        mTCBubbleViewGroup.setVisibility(View.VISIBLE);
        mIvBubbleDel.setVisibility(View.VISIBLE);
        // 暂停播放
        ((TCVideoEffectActivity) getActivity()).pausePlay();
    }

    private void showInputDialog(String text) {
        if (mWordInputDialog == null) {
            mWordInputDialog = new TCWordInputDialog();
            mWordInputDialog.setOnWordInputCallback(this);
            mWordInputDialog.setCancelable(false);
        }
        mWordInputDialog.setDefaultText(text);
        mWordInputDialog.show(getChildFragmentManager(), "word_input_dialog");
    }

    @Override
    public void onLayerOperationViewItemClick(TCLayerOperationView view, int lastSelectedPos, int currentSelectedPos) {
        TCBubbleViewParams tcBubbleViewParams = ((TCWordBubbleView) view).getBubbleParams();
        showInputDialog(((TCWordBubbleView) view).getBubbleParams().text); // 再次点击已选中的 字幕控件，则弹出文字输入框
    }

    @Override
    public void onWordInfoCallback(TCWordParamsInfo info) {
        // 新增气泡字幕
        if (!mIsEditWordAgain) {
            String defaultText = "双击修改文字";
            // 创建一个默认的参数
            TCBubbleViewParams params = TCBubbleViewParams.createDefaultParams(defaultText);
            // 添加到气泡view
            TCWordBubbleView view = createDefaultBubbleView(params);
            mTCBubbleViewGroup.addOperationView(view);// 添加到Group中去管理
            params.wordParamsInfo = info;
            params.bubbleBitmap = TCBubbleManager.getInstance(getActivity()).getBitmapFromAssets(params.wordParamsInfo.getBubbleInfo().getBubblePath());
            view.setBubbleParams(params);

            int count = mAddBubbleInfoList.size();
//            params.text = "字幕" + String.valueOf(count + 1);
            params.text = defaultText;
            // 更新下方的贴纸列表
            mAddBubbleInfoList.add(params);
            mAddBubbleAdapter.notifyDataSetChanged();
            mAddBubbleAdapter.setCurrentSelectedPos(mAddBubbleInfoList.size() - 1);

            // 更新进度条上的开始结束位置
            RangeSliderViewContainer rangeSliderView = new RangeSliderViewContainer(getActivity());
            rangeSliderView.init(((TCVideoEffectActivity) getActivity()).mVideoProgressController, mDefaultWordStartTime, mDefaultWordEndTime - mDefaultWordStartTime, mDuration);
            rangeSliderView.setDurationChangeListener(mOnDurationChangeListener);
            ((TCVideoEffectActivity) getActivity()).mVideoProgressController.addRangeSliderView(ViewConst.VIEW_TYPE_WORD, rangeSliderView);
            ((TCVideoEffectActivity) getActivity()).mVideoProgressController.setCurrentTimeMs(mDefaultWordStartTime);

            // 更新一下默认配置的时间
            updateDefaultTime();

//            mCurrentSelectedPos = info.getBubblePos();
            mCurrentSelectedPos = mAddBubbleInfoList.size() - 1;
        }
        // 修改
        else {
            //获取当前处于编辑状态的气泡字幕的view
            TCWordBubbleView view = (TCWordBubbleView) mTCBubbleViewGroup.getSelectedLayerOperationView();
            int index = mTCBubbleViewGroup.getSelectedViewIndex();
            if (view != null) {
                TCBubbleViewParams params = view.getBubbleParams();
                params.wordParamsInfo = info;
                params.bubbleBitmap = TCBubbleManager.getInstance(getActivity()).getBitmapFromAssets(params.wordParamsInfo.getBubbleInfo().getBubblePath());
                view.setBubbleParams(params);
            }
            TCBubbleViewParams tcBubbleViewParams = mAddBubbleInfoList.get(index);
            tcBubbleViewParams.wordParamsInfo = info;
            mAddBubbleAdapter.notifyDataSetChanged();

            mIsEditWordAgain = false;
        }
        mBubblePopWin.dismiss();

        addSubtitlesIntoVideo();
        saveIntoManager();
    }

    // 添加一个字幕控件到Group中，并显示出来
    public TCWordBubbleView createDefaultBubbleView(TCBubbleViewParams params) {
        final TCWordBubbleView view = TCWordBubbleViewFactory.newOperationView(getActivity()); // 创建一个气泡字幕的控件实例

        // 根据params初始化对应的控件
        view.setBubbleParams(params);
        view.showDelete(false);

        // 设置view显示出来的位置
        view.setCenterX(mTCBubbleViewGroup.getWidth() / 2);// 控件显示在父容器的中心
        view.setCenterY(mTCBubbleViewGroup.getHeight() / 2);// 控件显示在父容器的中心

        // 初始化字幕的默认的显示时间区间
        view.setStartTime(mDefaultWordStartTime, mDefaultWordEndTime);

        view.setIOperationViewClickListener(this);// 监听回调

        return view;
    }

    /****** 可编辑控件的回调start ******/
    @Override
    public void onDeleteClick() {

    }

    @Override
    public void onEditClick() {
        TCWordBubbleView view = (TCWordBubbleView) mTCBubbleViewGroup.getSelectedLayerOperationView();
        if (view != null) {
            mBubblePopWin.show(view.getBubbleParams().wordParamsInfo);
        }
        mIsEditWordAgain = true;
    }

    // 拖动、旋转的回调
    @Override
    public void onRotateClick() {
        Log.i(TAG, "onRotateClick");

        addSubtitlesIntoVideo();
        saveIntoManager();
    }

    /****** 可编辑控件的回调end ******/

    @Override
    public void onInputSure(String text) {
        if (mWordInputDialog != null) {
            mWordInputDialog = null;
        }
        // 修改字幕内容
        TCWordBubbleView view = (TCWordBubbleView) mTCBubbleViewGroup.getSelectedLayerOperationView();
        int index = mTCBubbleViewGroup.getSelectedViewIndex();
        if (view == null) {
            return;
        }
        TCBubbleViewParams params = view.getBubbleParams();
        params.text = text;
        params.bubbleBitmap = TCBubbleManager.getInstance(getActivity()).getBitmapFromAssets(params.wordParamsInfo.getBubbleInfo().getBubblePath());
        view.setBubbleParams(params);

        TCBubbleViewParams tcBubbleViewParams = mAddBubbleInfoList.get(index);
        tcBubbleViewParams.text = text;
        mAddBubbleAdapter.notifyDataSetChanged();

        mIsEditWordAgain = false;

        addSubtitlesIntoVideo();
        saveIntoManager();
    }

    @Override
    public void onInputCancel() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_bubble_del:
                deleteBubble();
                break;
        }
    }

    /**
     * ===========================将字幕添加到SDK中去=================================
     */
    private void addSubtitlesIntoVideo() {
        List<TXVideoEditConstants.TXSubtitle> subtitleList = new ArrayList<>();
        for (int i = 0; i < mTCBubbleViewGroup.getChildCount(); i++) {
            TCWordBubbleView view = (TCWordBubbleView) mTCBubbleViewGroup.getOperationView(i);
            TXVideoEditConstants.TXSubtitle subTitle = new TXVideoEditConstants.TXSubtitle();
            subTitle.titleImage = view.getRotateBitmap();

            TXVideoEditConstants.TXRect rect = new TXVideoEditConstants.TXRect();
            rect.x = view.getImageX();
            rect.y = view.getImageY();

            TXCLog.i(TAG, "addSubtitlesIntoVideo, subTitle x y = " + rect.x + "," + rect.y);

            rect.width = view.getImageWidth();
            subTitle.frame = rect;
            subTitle.startTime = view.getStartTime();
            subTitle.endTime = view.getEndTime();
            subtitleList.add(subTitle);
        }
        mTXVideoEditer.setSubtitleList(subtitleList);
    }

    /**
     * ===========================将字幕控件参数保存到Manager中去=================================
     * <p>
     * 将字幕控件的相关参数保存到Manager中去，方便出去之后可以重新进来再次编辑字幕
     */
    private void saveIntoManager() {
        TCBubbleViewInfoManager manager = TCBubbleViewInfoManager.getInstance();
        manager.clear();
        for (int i = 0; i < mTCBubbleViewGroup.getChildCount(); i++) {
            TCWordBubbleView view = (TCWordBubbleView) mTCBubbleViewGroup.getOperationView(i);

            Log.i(TAG, "saveIntoManager: x = " + view.getCenterX() + " y = " + view.getCenterY());

            TCBubbleViewInfo info = new TCBubbleViewInfo();
            info.setViewCenterX(view.getCenterX());
            info.setViewCenterY(view.getCenterY());
            info.setRotation(view.getImageRotate());
            info.setViewParams(view.getBubbleParams());
            info.setStartTime(view.getStartTime());
            info.setEndTime(view.getEndTime());
            info.setScale(view.getImageScale());

//            view.setBubbleParams(null);
            manager.add(info);
        }
    }

    /**
     * 将字幕控件的相关参数从Manager中重新恢复出来，恢复字幕编辑的场景。 以便继续编辑
     */
    private void recoverFromManager() {
        TCBubbleViewInfoManager manager = TCBubbleViewInfoManager.getInstance();
        for (int i = 0; i < manager.size(); i++) {
            TCBubbleViewInfo info = manager.get(i);

            TCBubbleViewParams params = info.getViewParams();
            // params设置进Bubble之后是不保存bitmap的,会被置空释放掉
            // 重新loadBitmap (因为在设置BubbleView的时候，原来的气泡Bitmap回在内部被回收。 所以这里直接重新load多一边
            params.bubbleBitmap = TCBubbleManager.getInstance(getActivity()).getBitmapFromAssets(params.wordParamsInfo.getBubbleInfo().getBubblePath());

            TCWordBubbleView view = createDefaultBubbleView(info.getViewParams());
            view.setCenterX(info.getViewCenterX());
            view.setCenterY(info.getViewCenterY());
            Log.i(TAG, "recoverFromManager: x = " + info.getViewCenterX() + " y = " + info.getViewCenterY());
            view.setImageRotate(info.getRotation());
            view.setImageScale(info.getScale());

            // 恢复时间的时候，需要检查一下是否符合这一次区间的startTime和endTime
            long viewStartTime = info.getStartTime();
            long viewEndTime = info.getEndTime();
            view.setStartTime(viewStartTime, viewEndTime);
            mTCBubbleViewGroup.addOperationView(view);// 添加到Group中去管理


            RangeSliderViewContainer rangeSliderView = new RangeSliderViewContainer(getActivity());
            rangeSliderView.init(((TCVideoEffectActivity) getActivity()).mVideoProgressController, viewStartTime, viewEndTime - viewStartTime, mDuration);
            rangeSliderView.setDurationChangeListener(mOnDurationChangeListener);
            rangeSliderView.setEditComplete();
            ((TCVideoEffectActivity) getActivity()).mVideoProgressController.addRangeSliderView(ViewConst.VIEW_TYPE_WORD, rangeSliderView);

            mAddBubbleInfoList.add(params);
        }
        mCurrentSelectedPos = manager.size() - 1;

        mAddBubbleAdapter.notifyDataSetChanged();
    }

    private void deleteBubble() {
        int index = mTCBubbleViewGroup.getSelectedViewIndex();
        if(index < 0){
            return;
        }
        TCWordBubbleView view = (TCWordBubbleView) mTCBubbleViewGroup.getSelectedLayerOperationView();
        if (view != null) {
            mTCBubbleViewGroup.removeOperationView(view);
        }
        ((TCVideoEffectActivity) getActivity()).mVideoProgressController.removeRangeSliderView(ViewConst.VIEW_TYPE_WORD, index);

        mAddBubbleInfoList.remove(index);
        mAddBubbleAdapter.notifyDataSetChanged();

        mCurrentSelectedPos = -1;
        mAddBubbleAdapter.setCurrentSelectedPos(mCurrentSelectedPos);

        addSubtitlesIntoVideo();
        saveIntoManager();
    }

    @Override
    public void notifyStartPlay() {
        if(mTCBubbleViewGroup != null){
            mTCBubbleViewGroup.setVisibility(View.GONE);
//            RangeSliderViewContainer lastSlider = ((TCVideoEffectActivity) getActivity()).mVideoProgressController.getRangeSliderView(ViewConst.VIEW_TYPE_WORD, mCurrentSelectedPos);
//            if (lastSlider != null) {
//                lastSlider.setEditComplete();
//            }
        }
    }

    @Override
    public void notifyPausePlay() {
        if(mTCBubbleViewGroup != null){
            mTCBubbleViewGroup.setVisibility(View.VISIBLE);
//            RangeSliderViewContainer lastSlider = ((TCVideoEffectActivity) getActivity()).mVideoProgressController.getRangeSliderView(ViewConst.VIEW_TYPE_WORD, mCurrentSelectedPos);
//            if (lastSlider != null) {
//                lastSlider.showEdit();
//            }
        }
    }

    @Override
    public void notifyResumePlay() {
        if(mTCBubbleViewGroup != null){
            mTCBubbleViewGroup.setVisibility(View.GONE);
//            RangeSliderViewContainer lastSlider = ((TCVideoEffectActivity) getActivity()).mVideoProgressController.getRangeSliderView(ViewConst.VIEW_TYPE_WORD, mCurrentSelectedPos);
//            if (lastSlider != null) {
//                lastSlider.setEditComplete();
//            }
        }
    }
}
