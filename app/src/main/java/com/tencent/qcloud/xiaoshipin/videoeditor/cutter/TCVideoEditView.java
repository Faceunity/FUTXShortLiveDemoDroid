package com.tencent.qcloud.xiaoshipin.videoeditor.cutter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.videoeditor.time.view.RangeSlider;
import com.tencent.qcloud.xiaoshipin.videoeditor.time.view.TCVideoEditerAdapter;
import com.tencent.qcloud.xiaoshipin.videoeditor.utils.Edit;
import com.tencent.rtmp.TXLog;
import com.tencent.ugc.TXVideoEditConstants;

public class TCVideoEditView extends RelativeLayout implements RangeSlider.OnRangeChangeListener {

    private String TAG = TCVideoEditView.class.getSimpleName();

    private Context mContext;

    private RecyclerView mRecyclerView;
    private RangeSlider mRangeSlider;
    private float mCurrentScroll;
    private int mSingleThumbnailWidth; // 单个缩略图的宽度
    private int mAllThumbnailWidth; // 所有缩略图的宽度

    private long mVideoDuration; // 整个视频的时长
    private long mViewMaxDuration; // 控件最大时长16s
    private long mStartTime = 0; // 如果视频时长超过了控件的最大时长，底部在滑动时最左边的起始位置时间
    private int mViewLeftTime; // 裁剪的起始时间，最左边是0
    private int mViewRightTime; // 裁剪的结束时间，最右边最大是16000ms
    private long mVideoStartPos; // 最终视频的起始时间
    private long mVideoEndPos; // 最终视频的结束时间

    private TCVideoEditerAdapter mAdapter;

    private Edit.OnCutChangeListener mRangeChangeListener;
    private RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            TXCLog.i(TAG, "onScrollStateChanged, new state = " + newState);

            switch (newState) {
                case RecyclerView.SCROLL_STATE_IDLE:
                    onTimeChanged();
                    break;
                case RecyclerView.SCROLL_STATE_DRAGGING:
                    if (mRangeChangeListener != null) {
                        mRangeChangeListener.onCutChangeKeyDown();
                    }
                    break;
                case RecyclerView.SCROLL_STATE_SETTLING:

                    break;
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            mCurrentScroll = mCurrentScroll + dx;
            float rate = mCurrentScroll / mAllThumbnailWidth;
            if(mCurrentScroll + mRecyclerView.getWidth() >= mAllThumbnailWidth){
                mStartTime = mVideoDuration - mViewMaxDuration;
            }else{
                mStartTime = (int) (rate * mVideoDuration);
            }
        }
    };

    public TCVideoEditView(Context context) {
        super(context);

        init(context);
    }

    public TCVideoEditView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public TCVideoEditView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context);
    }

    private void init(Context context) {
        mContext = context;

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.item_edit_view, this, true);

        mRangeSlider = (RangeSlider) findViewById(R.id.range_slider);
        mRangeSlider.setRangeChangeListener(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager manager = new LinearLayoutManager(mContext);
        manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.addOnScrollListener(mOnScrollListener);

        mAdapter = new TCVideoEditerAdapter(mContext);
        mRecyclerView.setAdapter(mAdapter);

        mSingleThumbnailWidth = mContext.getResources().getDimensionPixelOffset(R.dimen.ugc_item_thumb_height);
    }

    public void setCount(int count){
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        int width = count * mSingleThumbnailWidth;
        mAllThumbnailWidth = width;
//        TXCLog.i(TAG, "setCount, mAllThumbnailWidth = " + mAllThumbnailWidth);
        Resources resources = getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        if(width > screenWidth){
            width = screenWidth;
        }
        layoutParams.width = width + 2 * resources.getDimensionPixelOffset(R.dimen.ugc_cut_margin);
        setLayoutParams(layoutParams);
    }

    /**
     * 设置裁剪Listener
     *
     * @param listener
     */
    public void setCutChangeListener(Edit.OnCutChangeListener listener) {
        mRangeChangeListener = listener;
    }

    public void setMediaFileInfo(TXVideoEditConstants.TXVideoInfo videoInfo) {
        if (videoInfo == null) {
            return;
        }
        mVideoDuration = videoInfo.duration;

        if (mVideoDuration >= 16000){
            mViewMaxDuration = 16000;
        } else {
            mViewMaxDuration = mVideoDuration;
        }

        mViewLeftTime = 0;
        mViewRightTime = (int)mViewMaxDuration;

        mVideoStartPos = 0;
        mVideoEndPos = mViewMaxDuration;
    }

    public void addBitmap(int index, Bitmap bitmap) {
        mAdapter.add(index, bitmap);
//        TXCLog.i(TAG, "addBitmap, recylerview width = " + mRecyclerView.getWidth());
    }

    @Override
    public void onKeyDown(int type) {
        if (mRangeChangeListener != null) {
            mRangeChangeListener.onCutChangeKeyDown();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAdapter != null) {
            TXLog.i(TAG, "onDetachedFromWindow: 清除所有bitmap");
            mAdapter.clearAllBitmap();
        }
    }

    @Override
    public void onKeyUp(int type, int leftPinIndex, int rightPinIndex) {
        mViewLeftTime = (int) (mViewMaxDuration * leftPinIndex / 100); //ms
        mViewRightTime = (int) (mViewMaxDuration * rightPinIndex / 100);

//        if (type == RangeSlider.TYPE_LEFT) {
//            mVideoStartPos = mStartTime + leftTime;
//        } else {
//            mVideoEndPos = mStartTime + rightTime;
//        }

        onTimeChanged();
    }

    private void onTimeChanged() {
        mVideoStartPos = mStartTime + mViewLeftTime;
        mVideoEndPos = mStartTime + mViewRightTime;

//        TXCLog.i(TAG, "mVideoStartPos, mVideoEndPos = " + mVideoStartPos + ", " + mVideoEndPos);

        if (mRangeChangeListener != null) {
            mRangeChangeListener.onCutChangeKeyUp((int) mVideoStartPos, (int) mVideoEndPos, 0);
        }
    }

    public int getSegmentFrom() {
        return (int) mVideoStartPos;
    }

    public int getSegmentTo() {
        return (int) mVideoEndPos;
    }

}
