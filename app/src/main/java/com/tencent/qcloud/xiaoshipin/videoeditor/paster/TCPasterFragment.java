package com.tencent.qcloud.xiaoshipin.videoeditor.paster;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.faceunity.beautycontrolview.BeautyControlView;
import com.faceunity.beautycontrolview.FURenderer;
import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.utils.FileUtils;
import com.tencent.qcloud.xiaoshipin.videoeditor.BaseEditFragment;
import com.tencent.qcloud.xiaoshipin.videoeditor.TCVideoEditerWrapper;
import com.tencent.qcloud.xiaoshipin.videoeditor.TCVideoEffectActivity;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.BaseRecyclerAdapter;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.layer.TCLayerOperationView;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.layer.TCLayerViewGroup;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.videotimeline.RangeSliderViewContainer;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.videotimeline.ViewConst;
import com.tencent.qcloud.xiaoshipin.videoeditor.paster.view.PasterAdapter;
import com.tencent.qcloud.xiaoshipin.videoeditor.paster.view.PasterOperationView;
import com.tencent.qcloud.xiaoshipin.videoeditor.paster.view.TCPasterOperationViewFactory;
import com.tencent.qcloud.xiaoshipin.videoeditor.paster.view.TCPasterSelectView;
import com.tencent.ugc.TXVideoEditConstants;
import com.tencent.ugc.TXVideoEditer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vinsonswang on 2017/12/12.
 */

