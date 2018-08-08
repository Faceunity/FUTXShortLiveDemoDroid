package com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.layer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hanszhli on 2017/6/22.
 * <p>
 * 用于统一管理{@link TCLayerOperationView}的layout
 */
public class TCLayerViewGroup extends FrameLayout implements View.OnClickListener{
    private final String TAG = "TCLayerViewGroup";
    private List<TCLayerOperationView> mChilds;
    private int mLastSelectedPos = -1;
    private boolean mEnableChildSingleClick = true;
    private boolean mEnableChildDoubleClick = false;

    long mLastTime=0;
    long mCurTime=0;

    public TCLayerViewGroup(Context context) {
        super(context);
        init();
    }

    public TCLayerViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TCLayerViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mChilds = new ArrayList<TCLayerOperationView>();
    }

    public void addOperationView(TCLayerOperationView view) {
        mChilds.add(view);
        selectOperationView(mChilds.size() - 1);
        addView(view);
        view.setOnClickListener(this);
    }

    public void removeOperationView(TCLayerOperationView view) {
        int viewIndex = mChilds.indexOf(view);
        mChilds.remove(view);
        mLastSelectedPos = -1;
        removeView(view);
        view.setOnClickListener(null);
    }

    public TCLayerOperationView getOperationView(int index) {
        return mChilds.get(index);
    }


    public void selectOperationView(int pos) {
        if (pos < mChilds.size() && pos >= 0) {
            if (mLastSelectedPos != -1)
                mChilds.get(mLastSelectedPos).setEditable(false);//不显示编辑的边框
            mChilds.get(pos).setEditable(true);//显示编辑的边框
            mLastSelectedPos = pos;
        }
    }

    private void unSelectOperationView(int pos) {
        if (pos < mChilds.size() && mLastSelectedPos != -1) {
            mChilds.get(mLastSelectedPos).setEditable(false);//不显示编辑的边框
            mLastSelectedPos = -1;
        }
    }

    public TCLayerOperationView getSelectedLayerOperationView() {
        if (mLastSelectedPos < 0 || mLastSelectedPos >= mChilds.size()) return null;
        return mChilds.get(mLastSelectedPos);
    }

    public int getSelectedViewIndex() {
        return mLastSelectedPos;
    }

    public int getChildCount() {
        return mChilds.size();
    }

    public void enableChildSingleClick(boolean enable){
        mEnableChildSingleClick = enable;
    }

    public void enableDoubleChildClick(boolean enable){
        mEnableChildDoubleClick = enable;
    }

    @Override
    public void onClick(View v) {
        mLastTime = mCurTime;
        mCurTime = System.currentTimeMillis();
        if(mCurTime - mLastTime < 300){//双击事件
            mCurTime =0;
            mLastTime = 0;
            if(mEnableChildDoubleClick){
                onItemClick(v);
            }
        }else{//单击事件
            if(mEnableChildSingleClick){
                onItemClick(v);
            }
        }
    }

    private void onItemClick(View v){
        TCLayerOperationView tcLayerOperationView = (TCLayerOperationView) v;
        int pos = mChilds.indexOf(tcLayerOperationView);
        int lastPos = mLastSelectedPos;
        selectOperationView(pos); //选中编辑
        if (mListener != null) {
            mListener.onLayerOperationViewItemClick(tcLayerOperationView, lastPos, pos);
        }
    }

    private OnItemClickListener mListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public interface OnItemClickListener {
        void onLayerOperationViewItemClick(TCLayerOperationView view, int lastSelectedPos, int currentSelectedPos);
    }

}
