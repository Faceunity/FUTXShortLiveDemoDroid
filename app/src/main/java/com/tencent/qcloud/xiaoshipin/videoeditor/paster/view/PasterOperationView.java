package com.tencent.qcloud.xiaoshipin.videoeditor.paster.view;

import android.content.Context;
import android.util.AttributeSet;

import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.layer.TCLayerOperationView;

/**
 * Created by vinsonswang on 2017/10/30.
 */

public class PasterOperationView extends TCLayerOperationView {
    public static int TYPE_CHILD_VIEW_PASTER            = 1;
    public static int TYPE_CHILD_VIEW_ANIMATED_PASTER   = 2;

    private int mChildType;
    private String mPasterName;
    private String mPasterPath; // 预览界面上显示的图片
    private String mIconPath; // 缩略图

    public String getmIconPath() {
        return mIconPath;
    }

    public void setmIconPath(String mIconPath) {
        this.mIconPath = mIconPath;
    }

    public PasterOperationView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public PasterOperationView(Context context) {
        super(context, null);
    }

    public PasterOperationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public int getChildType() {
        return mChildType;
    }

    public void setChildType(int mChildType) {
        this.mChildType = mChildType;
    }

    public String getPasterName() {
        return mPasterName;
    }

    public void setPasterName(String name) {
        this.mPasterName = name;
    }

    public String getPasterPath() {
        return mPasterPath;
    }

    public void setPasterPath(String mPasterPath) {
        this.mPasterPath = mPasterPath;
    }

}