public class TCPasterFragment extends BaseEditFragment implements BaseRecyclerAdapter.OnItemClickListener,
        TCLayerViewGroup.OnItemClickListener,
        TCLayerOperationView.IOperationViewClickListener,
        PasterAdapter.OnItemClickListener,
        TCPasterSelectView.OnTabChangedListener,
        TCPasterSelectView.OnAddClickListener,
        View.OnClickListener {
    private final String TAG = "TCPasterFragment";

    private final int MSG_COPY_PASTER_FILES = 1;

    private final String PASTER_FOLDER_NAME = "paster";
    private final String ANIMATED_PASTER_FOLDER_NAME = "AnimatedPaster";
    private final String PASTER_LIST_JSON_FILE_NAME = "pasterList.json";
    private String mPasterSDcardFolder;
    private String mAnimatedPasterSDcardFolder;

    private TXVideoEditer mTXVideoEditer;

    private TextView mTvPlay;
//    private RecyclerView mRvPaster;
//    private ImageView mIvDel;
//    private View mFootView;
//    private AddPasterAdapter mAddPasterAdapter;
//    private List<TCPasterInfo> mAddPasterInfoList;

    private TCPasterSelectView mTCPasterSelectView; // 选择贴纸控件
    private TCLayerViewGroup mTCLayerViewGroup; // 图层父布局，承载贴纸
//    private int mCurrentSelectedPos = -1;// 当前被选中的贴纸控件

    private RangeSliderViewContainer.OnDurationChangeListener mOnDurationChangeListener;

    // 子线程
    private HandlerThread mWorkHandlerThread;
    private Handler mWorkHandler;

    private List<TCPasterInfo> mPasterInfoList;
    private List<TCPasterInfo> mAnimatedPasterInfoList;

    private boolean mIsUpdatePng = false;

    //================================== 美颜贴纸 ==============================
    private FURenderer mFURenderer;
    private BeautyControlView mBeautyControlView;
    private boolean isInit = false;//是否初始化
    private boolean isUserFilter = false;//是否使用美颜

    //================================== 时间 ==============================
    private long mDuration;
    private long mDefaultWordStartTime;
    private long mDefaultWordEndTime;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_paster, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView(view);

        initData();

        initHandler();

        initRangeDurationChangeListener();

        mWorkHandler.sendEmptyMessage(MSG_COPY_PASTER_FILES);

        recoverFromManager();
        Log.d("activity", "onViewCreated");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("activity", "onResume");
        isUserFilter = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        isUserFilter = false;
        Log.d("activity", "onPause");
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            mTCLayerViewGroup.setVisibility(View.GONE);
            ((TCVideoEffectActivity) getActivity()).mVideoProgressController.showAllRangeSliderView(ViewConst.VIEW_TYPE_PASTER, false);
        } else {
            ((TCVideoEffectActivity) getActivity()).mVideoProgressController.showAllRangeSliderView(ViewConst.VIEW_TYPE_PASTER, true);
        }
    }

    private void initRangeDurationChangeListener() {
        mOnDurationChangeListener = new RangeSliderViewContainer.OnDurationChangeListener() {
            @Override
            public void onDurationChange(long startTime, long endTime) {
                // 获取当选中的贴纸，并且将时间设置进去
                PasterOperationView view = (PasterOperationView) mTCLayerViewGroup.getSelectedLayerOperationView();
                if (view != null) {
                    view.setStartTime(startTime, endTime);
                }
                // 时间范围修改也马上设置到sdk中去
                addPasterListVideo();
            }
        };
    }

    private void initView(View view) {
//        mFootView = LayoutInflater.from(view.getContext()).inflate(R.layout.item_add, null);
//        mAddPasterInfoList = new ArrayList<>();
//        mRvPaster = (RecyclerView) view.findViewById(R.id.paster_rv_list);
//        mRvPaster.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
//        mAddPasterAdapter = new AddPasterAdapter(mAddPasterInfoList);
//        mAddPasterAdapter.setOnItemClickListener(this);
//        mRvPaster.setAdapter(mAddPasterAdapter);
//        mAddPasterAdapter.setFooterView(mFootView);

        mTCPasterSelectView = (TCPasterSelectView) getActivity().findViewById(R.id.tcpaster_select_view);
        mTCPasterSelectView.setOnTabChangedListener(this);
        mTCPasterSelectView.setOnItemClickListener(this);
        mTCPasterSelectView.setOnAddClickListener(this);
        mTCPasterSelectView.setVisibility(View.GONE);

        mTCLayerViewGroup = (TCLayerViewGroup) getActivity().findViewById(R.id.paster_container);
        mTCLayerViewGroup.setOnItemClickListener(this);
        mTCLayerViewGroup.enableChildSingleClick(false); // 在容器里不响应子控件的单击事件
        mTCLayerViewGroup.enableDoubleChildClick(false); // 在容器里不响应子控件的双击事件

//        mIvDel = (ImageView) view.findViewById(R.id.iv_del);
//        mIvDel.setOnClickListener(this);

        mFURenderer = new FURenderer.Builder(getActivity())
                .inputTextureType(0)
                .setNeedFaceBeauty(true)
                .build();
        mBeautyControlView = (BeautyControlView) view.findViewById(R.id.beauty_control);
        mBeautyControlView.setOnFaceUnityControlListener(mFURenderer);
    }

    private void initData() {
        mPasterSDcardFolder = getActivity().getExternalFilesDir(null) + File.separator + PASTER_FOLDER_NAME + File.separator;
        mAnimatedPasterSDcardFolder = getActivity().getExternalFilesDir(null) + File.separator + ANIMATED_PASTER_FOLDER_NAME + File.separator;

        mTXVideoEditer = TCVideoEditerWrapper.getInstance().getEditer();
        mDuration = TCVideoEditerWrapper.getInstance().getTXVideoInfo().duration;
        updateDefaultTime();

        mTXVideoEditer.setCustomVideoProcessListener(new TXVideoEditer.TXVideoCustomProcessListener() {
            @Override
            public int onTextureCustomProcess(int textureId, int width, int height, long timestamp) {
                Log.d(TAG, "onTextureCustomProcess:textureId=" + textureId);
                if (!isUserFilter) {
                    return textureId;
                }
                if (!isInit) {
                    mFURenderer.onSurfaceCreated();
                    isInit = true;
                }
                return mFURenderer.onDrawFrame(textureId, width, height);
            }

            @Override
            public void onTextureDestroyed() {
                Log.d(TAG, "onTextureDestroyed");
            }
        });
    }

    /**
     * 根据当前控件数量 更新默认的一个控件开始时间和结束时间
     */
    private void updateDefaultTime() {
        int count = mTCLayerViewGroup != null ? mTCLayerViewGroup.getChildCount() : 0;
        mDefaultWordStartTime = count * 1000; // 两个之间间隔1秒
        mDefaultWordEndTime = mDefaultWordStartTime + 2000;

        if (mDefaultWordStartTime > mDuration) {
            mDefaultWordStartTime = mDuration - 2000;
            mDefaultWordEndTime = mDuration;
        } else if (mDefaultWordEndTime > mDuration) {
            mDefaultWordEndTime = mDuration;
        }
    }

    private void initHandler() {
        mWorkHandlerThread = new HandlerThread("TCPasterFragment_handlerThread");
        mWorkHandlerThread.start();
        mWorkHandler = new Handler(mWorkHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_COPY_PASTER_FILES:
                        if (mIsUpdatePng) {
                            FileUtils.deleteFile(mPasterSDcardFolder);
                            FileUtils.deleteFile(mAnimatedPasterSDcardFolder);
                        }
                        File pasterFolder = new File(mPasterSDcardFolder);
                        File animatedPasterFolder = new File(mAnimatedPasterSDcardFolder);
                        if (!pasterFolder.exists() || !animatedPasterFolder.exists()) {
                            copyPasterFilesToSdcard();
                        }
                        preparePasterInfoToShow();
                        break;
                }
            }
        };
    }

    // mAddPasterAdapter底部的已添加的贴纸列表选中
    @Override
    public void onItemClick(View view, int position) {
//        if(position == mAddPasterInfoList.size()){
//            // 新增
//            clickBtnAdd();
//        }else{
//            if( !mTCLayerViewGroup.isShown() ){
//                mTCLayerViewGroup.setVisibility(View.VISIBLE);
//                // 暂停播放
//                ((TCVideoEffectActivity) getActivity()).pausePlay();
//                mTXVideoEditer.refreshOneFrame();
//            }
//            // 列表选中
//            mAddPasterAdapter.setCurrentSelectedPos(position);
//            // 预览界面选中
//            mTCLayerViewGroup.selectOperationView(position);
//            // 进度条范围选中
//            RangeSliderViewContainer lastSlider = ((TCVideoEffectActivity) getActivity()).mVideoProgressController.getRangeSliderView(ViewConst.VIEW_TYPE_PASTER, mCurrentSelectedPos);
//            if (lastSlider != null) {
//                lastSlider.setEditComplete();
//            }
//
//            RangeSliderViewContainer currentSlider = ((TCVideoEffectActivity) getActivity()).mVideoProgressController.getRangeSliderView(ViewConst.VIEW_TYPE_PASTER, position);
//            if (currentSlider != null) {
//                currentSlider.showEdit();
//            }
//
//            mCurrentSelectedPos = position;
//        }
    }

    private void clickBtnAdd() {
        mTCPasterSelectView.show();
        mTCLayerViewGroup.setVisibility(View.VISIBLE);
        // 暂停播放
        ((TCVideoEffectActivity) getActivity()).pausePlay();
    }

    private void copyPasterFilesToSdcard() {
        File pasterFolder = new File(mPasterSDcardFolder);
        if (!pasterFolder.exists()) {
            FileUtils.copyFilesFromAssets(getActivity(), PASTER_FOLDER_NAME, mPasterSDcardFolder);
        }

        File animatedFolder = new File(mAnimatedPasterSDcardFolder);
        if (!animatedFolder.exists()) {
            FileUtils.copyFilesFromAssets(getActivity(), ANIMATED_PASTER_FOLDER_NAME, mAnimatedPasterSDcardFolder);
        }
    }

    private void preparePasterInfoToShow() {
        mPasterInfoList = getPasterInfoList(PasterOperationView.TYPE_CHILD_VIEW_PASTER, mPasterSDcardFolder, PASTER_LIST_JSON_FILE_NAME);
        mAnimatedPasterInfoList = getPasterInfoList(PasterOperationView.TYPE_CHILD_VIEW_ANIMATED_PASTER, mAnimatedPasterSDcardFolder, PASTER_LIST_JSON_FILE_NAME);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int currentTab = mTCPasterSelectView.getCurrentTab();
                changeListViewData(currentTab);
            }
        });
    }

    private void changeListViewData(int currentTab) {
        if (currentTab == TCPasterSelectView.TAB_PASTER) {
            mTCPasterSelectView.setPasterInfoList(mPasterInfoList);
        } else if (currentTab == TCPasterSelectView.TAB_ANIMATED_PASTER) {
            mTCPasterSelectView.setPasterInfoList(mAnimatedPasterInfoList);
        }
    }

    private List<TCPasterInfo> getPasterInfoList(int pasterType, String fileFolder, String fileName) {
        String filePath = fileFolder + fileName;
        List<TCPasterInfo> pasterInfoList = new ArrayList<TCPasterInfo>();
        try {
            String jsonString = FileUtils.getJsonFromFile(filePath);
            if (TextUtils.isEmpty(jsonString)) {
                TXCLog.e(TAG, "getPasterInfoList, jsonString is empty");
                return pasterInfoList;
            }
            JSONObject pasterJson = new JSONObject(jsonString);
            JSONArray pasterInfoJsonArray = pasterJson.getJSONArray("pasterList");
            for (int i = 0; i < pasterInfoJsonArray.length(); i++) {
                JSONObject pasterInfoJsonObject = pasterInfoJsonArray.getJSONObject(i);
                TCPasterInfo tcPasterInfo = new TCPasterInfo();

                tcPasterInfo.setName(pasterInfoJsonObject.getString("name"));
                tcPasterInfo.setIconPath(fileFolder + pasterInfoJsonObject.getString("icon"));
                tcPasterInfo.setPasterType(pasterType);

                pasterInfoList.add(tcPasterInfo);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return pasterInfoList;
    }

    @Override
    public void onLayerOperationViewItemClick(TCLayerOperationView view, int lastSelectedPos, int currentSelectedPos) {
//        pausePlay(true);
//
//        RangeSliderViewContainer lastSlider = ((TCVideoEffectActivity) getActivity()).mVideoProgressController.getRangeSliderView(lastSelectedPos);
//        if (lastSlider != null) {
//            lastSlider.setEditComplete();
//        }
//
//        RangeSliderViewContainer currentSlider = ((TCVideoEffectActivity) getActivity()).mVideoProgressController.getRangeSliderView(currentSelectedPos);
//        if (currentSlider != null) {
//            currentSlider.showEdit();
//        }
//
//        mCurrentSelectedPos = currentSelectedPos;
    }

    private void pausePlay(boolean isShow) {
        ((TCVideoEffectActivity) getActivity()).pausePlay();

        if (isShow) {
            // 将字幕控件显示出来
            mTCLayerViewGroup.setVisibility(View.VISIBLE);
            mTXVideoEditer.refreshOneFrame();// 将视频画面中的字幕清除  ，避免与上层控件造成混淆导致体验不好的问题。
        }
        int selectedIndex = mTCLayerViewGroup.getSelectedViewIndex();
        if (selectedIndex != -1) {// 说明有控件被选中 那么显示出时间区间的选择
            RangeSliderViewContainer view = ((TCVideoEffectActivity) getActivity()).mVideoProgressController.getRangeSliderView(selectedIndex);
            if (isShow) {
                view.showEdit();
            } else {
                view.setEditComplete();
            }
        }
    }

    // 选择贴纸
    @Override
    public void onItemClick(TCPasterInfo tcPasterInfo, int position) {
        int index = mTCLayerViewGroup.getSelectedViewIndex();
        TXCLog.i(TAG, "onItemClick: index = " + index);
        RangeSliderViewContainer lastSlider = ((TCVideoEffectActivity) getActivity()).mVideoProgressController.getRangeSliderView(index);
        if (lastSlider != null) {
            lastSlider.setEditComplete();
        } else {
            Log.e(TAG, "onItemClick: slider view is null");
        }

        String pasterPath = null;
        Bitmap bitmap = null;
        int pasterType = tcPasterInfo.getPasterType();
        if (pasterType == PasterOperationView.TYPE_CHILD_VIEW_ANIMATED_PASTER) {
            AnimatedPasterConfig animatedPasterConfig = getAnimatedPasterParamFromPath(mAnimatedPasterSDcardFolder + tcPasterInfo.getName() + File.separator);
            if (animatedPasterConfig == null) {
                TXCLog.e(TAG, "onItemClick, animatedPasterConfig is null");
                return;
            }
            int keyFrameIndex = animatedPasterConfig.keyframe;
            String keyFrameName = animatedPasterConfig.frameArray.get(keyFrameIndex - 1).pictureName;
            pasterPath = mAnimatedPasterSDcardFolder + tcPasterInfo.getName() + File.separator + keyFrameName + ".png";
            bitmap = BitmapFactory.decodeFile(pasterPath);
        } else if (pasterType == PasterOperationView.TYPE_CHILD_VIEW_PASTER) {
            pasterPath = mPasterSDcardFolder + tcPasterInfo.getName() + File.separator + tcPasterInfo.getName() + ".png";
            bitmap = BitmapFactory.decodeFile(pasterPath);
        }
        // 更新一下默认配置的时间
        updateDefaultTime();

        PasterOperationView pasterOperationView = TCPasterOperationViewFactory.newOperationView(getActivity());
        pasterOperationView.setPasterPath(pasterPath);
        pasterOperationView.setChildType(tcPasterInfo.getPasterType());
//        pasterOperationView.setImageBitamp(bitmap);
        pasterOperationView.setmIconPath(tcPasterInfo.getIconPath());
        pasterOperationView.setCenterX(mTCLayerViewGroup.getWidth() / 2);
        pasterOperationView.setCenterY(mTCLayerViewGroup.getHeight() / 2);
        pasterOperationView.setStartTime(mDefaultWordStartTime, mDefaultWordEndTime);
        pasterOperationView.setIOperationViewClickListener(this);
        pasterOperationView.setPasterName(tcPasterInfo.getName());
        pasterOperationView.showDelete(false);
        pasterOperationView.showEdit(false);

        RangeSliderViewContainer rangeSliderView = new RangeSliderViewContainer(getActivity());
        rangeSliderView.init(((TCVideoEffectActivity) getActivity()).mVideoProgressController, mDefaultWordStartTime, mDefaultWordEndTime - mDefaultWordStartTime, mDuration);
        rangeSliderView.setDurationChangeListener(mOnDurationChangeListener);
        ((TCVideoEffectActivity) getActivity()).mVideoProgressController.addRangeSliderView(ViewConst.VIEW_TYPE_PASTER, rangeSliderView);
        ((TCVideoEffectActivity) getActivity()).mVideoProgressController.setCurrentTimeMs(mDefaultWordStartTime);

//        mCurrentState = STATE_PREVIEW_AT_TIME;
//        mPreviewAtTime = mDefaultWordStartTime;

        mTCLayerViewGroup.addOperationView(pasterOperationView);
        pasterOperationView.setImageBitamp(bitmap);
        mTCPasterSelectView.dismiss();

        // 更新下方的贴纸列表
//        mAddPasterInfoList.add(tcPasterInfo);
//        mAddPasterAdapter.notifyDataSetChanged();
//        mAddPasterAdapter.setCurrentSelectedPos(mAddPasterInfoList.size() - 1);

//        mCurrentSelectedPos = mAddPasterInfoList.size() - 1;

        addPasterListVideo();
        saveIntoManager();
    }

    // 动态、静态切换
    @Override
    public void onTabChanged(int currentTab) {
        changeListViewData(currentTab);
    }

    /****** 可编辑控件的回调start ******/
    @Override
    public void onDeleteClick() {
//        int index = mTCLayerViewGroup.getSelectedViewIndex();
//        PasterOperationView view = (PasterOperationView) mTCLayerViewGroup.getSelectedLayerOperationView();
//        if (view != null) {
//            mTCLayerViewGroup.removeOperationView(view);
//        }
//        ((TCVideoEffectActivity) getActivity()).mVideoProgressController.removeRangeSliderView(index);
//
//        mAddPasterInfoList.remove(index);
//        mAddPasterAdapter.notifyDataSetChanged();
//        mAddPasterAdapter.setCurrentSelectedPos(-1);
    }

    @Override
    public void onEditClick() {

    }

    // 拖动、旋转的回调
    @Override
    public void onRotateClick() {
        addPasterListVideo();
        saveIntoManager();
    }

    /****** 可编辑控件的回调end ******/

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
//            case R.id.tv_paster_play:
//                int playState = ((TCVideoEffectActivity) getActivity()).getmCurrentState();
//                TXCLog.i(TAG, "playState = " + playState);
//                if(playState == PlayState.STATE_PAUSE || playState == PlayState.STATE_PREVIEW_AT_TIME){
//                    mTCLayerViewGroup.setVisibility(View.GONE);
//                    addPasterListVideoToEditer();
//                    saveIntoManager();
//                }else{
//                    mTCLayerViewGroup.setVisibility(View.VISIBLE);
//                }
//
//                ((TCVideoEffectActivity) getActivity()).switchPlayVideo();
//                break;

//            case R.id.iv_del:
//                deletePaster();
//                break;
        }
    }

    private void deletePaster() {
        int index = mTCLayerViewGroup.getSelectedViewIndex();
        if (index < 0) {
            return;
        }
        PasterOperationView view = (PasterOperationView) mTCLayerViewGroup.getSelectedLayerOperationView();
        if (view != null) {
            mTCLayerViewGroup.removeOperationView(view);
        }
        ((TCVideoEffectActivity) getActivity()).mVideoProgressController.removeRangeSliderView(ViewConst.VIEW_TYPE_PASTER, index);

//        if(mAddPasterInfoList.size() > 0){
//            mAddPasterInfoList.remove(index);
//        }

//        mAddPasterAdapter.notifyDataSetChanged();

//        mCurrentSelectedPos = -1;
//        mAddPasterAdapter.setCurrentSelectedPos(mCurrentSelectedPos);

        addPasterListVideo();
        saveIntoManager();
    }

    /**
     * 从指定路径加载贴纸配置
     *
     * @param pathFolder
     * @return
     */
    private AnimatedPasterConfig getAnimatedPasterParamFromPath(String pathFolder) {
        AnimatedPasterConfig animatedPasterConfig = null;
        String configPath = pathFolder + AnimatedPasterConfig.FILE_NAME;

        String configJsonStr = FileUtils.getJsonFromFile(configPath);

        if (TextUtils.isEmpty(configJsonStr)) {
            TXCLog.e(TAG, "getTXAnimatedPasterParamFromPath, configJsonStr is empty");
            return animatedPasterConfig;
        }

        JSONObject jsonObjectConfig = null;
        try {
            jsonObjectConfig = new JSONObject(configJsonStr);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (jsonObjectConfig == null) {
            TXCLog.e(TAG, "getTXAnimatedPasterParamFromPath, jsonObjectConfig is null");
            return animatedPasterConfig;
        }

        animatedPasterConfig = new AnimatedPasterConfig();
        try {
            animatedPasterConfig.name = jsonObjectConfig.getString(AnimatedPasterConfig.CONFIG_NAME);
            animatedPasterConfig.count = jsonObjectConfig.getInt(AnimatedPasterConfig.CONFIG_COUNT);
            animatedPasterConfig.period = jsonObjectConfig.getInt(AnimatedPasterConfig.CONFIG_PERIOD);
            animatedPasterConfig.width = jsonObjectConfig.getInt(AnimatedPasterConfig.CONFIG_WIDTH);
            animatedPasterConfig.height = jsonObjectConfig.getInt(AnimatedPasterConfig.CONFIG_HEIGHT);
            animatedPasterConfig.keyframe = jsonObjectConfig.getInt(AnimatedPasterConfig.CONFIG_KEYFRAME);
            JSONArray frameJsonArray = jsonObjectConfig.getJSONArray(AnimatedPasterConfig.CONFIG_KEYFRAME_ARRAY);
            for (int i = 0; i < animatedPasterConfig.count; i++) {
                JSONObject frameNameObject = frameJsonArray.getJSONObject(i);
                AnimatedPasterConfig.PasterPicture pasterPicture = new AnimatedPasterConfig.PasterPicture();
                pasterPicture.pictureName = frameNameObject.getString(AnimatedPasterConfig.PasterPicture.PICTURE_NAME);

                animatedPasterConfig.frameArray.add(pasterPicture);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return animatedPasterConfig;
    }

    /**
     * ===========================将贴纸添加到SDK中去=================================
     */
    private void addPasterListVideo() {
        List<TXVideoEditConstants.TXAnimatedPaster> animatedPasterList = new ArrayList<>();
        List<TXVideoEditConstants.TXPaster> pasterList = new ArrayList<>();
        for (int i = 0; i < mTCLayerViewGroup.getChildCount(); i++) {
            PasterOperationView view = (PasterOperationView) mTCLayerViewGroup.getOperationView(i);
            TXVideoEditConstants.TXRect rect = new TXVideoEditConstants.TXRect();
            rect.x = view.getImageX();
            rect.y = view.getImageY();
            rect.width = view.getImageWidth();
            TXCLog.i(TAG, "addPasterListVideoToEditer, adjustPasterRect, paster x y = " + rect.x + "," + rect.y);

            int childType = view.getChildType();
            if (childType == PasterOperationView.TYPE_CHILD_VIEW_ANIMATED_PASTER) {
                TXVideoEditConstants.TXAnimatedPaster txAnimatedPaster = new TXVideoEditConstants.TXAnimatedPaster();

                txAnimatedPaster.animatedPasterPathFolder = mAnimatedPasterSDcardFolder + view.getPasterName() + File.separator;
                txAnimatedPaster.startTime = view.getStartTime();
                txAnimatedPaster.endTime = view.getEndTime();
                txAnimatedPaster.frame = rect;
                txAnimatedPaster.rotation = view.getImageRotate();

                animatedPasterList.add(txAnimatedPaster);
                TXCLog.i(TAG, "addPasterListVideoToEditer, txAnimatedPaster startTimeMs, endTime is : " + txAnimatedPaster.startTime + ", " + txAnimatedPaster.endTime);
            } else if (childType == PasterOperationView.TYPE_CHILD_VIEW_PASTER) {
                TXVideoEditConstants.TXPaster txPaster = new TXVideoEditConstants.TXPaster();

                txPaster.pasterImage = view.getRotateBitmap();
                txPaster.startTime = view.getStartTime();
                txPaster.endTime = view.getEndTime();
                txPaster.frame = rect;

                pasterList.add(txPaster);
                TXCLog.i(TAG, "addPasterListVideoToEditer, txPaster startTimeMs, endTime is : " + txPaster.startTime + ", " + txPaster.endTime);
            }
        }
        mTXVideoEditer.setAnimatedPasterList(animatedPasterList);
        mTXVideoEditer.setPasterList(pasterList);
    }

    /**
     * ===========================将贴纸控件参数保存到Manager中去=================================
     * <p>
     * 将贴纸控件的相关参数保存到Manager中去，方便出去之后可以重新进来再次编辑贴纸
     */
    private void saveIntoManager() {
        TXCLog.i(TAG, "saveIntoManager");
        TCPasterViewInfoManager manager = TCPasterViewInfoManager.getInstance();
        manager.clear();
        for (int i = 0; i < mTCLayerViewGroup.getChildCount(); i++) {
            PasterOperationView view = (PasterOperationView) mTCLayerViewGroup.getOperationView(i);

            TXCLog.i(TAG, "saveIntoManager, view centerX and centerY = " + view.getCenterX() + ", " + view.getCenterY() +
                    ", start end time = " + view.getStartTime() + ", " + view.getEndTime());

            TCPasterViewInfo info = new TCPasterViewInfo();
            info.setViewCenterX(view.getCenterX());
            info.setViewCenterY(view.getCenterY());
            info.setRotation(view.getImageRotate());
            info.setImageScale(view.getImageScale());
            info.setPasterPath(view.getPasterPath());
            info.setIconPath(view.getmIconPath());
            info.setStartTime(view.getStartTime());
            info.setEndTime(view.getEndTime());
            info.setName(view.getPasterName());
            info.setViewType(view.getChildType());

            manager.add(info);
        }
    }

    /**
     * 将贴纸控件的相关参数从Manager中重新恢复出来，恢复贴纸编辑的场景。 以便继续编辑
     */
    private void recoverFromManager() {
        TCPasterViewInfoManager manager = TCPasterViewInfoManager.getInstance();
        TXCLog.i(TAG, "recoverFromManager, manager.size = " + manager.getSize());
        for (int i = 0; i < manager.getSize(); i++) {
            TCPasterViewInfo info = manager.get(i);
            Bitmap pasterBitmap = BitmapFactory.decodeFile(info.getPasterPath());
            TXCLog.i(TAG, "recoverFromManager, info.getPasterPath() = " + info.getPasterPath());
            if (pasterBitmap == null) {
                TXCLog.e(TAG, "recoverFromManager, pasterBitmap is null!");
                continue;
            }
            PasterOperationView view = TCPasterOperationViewFactory.newOperationView(getActivity());
            view.setImageBitamp(pasterBitmap);
            view.setChildType(info.getViewType());
            view.setCenterX(info.getViewCenterX());
            view.setCenterY(info.getViewCenterY());
            view.setImageRotate(info.getRotation());
            view.setImageScale(info.getImageScale());
            view.setPasterPath(info.getPasterPath());
            view.setmIconPath(info.getIconPath());
            view.setPasterName(info.getName());
            view.showDelete(false);
            view.showEdit(false);
            view.setIOperationViewClickListener(this);

            // 恢复时间的时候，需要检查一下是否符合这一次区间的startTime和endTime
            long viewStartTime = info.getStartTime();
            long viewEndTime = info.getEndTime();
            view.setStartTime(viewStartTime, viewEndTime);

            RangeSliderViewContainer rangeSliderView = new RangeSliderViewContainer(getActivity());
            rangeSliderView.init(((TCVideoEffectActivity) getActivity()).mVideoProgressController, viewStartTime, viewEndTime - viewStartTime, mDuration);
            rangeSliderView.setDurationChangeListener(mOnDurationChangeListener);
            rangeSliderView.setEditComplete();
            ((TCVideoEffectActivity) getActivity()).mVideoProgressController.addRangeSliderView(ViewConst.VIEW_TYPE_PASTER, rangeSliderView);
            mTCLayerViewGroup.addOperationView(view);// 添加到Group中去管理

            TCPasterInfo tcPasterInfo = new TCPasterInfo();
            tcPasterInfo.setName(info.getName());
            tcPasterInfo.setIconPath(info.getIconPath());
            tcPasterInfo.setPasterType(info.getViewType());
//            mAddPasterInfoList.add(tcPasterInfo);
        }
//        mCurrentSelectedPos = manager.getSize() - 1;
//
//        mAddPasterAdapter.notifyDataSetChanged();
    }

    @Override
    public void notifyStartPlay() {
        if (mTCLayerViewGroup != null) {
            mTCLayerViewGroup.setVisibility(View.GONE);
        }
    }

    @Override
    public void notifyPausePlay() {
        if (mTCLayerViewGroup != null) {
            mTCLayerViewGroup.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void notifyResumePlay() {
        if (mTCLayerViewGroup != null) {
            mTCLayerViewGroup.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAdd() {
        addPasterListVideo();
    }
}
